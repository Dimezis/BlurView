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
import android.util.Log;
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
    private float blurRadius = 1f;
    private boolean enabled = true;

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
        // TODO would be good to keep it the size of the BlurView instead of the target, but then the animation
        //  like translation and rotation would go out of bounds. Not sure if there's a good fix for this
        blurNode.setPosition(0, 0, target.getWidth(), target.getHeight());
        updateRenderNodeProperties();

        drawSnapshot();

        // Draw on the system canvas
        canvas.drawRenderNode(blurNode);
        if (applyNoise) {
            Noise.apply(canvas, blurView.getContext(), blurView.getWidth(), blurView.getHeight());
        }
        if (overlayColor != Color.TRANSPARENT) {
            canvas.drawColor(overlayColor);
        }
    }

    private void updateRenderNodeProperties() {
        float layoutTranslationX = -getLeft();
        float layoutTranslationY = -getTop();

        // Pivot point for the rotation and scale (in case it's applied)
        blurNode.setPivotX(blurView.getWidth() / 2f - layoutTranslationX);
        blurNode.setPivotY(blurView.getHeight() / 2f - layoutTranslationY);
        blurNode.setTranslationX(layoutTranslationX);
        blurNode.setTranslationY(layoutTranslationY);
    }

    private void drawSnapshot() {
        RecordingCanvas recordingCanvas = blurNode.beginRecording();
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
            frameClearDrawable.draw(canvas);
        }
        try {
            target.draw(softwareCanvas);
        } catch (Exception e) {
            // Can potentially fail on rendering Hardware Bitmaps or something like that
            Log.e("BlurView", "Error during snapshot capturing", e);
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
        // https://github.com/Dimezis/BlurView/issues/128
        float scaleFactorH = (float) targetSize.height / scaledSize.height;
        float scaleFactorW = (float) targetSize.width / scaledSize.width;

        float scaledLeftPosition = -getLeft() / scaleFactorW;
        float scaledTopPosition = -getTop() / scaleFactorH;

        canvas.translate(scaledLeftPosition, scaledTopPosition);
        canvas.scale(1 / scaleFactorW, 1 / scaleFactorH);
    }

    private int getTop() {
        return blurViewLocation[1] - targetLocation[1];
    }

    private int getLeft() {
        return blurViewLocation[0] - targetLocation[0];
    }

    @Override
    public void updateBlurViewSize() {
        // No-op, the size is updated in draw method, it's cheap and not called frequently
    }

    @Override
    public void destroy() {
        blurNode.discardDisplayList();
        if (fallbackBlur != null) {
            fallbackBlur.destroy();
            fallbackBlur = null;
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

    void updateRotation(float rotation) {
        blurNode.setRotationZ(-rotation);
    }

    public void updateScaleX(float scaleX) {
        blurNode.setScaleX(1 / scaleX);
    }

    public void updateScaleY(float scaleY) {
        blurNode.setScaleY(1 / scaleY);
    }

    private void saveOnScreenLocation() {
        target.getLocationOnScreen(targetLocation);
        blurView.getLocationOnScreen(blurViewLocation);
    }
}
