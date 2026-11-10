package eightbitlab.com.blurview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RecordingCanvas;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import eightbitlab.com.blurview.internal.BlurViewLog;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import eightbitlab.com.blurview.SizeScaler.Size;

@RequiresApi(api = Build.VERSION_CODES.S)
public class RenderNodeBlurController implements BlurController {
    private final int[] targetLocation = new int[2];
    private final int[] blurViewLocation = new int[2];

    // The part of the target the blur node holds, the part the BlurView needs now, and the part
    // about to be recorded (left, top, right, bottom in target coordinates). See BlurArea.
    private final RecordedArea recorded = new RecordedArea();
    private final int[] neededArea = new int[4];
    private final int[] nextArea = new int[4];

    private final BlurView blurView;
    private final BlurTarget target;
    private final RenderNode blurNode = new RenderNode("BlurView node");
    private final float scaleFactor;
    private final boolean applyNoise;

    private Drawable frameClearDrawable;
    private int overlayColor;
    private float blurRadius = BlurController.DEFAULT_BLUR_RADIUS;
    private boolean enabled = true;

    // Potentially cached stuff from the slow software path
    @Nullable
    private Bitmap cachedBitmap;
    @Nullable
    private RenderScriptBlur fallbackBlur;

    // This tracks BlurView location in scrollable containers, during animations, etc.
    private final ViewTreeObserver.OnPreDrawListener drawListener = () -> {
        followBlurView();
        return true;
    };

    public RenderNodeBlurController(@NonNull BlurView blurView, @NonNull BlurTarget target, int overlayColor, float scaleFactor, boolean applyNoise) {
        this.blurView = blurView;
        this.overlayColor = overlayColor;
        this.target = target;
        this.scaleFactor = scaleFactor;
        this.applyNoise = applyNoise;
        blurView.setWillNotDraw(false);
        blurView.getViewTreeObserver().addOnPreDrawListener(drawListener);
    }

    @Override
    public boolean draw(Canvas canvas) {
        if (!enabled) {
            return true;
        }
        saveOnScreenLocation();

       if (canvas.isHardwareAccelerated()) {
            if (!target.renderNode.hasDisplayList()) {
                // The target and the BlurView are presumably in different windows
                // and the BlurView's Window was traversed before the target's, so
                // we don't have a valid renderNode to blur.
                // Invalidate the BlurView until the target is drawn
                blurView.invalidate();
            }
            hardwarePath(canvas);
        } else {
            // Rendering on a software canvas.
            // Presumably this is something taking a programmatic screenshot,
            // or maybe a software-based View/Fragment transition.
            // This is slow and shouldn't be a common case for this controller.
            softwarePath(canvas);
        }
        return true;
    }

    // Not doing any scaleFactor-related manipulations here, because RenderEffect blur internally
    // already scales down the snapshot depending on the blur radius.
    // https://cs.android.com/android/platform/superproject/main/+/main:external/skia/src/core/SkImageFilterTypes.cpp;drc=61197364367c9e404c7da6900658f1b16c42d0da;l=2103
    // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/libs/hwui/jni/RenderEffect.cpp;l=39;drc=61197364367c9e404c7da6900658f1b16c42d0da?q=nativeCreateBlurEffect&ss=android%2Fplatform%2Fsuperproject%2Fmain
    private void hardwarePath(Canvas canvas) {
        BlurViewTransform t = BlurViewTransform.compute(blurView, blurViewLocation, targetLocation);
        record(t, BlurArea.reach(blurRadius * scaleFactor));
        updateRenderNodeProperties(t);

        canvas.save();
        // Don't draw outside of the BlurView bounds if parent has clipChildren = false
        canvas.clipRect(0f, 0f, blurView.getWidth(), blurView.getHeight());
        // Draw on the system canvas. Drawn even with nothing recorded (the BlurView outside the
        // target), so a recording made later in onPreDraw() shows without redrawing the BlurView.
        canvas.drawRenderNode(blurNode);
        if (applyNoise) {
            Noise.apply(canvas, blurView.getContext(), blurView.getWidth(), blurView.getHeight());
        }
        if (overlayColor != Color.TRANSPARENT) {
            canvas.drawColor(overlayColor);
        }
        canvas.restore();
    }

    /**
     * Follows the BlurView's position against the target, recording another part of the target
     * when it moved past what the node holds.
     */
    private void followBlurView() {
        saveOnScreenLocation();
        BlurViewTransform t = BlurViewTransform.compute(blurView, blurViewLocation, targetLocation);
        if (enabled) {
            int reach = BlurArea.reach(blurRadius * scaleFactor);
            boolean needs = computeArea(t, reach, neededArea);
            if (!recorded.covers(needs, neededArea)) {
                // Recorded here rather than by invalidating the BlurView: its display list refers to
                // the node, so neither it nor its parents have to record theirs again.
                record(t, reach);
            }
        }
        updateRenderNodeProperties(t);
    }

    /**
     * Records the part of the target the BlurView shows, with the blur's reach around it and a
     * grid cell of slack, so a moving BlurView records again once per cell rather than per frame.
     * <p>
     * The node is then about the size of the BlurView rather than the target, and so are its layer
     * and the rasterization of the target into it. The area is taken after the BlurView's scale
     * and rotation, so it still covers an animating BlurView.
     */
    private void record(BlurViewTransform t, int reach) {
        boolean has = computeArea(t, reach + BlurArea.GRID, nextArea);
        recorded.set(has, nextArea);
        if (has) {
            int[] area = recorded.area;
            blurNode.setPosition(0, 0, area[2] - area[0], area[3] - area[1]);
            drawSnapshot();
        } else {
            // The BlurView is outside the target, so there is nothing of it to show
            blurNode.discardDisplayList();
        }
    }

    /**
     * Fills {@code out} with the part of the target the BlurView shows plus {@code reach};
     * false when that misses the target.
     */
    private boolean computeArea(BlurViewTransform t, int reach, int[] out) {
        // About the BlurView's center, as updateRenderNodeProperties() counter-transforms the node,
        // so the area covers what the node shows (with a custom pivot, the node itself is off, as
        // it was before the node was cropped).
        return BlurArea.compute(t.layoutLeft, t.layoutTop, blurView.getWidth(), blurView.getHeight(),
                blurView.getWidth() / 2f, blurView.getHeight() / 2f, t.scaleX, t.scaleY, t.rotationDeg,
                reach, target.getWidth(), target.getHeight(), out);
    }

    private void updateRenderNodeProperties(BlurViewTransform t) {
        // The node's origin is the recorded area's top-left in the target, not the target's.
        float layoutTranslationX = -t.layoutLeft + recorded.area[0];
        float layoutTranslationY = -t.layoutTop + recorded.area[1];

        // Pivot at the center of the BlurView in target coordinates, invariant under the view's transforms.
        blurNode.setPivotX(blurView.getWidth() / 2f - layoutTranslationX);
        blurNode.setPivotY(blurView.getHeight() / 2f - layoutTranslationY);
        blurNode.setTranslationX(layoutTranslationX);
        blurNode.setTranslationY(layoutTranslationY);
        // Counter-scale so the visually expanded area shows the correct underlying content.
        blurNode.setScaleX(1f / t.scaleX);
        blurNode.setScaleY(1f / t.scaleY);
        // Counter-rotate so the blurred content stays aligned with the background.
        blurNode.setRotationZ(-t.rotationDeg);

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.S) {
            // There's a bug on API 31 - blurNode doesn't get re-rendered on setting new translation/scale/rotation,
            // so we need to re-apply the blur effect to trigger a redraw.
            applyBlur();
        }
    }

    private void drawSnapshot() {
        RecordingCanvas recordingCanvas = blurNode.beginRecording();
        recordingCanvas.translate(-recorded.area[0], -recorded.area[1]);
        if (frameClearDrawable != null) {
            frameClearDrawable.draw(recordingCanvas);
        }
        recordingCanvas.drawRenderNode(target.renderNode);
        // Looks like the order of this doesn't matter
        applyBlur();
        blurNode.endRecording();
    }

    private void softwarePath(Canvas canvas) {
        SizeScaler sizeScaler = new SizeScaler(scaleFactor);
        Size original = new Size(blurView.getWidth(), blurView.getHeight());
        Size scaled = sizeScaler.scale(original);
        if (cachedBitmap == null || cachedBitmap.getWidth() != scaled.width || cachedBitmap.getHeight() != scaled.height) {
            cachedBitmap = Bitmap.createBitmap(scaled.width, scaled.height, Bitmap.Config.ARGB_8888);
        }
        Canvas softwareCanvas = new Canvas(cachedBitmap);

        softwareCanvas.save();
        setupCanvasMatrix(softwareCanvas, original, scaled);
        if (frameClearDrawable != null) {
            frameClearDrawable.draw(softwareCanvas);
        }
        try {
            target.draw(softwareCanvas);
        } catch (Exception e) {
            // Can potentially fail on rendering Hardware Bitmaps or something like that
            BlurViewLog.error("BlurView", "Error during snapshot capturing", e);
        }
        softwareCanvas.restore();

        if (fallbackBlur == null) {
            fallbackBlur = new RenderScriptBlur(blurView.getContext());
        }
        fallbackBlur.blur(cachedBitmap, blurRadius);
        canvas.save();
        canvas.scale((float) original.width / scaled.width, (float) original.height / scaled.height);
        fallbackBlur.render(canvas, cachedBitmap);
        canvas.restore();
        if (applyNoise) {
            Noise.apply(canvas, blurView.getContext(), blurView.getWidth(), blurView.getHeight());
        }
        if (overlayColor != Color.TRANSPARENT) {
            canvas.drawColor(overlayColor);
        }
    }

    /**
     * Set up matrix to draw starting from blurView's position
     */
    private void setupCanvasMatrix(Canvas canvas, Size targetSize, Size scaledSize) {
        BlurViewTransform t = BlurViewTransform.compute(blurView, blurViewLocation, targetLocation);

        float rootCenterX = t.layoutLeft + blurView.getWidth() / 2f;
        float rootCenterY = t.layoutTop + blurView.getHeight() / 2f;

        // https://github.com/Dimezis/BlurView/issues/128
        float scaleFactorH = (float) targetSize.height / scaledSize.height;
        float scaleFactorW = (float) targetSize.width / scaledSize.width;
        float bitmapCenterX = scaledSize.width / 2f;
        float bitmapCenterY = scaledSize.height / 2f;

        canvas.translate(bitmapCenterX, bitmapCenterY);
        canvas.rotate(-t.rotationDeg);
        canvas.scale(1f / (scaleFactorW * t.scaleX), 1f / (scaleFactorH * t.scaleY));
        canvas.translate(-rootCenterX, -rootCenterY);
    }

    @Override
    public void updateBlurViewSize() {
        // No-op, the size is updated in draw method, it's cheap and not called frequently
    }

    @Override
    public void destroy() {
        // BlurView.setupWith replaces a destroyed controller, which must stop following the view
        ViewTreeObserver observer = blurView.getViewTreeObserver();
        if (observer.isAlive()) {
            observer.removeOnPreDrawListener(drawListener);
        }
        blurNode.discardDisplayList();
        recorded.clear();
        if (fallbackBlur != null) {
            fallbackBlur.destroy();
            fallbackBlur = null;
        }
        // Safe to recycle: draw() and destroy() both run on the UI thread and never overlap, so no
        // draw can touch the bitmap after this. A later software draw reallocates it (field is nulled).
        if (cachedBitmap != null) {
            cachedBitmap.recycle();
            cachedBitmap = null;
        }
    }

    @Override
    public BlurViewFacade setBlurEnabled(boolean enabled) {
        this.enabled = enabled;
        blurView.invalidate();
        return this;
    }

    @Override
    public BlurViewFacade setBlurAutoUpdate(boolean enabled) {
        blurView.getViewTreeObserver().removeOnPreDrawListener(drawListener);
        if (enabled) {
            blurView.getViewTreeObserver().addOnPreDrawListener(drawListener);
        }
        return this;
    }

    @Override
    public BlurViewFacade setFrameClearDrawable(@Nullable Drawable frameClearDrawable) {
        this.frameClearDrawable = frameClearDrawable;
        return this;
    }

    @Override
    public BlurViewFacade setBlurRadius(float radius) {
        this.blurRadius = radius;
        applyBlur();
        // A wider blur reads further past the BlurView than the node may hold
        followBlurView();
        return this;
    }

    private void applyBlur() {
        // scaleFactor is only used to increase the blur radius
        // because RenderEffect already scales down the snapshot when needed.
        float realBlurRadius = blurRadius * scaleFactor;
        RenderEffect blur = RenderEffect.createBlurEffect(realBlurRadius, realBlurRadius, Shader.TileMode.CLAMP);
        blurNode.setRenderEffect(blur);
    }

    @Override
    public BlurViewFacade setOverlayColor(int overlayColor) {
        if (this.overlayColor != overlayColor) {
            this.overlayColor = overlayColor;
            blurView.invalidate();
        }
        return this;
    }

    /**
     * Single entry point for view-property-driven updates (scaleX, scaleY, rotation).
     * <p>
     * The caller (BlurView) has already applied the new value via its super setter before
     * notifying, so no value needs to be passed in: {@link #followBlurView()}
     * reads the current transform via {@link BlurViewTransform#compute}. Routing through the
     * full recompute keeps the blur node pivot, translation, scale, and rotation consistent,
     * instead of mutating a single property in isolation.
     */
    void onViewTransformChanged() {
        followBlurView();
        blurView.invalidate();
    }

    private void saveOnScreenLocation() {
        target.getLocationOnScreen(targetLocation);
        blurView.getLocationOnScreen(blurViewLocation);
    }
}
