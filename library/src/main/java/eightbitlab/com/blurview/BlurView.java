package eightbitlab.com.blurview;

import static eightbitlab.com.blurview.BlurController.DEFAULT_SCALE_FACTOR;
import static eightbitlab.com.blurview.PreDrawBlurController.TRANSPARENT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.eightbitlab.blurview.R;

import eightbitlab.com.blurview.internal.BlurViewLog;

/**
 * FrameLayout that blurs its underlying content.
 * Can have children and draw them over blurred background.
 * <p>
 * <strong>Large-radius note:</strong> the blur is backed by one of three platform paths depending on
 * API level - {@code RenderEffect} (API 31+), OpenGL (API 29-30), or RenderScript (older). These paths
 * do not share a single kernel implementation, so at large blur radii their appearance can diverge: the
 * OpenGL path caps its Gaussian kernel at a fixed tap count, which clamps effective blur strength once a
 * radius grows past that ceiling (the blur plateaus instead of widening further), whereas RenderEffect
 * and RenderScript have no equivalent shared cap. Expect slightly different looks across API levels at
 * high radii; to blur harder past the OpenGL ceiling, increase the scale factor (downscale the snapshot
 * more) rather than relying on radius alone.
 */
public class BlurView extends FrameLayout {

    BlurController blurController = new NoOpController();

    @ColorInt
    private int overlayColor;
    private boolean blurAutoUpdate = true;

    public BlurView(Context context) {
        super(context);
        init(null, 0);
    }

    public BlurView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public BlurView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    private void init(AttributeSet attrs, int defStyleAttr) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.BlurView, defStyleAttr, 0);
        overlayColor = a.getColor(R.styleable.BlurView_blurOverlayColor, TRANSPARENT);
        a.recycle();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        boolean shouldDraw = blurController.draw(canvas);
        if (shouldDraw) {
            super.draw(canvas);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        blurController.updateBlurViewSize();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        blurController.setBlurAutoUpdate(false);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isHardwareAccelerated()) {
            BlurViewLog.error("BlurView", "BlurView can't be used in not hardware-accelerated window!");
        } else {
            blurController.setBlurAutoUpdate(this.blurAutoUpdate);
        }
    }

    /**
     * @param target      the root to start blur from.
     * @param algorithm   sets the blur algorithm. Ignored on API >= 29 where a hardware rendering pipeline is used.
     * @param scaleFactor a scale factor to downscale the view snapshot before blurring.
     *                    Helps achieving stronger blur and potentially better performance at the expense of blur precision.
     *                    The blur radius is essentially the radius * scaleFactor.
     * @param applyNoise  optional blue noise texture over the blurred content to make it look more natural. True by default.
     * @return {@link BlurView} to setup needed params.
     */
    public BlurViewFacade setupWith(@NonNull BlurTarget target, BlurAlgorithm algorithm, float scaleFactor, boolean applyNoise) {
        blurController.destroy();
        if (BlurTarget.canUseHardwareRendering) {
            // Ignores the blur algorithm, always uses RenderEffect
            blurController = new RenderNodeBlurController(this, target, overlayColor, scaleFactor, applyNoise);
        } else {
            if (algorithm == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    algorithm = new OpenGLBlurAlgorithm();
                } else {
                    //noinspection deprecation
                    algorithm = new RenderScriptBlur(getContext());
                }
            }
            blurController = new PreDrawBlurController(this, target, overlayColor, algorithm, scaleFactor, applyNoise);
        }

        return blurController;
    }

    /**
     * @param rootView    the root to start blur from.
     *                    BlurAlgorithm is automatically picked based on the API version.
     *                    It uses RenderEffect on API 31+, OpenGL on API 29-30, and RenderScriptBlur below that.
     * @param scaleFactor a scale factor to downscale the view snapshot before blurring.
     *                    Helps achieving stronger blur and potentially better performance at the expense of blur precision.
     *                    The blur radius is essentially the radius * scaleFactor.
     * @param applyNoise  optional blue noise texture over the blurred content to make it look more natural. True by default.
     * @return {@link BlurView} to setup needed params.
     */
    public BlurViewFacade setupWith(@NonNull BlurTarget rootView, float scaleFactor, boolean applyNoise) {
        // The algorithm is only used by the PreDrawBlurController on API < 29; the RenderEffect and
        // OpenGL paths ignore it.
        BlurAlgorithm algorithm = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                ? new RenderScriptBlur(getContext())
                : null;
        return setupWith(rootView, algorithm, scaleFactor, applyNoise);
    }

    /**
     * @param rootView root to start blur from.
     *                 BlurAlgorithm is automatically picked based on the API version.
     *                 It uses RenderEffect on API 31+, and RenderScriptBlur on older versions.
     *                 The {@link DEFAULT_SCALE_FACTOR} scale factor for view snapshot is used.
     *                 Blue noise texture is applied by default.
     * @return {@link BlurView} to setup needed params.
     */
    public BlurViewFacade setupWith(@NonNull BlurTarget rootView) {
        return setupWith(rootView, DEFAULT_SCALE_FACTOR, true);
    }

    // Setters duplicated to be able to conveniently change these settings outside of setupWith chain

    /**
     * @see BlurViewFacade#setBlurRadius(float)
     */
    public BlurViewFacade setBlurRadius(float radius) {
        return blurController.setBlurRadius(radius);
    }

    /**
     * @see BlurViewFacade#setOverlayColor(int)
     */
    public BlurViewFacade setOverlayColor(@ColorInt int overlayColor) {
        this.overlayColor = overlayColor;
        return blurController.setOverlayColor(overlayColor);
    }

    /**
     * @see BlurViewFacade#setBlurAutoUpdate(boolean)
     */
    public BlurViewFacade setBlurAutoUpdate(boolean enabled) {
        this.blurAutoUpdate = enabled;
        return blurController.setBlurAutoUpdate(enabled);
    }

    /**
     * @see BlurViewFacade#setBlurEnabled(boolean)
     */
    public BlurViewFacade setBlurEnabled(boolean enabled) {
        return blurController.setBlurEnabled(enabled);
    }

    /**
     * Installs a logger for BlurView's internal error reports, letting the host app
     * route them to its logging or crash-reporting pipeline for production observability.
     * By default reports go to {@link android.util.Log}.
     * <p>
     * This setting is global to the process, not per-instance.
     *
     * @param logger the logger to receive error reports
     */
    public static void setLogger(@NonNull BlurLogger logger) {
        BlurViewLog.setLogger(logger);
    }

    @Override
    public void setRotation(float rotation) {
        super.setRotation(rotation);
        notifyRotationChanged(rotation);
    }

    @Override
    public void setScaleX(float scaleX) {
        super.setScaleX(scaleX);
        notifyScaleXChanged(scaleX);
    }

    @Override
    public void setScaleY(float scaleY) {
        super.setScaleY(scaleY);
        notifyScaleYChanged(scaleY);
    }

    @SuppressLint("NewApi")
    public void notifyRotationChanged(float rotation) {
        if (usingRenderNode()) {
            ((RenderNodeBlurController) blurController).onViewTransformChanged();
        }
    }

    @SuppressLint("NewApi")
    public void notifyScaleXChanged(float scaleX) {
        if (usingRenderNode()) {
            ((RenderNodeBlurController) blurController).onViewTransformChanged();
        }
    }

    @SuppressLint("NewApi")
    public void notifyScaleYChanged(float scaleY) {
        if (usingRenderNode()) {
            ((RenderNodeBlurController) blurController).onViewTransformChanged();
        }
    }

    private boolean usingRenderNode() {
        return blurController instanceof RenderNodeBlurController;
    }
}
