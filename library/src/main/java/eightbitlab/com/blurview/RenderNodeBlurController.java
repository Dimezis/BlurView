package eightbitlab.com.blurview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RecordingCanvas;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;

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

    private Drawable frameClearDrawable;
    private int overlayColor;
    private float blurRadius = 1f;
    private boolean enabled = true;

    // Potentially cached stuff from the slow software path
    @Nullable
    private Bitmap cachedBitmap;
    @Nullable
    private RenderScriptBlur fallbackBlur;
    @Nullable
    private Paint paint;

    public RenderNodeBlurController(@NonNull BlurView blurView, @NonNull BlurTarget target, int overlayColor, float scaleFactor) {
        this.blurView = blurView;
        this.overlayColor = overlayColor;
        this.target = target;
        this.scaleFactor = scaleFactor;
        blurView.setWillNotDraw(false);
    }

    @Override
    public boolean draw(Canvas canvas) {
        if (!enabled) {
            return true;
        }
        target.getLocationOnScreen(targetLocation);
        blurView.getLocationOnScreen(blurViewLocation);

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

    private void hardwarePath(Canvas canvas) {
        RenderEffect renderEffect = RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.MIRROR);
        SizeScaler sizeScaler = new SizeScaler(scaleFactor, true);
        Size original = new Size(target.getWidth(), target.getHeight());
        Size scaled = sizeScaler.scale(original);
        Size scaledBlurView = sizeScaler.scale(blurView.getWidth(), blurView.getHeight());

        int left = blurViewLocation[0] - targetLocation[0];
        int top = blurViewLocation[1] - targetLocation[1];
        float scaleFactorH = (float) original.height / scaled.height;
        float scaleFactorW = (float) original.width / scaled.width;
        int scaledLeftPosition = (int) (-left / scaleFactorW);
        int scaledTopPosition = (int) (-top / scaleFactorH);

        // TODO would be good to keep it the size of the BlurView instead of the target, but then the animation
        //  like translation and rotation would go out of bounds. Not sure if there's a good fix for this
        blurNode.setPosition(scaledLeftPosition, scaledTopPosition, scaled.width, scaled.height);
        // Pivot point for the rotation and scale (in case it's applied)
        blurNode.setPivotX(scaledBlurView.width / 2f - scaledLeftPosition);
        blurNode.setPivotY(scaledBlurView.height / 2f - scaledTopPosition);
        blurNode.setRenderEffect(renderEffect);

        RecordingCanvas recordingCanvas = blurNode.beginRecording();
        if (frameClearDrawable != null) {
            frameClearDrawable.draw(recordingCanvas);
        }
        recordingCanvas.save();
        // Not reusing setupCanvasMatrix here because there are some weird clipping issues during rotation animation.
        // Instead, setting the scaled translation in blurNode.setPosition and scaling here
        recordingCanvas.scale(1 / scaleFactorW, 1 / scaleFactorH);
        recordingCanvas.drawRenderNode(target.renderNode);
        recordingCanvas.restore();
        if (overlayColor != Color.TRANSPARENT) {
            recordingCanvas.drawColor(overlayColor);
        }
        blurNode.endRecording();

        // Draw on the system canvas
        canvas.save();
        canvas.scale((float) target.getWidth() / scaled.width, (float) target.getHeight() / scaled.height);
        canvas.drawRenderNode(blurNode);
        canvas.restore();
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
        target.draw(softwareCanvas);
        softwareCanvas.restore();

        if (fallbackBlur == null) {
            fallbackBlur = new RenderScriptBlur(blurView.getContext());
            paint = new Paint();
            paint.setFilterBitmap(true);
        }
        fallbackBlur.blur(cachedBitmap, blurRadius);
        if (overlayColor != Color.TRANSPARENT) {
            softwareCanvas.drawColor(overlayColor);
        }

        canvas.drawBitmap(cachedBitmap, 0f, 0f, paint);
    }

    /**
     * Set up matrix to draw starting from blurView's position
     */
    private void setupCanvasMatrix(Canvas canvas, Size targetSize, Size scaledSize) {
        int left = blurViewLocation[0] - targetLocation[0];
        int top = blurViewLocation[1] - targetLocation[1];

        // https://github.com/Dimezis/BlurView/issues/128
        float scaleFactorH = (float) targetSize.height / scaledSize.height;
        float scaleFactorW = (float) targetSize.width / scaledSize.width;

        float scaledLeftPosition = -left / scaleFactorW;
        float scaledTopPosition = -top / scaleFactorH;

        canvas.translate(scaledLeftPosition, scaledTopPosition);
        canvas.scale(1 / scaleFactorW, 1 / scaleFactorH);
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
        // No auto update setting, we draw on system demand and RenderNode keeps track of changes
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
        RenderEffect renderEffect = RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.MIRROR);
        blurNode.setRenderEffect(renderEffect);
        return this;
    }

    @Override
    public BlurViewFacade setOverlayColor(int overlayColor) {
        if (this.overlayColor != overlayColor) {
            this.overlayColor = overlayColor;
            blurView.invalidate();
        }
        return this;
    }

    void updateTranslationY(float translationY) {
        blurNode.setTranslationY(-translationY);
    }

    void updateTranslationX(float translationX) {
        blurNode.setTranslationX(-translationX);
    }

    void updateTranslationZ(float translationZ) {
        blurNode.setTranslationZ(-translationZ);
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
}
