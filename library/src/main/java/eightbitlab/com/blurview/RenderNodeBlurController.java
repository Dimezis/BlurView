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

    private final BlurView blurView;
    private final BlurTarget target;
    private final RenderNode blurNode = new RenderNode("BlurView node");
    private final float scaleFactor;
    private final boolean applyNoise;

    private Drawable frameClearDrawable;
    private int overlayColor;
    private float blurRadius = BlurController.DEFAULT_BLUR_RADIUS;
    private boolean enabled = true;
    private int lastNodeWidth = -1;
    private int lastNodeHeight = -1;
    @Nullable
    private RenderEffect cachedBlurEffect;
    private float cachedEffectRadius = -1f;

    // Potentially cached stuff from the slow software path
    @Nullable
    private Bitmap cachedBitmap;
    @Nullable
    private RenderScriptBlur fallbackBlur;

    // This tracks BlurView location in scrollable containers, during animations, etc.
    private final ViewTreeObserver.OnPreDrawListener drawListener = () -> {
        saveOnScreenLocation();
        updateRenderNodeProperties();
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

    // Record the target snapshot into a blurNode downscaled by scaleFactor before applying
    // RenderEffect. Although SkBlurImageFilter downscales internally for large sigmas, HWUI still
    // allocates and rasterizes the RenderNode's offscreen layer at its own position bounds first,
    // so keeping blurNode at 1/scaleFactor cuts both offscreen GPU memory and rasterization cost.
    private void hardwarePath(Canvas canvas) {
        int nodeWidth = downscaledDimension(target.getWidth(), scaleFactor);
        int nodeHeight = downscaledDimension(target.getHeight(), scaleFactor);
        if (nodeWidth <= 0 || nodeHeight <= 0) {
            return;
        }
        // TODO would be good to keep it the size of the BlurView instead of the target, but then the animation
        //  like translation and rotation would go out of bounds. Not sure if there's a good fix for this
        if (nodeWidth != lastNodeWidth || nodeHeight != lastNodeHeight) {
            blurNode.setPosition(0, 0, nodeWidth, nodeHeight);
            lastNodeWidth = nodeWidth;
            lastNodeHeight = nodeHeight;
        }
        updateRenderNodeProperties();

        drawSnapshot();

        canvas.save();
        // Don't draw outside of the BlurView bounds if parent has clipChildren = false
        canvas.clipRect(0f, 0f, blurView.getWidth(), blurView.getHeight());
        // Draw on the system canvas
        canvas.drawRenderNode(blurNode);
        if (applyNoise) {
            Noise.apply(canvas, blurView.getContext(), blurView.getWidth(), blurView.getHeight());
        }
        if (overlayColor != Color.TRANSPARENT) {
            canvas.drawColor(overlayColor);
        }
        canvas.restore();
    }

    private void updateRenderNodeProperties() {
        BlurViewTransform t = BlurViewTransform.compute(blurView, blurViewLocation, targetLocation);
        NodeTransform node = computeNodeTransform(
                blurView.getWidth(),
                blurView.getHeight(),
                t.layoutLeft,
                t.layoutTop,
                t.scaleX,
                t.scaleY,
                t.rotationDeg,
                scaleFactor
        );

        // Pivot at the center of the BlurView in downscaled target coordinates, invariant under the view's transforms.
        blurNode.setPivotX(node.pivotX);
        blurNode.setPivotY(node.pivotY);
        blurNode.setTranslationX(node.translationX);
        blurNode.setTranslationY(node.translationY);
        // Counter-scale and restore the downscaled node back to screen coordinates.
        blurNode.setScaleX(node.scaleX);
        blurNode.setScaleY(node.scaleY);
        // Counter-rotate so the blurred content stays aligned with the background.
        blurNode.setRotationZ(node.rotationZ);

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.S) {
            // There's a bug on API 31 - blurNode doesn't get re-rendered on setting new translation/scale/rotation,
            // so we need to re-apply the blur effect to trigger a redraw.
            applyBlur();
        }
    }

    private void drawSnapshot() {
        RecordingCanvas recordingCanvas = blurNode.beginRecording();
        float invScale = 1f / effectiveScale(scaleFactor);
        recordingCanvas.scale(invScale, invScale);
        if (frameClearDrawable != null) {
            frameClearDrawable.draw(recordingCanvas);
        }
        recordingCanvas.drawRenderNode(target.renderNode);
        blurNode.endRecording();
        if (cachedBlurEffect == null) {
            applyBlur();
        }
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
        blurNode.discardDisplayList();
        lastNodeWidth = -1;
        lastNodeHeight = -1;
        cachedBlurEffect = null;
        cachedEffectRadius = -1f;
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
        return this;
    }

    private void applyBlur() {
        float radius = nodeBlurRadius(blurRadius, scaleFactor);
        if (cachedBlurEffect == null || cachedEffectRadius != radius) {
            cachedBlurEffect = RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP);
            cachedEffectRadius = radius;
        }
        blurNode.setRenderEffect(cachedBlurEffect);
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
     * notifying, so no value needs to be passed in: {@link #updateRenderNodeProperties()}
     * reads the current transform via {@link BlurViewTransform#compute}. Routing through the
     * full recompute keeps the blur node pivot, translation, scale, and rotation consistent,
     * instead of mutating a single property in isolation.
     */
    void onViewTransformChanged() {
        saveOnScreenLocation();
        updateRenderNodeProperties();
        blurView.invalidate();
    }

    private void saveOnScreenLocation() {
        target.getLocationOnScreen(targetLocation);
        blurView.getLocationOnScreen(blurViewLocation);
    }

    static float effectiveScale(float scaleFactor) {
        return Math.max(1f, scaleFactor);
    }

    static int downscaledDimension(int targetSize, float scaleFactor) {
        if (targetSize <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(targetSize / effectiveScale(scaleFactor)));
    }

    static float nodeBlurRadius(float blurRadius, float scaleFactor) {
        float realBlurRadius = blurRadius * scaleFactor;
        return realBlurRadius / effectiveScale(scaleFactor);
    }

    static NodeTransform computeNodeTransform(
            float blurViewWidth,
            float blurViewHeight,
            float layoutLeft,
            float layoutTop,
            float viewScaleX,
            float viewScaleY,
            float rotationDeg,
            float scaleFactor
    ) {
        float scale = effectiveScale(scaleFactor);
        float invScale = 1f / scale;
        float pivotX = (blurViewWidth / 2f + layoutLeft) * invScale;
        float pivotY = (blurViewHeight / 2f + layoutTop) * invScale;
        float translationX = blurViewWidth / 2f - pivotX;
        float translationY = blurViewHeight / 2f - pivotY;
        return new NodeTransform(
                pivotX,
                pivotY,
                translationX,
                translationY,
                scale / viewScaleX,
                scale / viewScaleY,
                -rotationDeg
        );
    }

    static final class NodeTransform {
        final float pivotX;
        final float pivotY;
        final float translationX;
        final float translationY;
        final float scaleX;
        final float scaleY;
        final float rotationZ;

        NodeTransform(
                float pivotX,
                float pivotY,
                float translationX,
                float translationY,
                float scaleX,
                float scaleY,
                float rotationZ
        ) {
            this.pivotX = pivotX;
            this.pivotY = pivotY;
            this.translationX = translationX;
            this.translationY = translationY;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.rotationZ = rotationZ;
        }
    }
}
