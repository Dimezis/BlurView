package eightbitlab.com.blurview;

import static eightbitlab.com.blurview.BlurController.DEFAULT_SCALE_FACTOR;
import static eightbitlab.com.blurview.PreDrawBlurController.TRANSPARENT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.eightbitlab.blurview.R;

/**
 * FrameLayout that blurs its underlying content.
 * Can have children and draw them over blurred background.
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
            Log.e("BlurView", "BlurView can't be used in not hardware-accelerated window!");
        } else {
            blurController.setBlurAutoUpdate(this.blurAutoUpdate);
        }
    }

    /**
     * @param target      the root to start blur from.
     * @param algorithm   sets the blur algorithm. Ignored on API >= 31 where efficient hardware rendering pipeline is used.
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
            blurController = new PreDrawBlurController(this, target, overlayColor, algorithm, scaleFactor, applyNoise);
        }

        return blurController;
    }

    /**
     * @param rootView    the root to start blur from.
     *                    BlurAlgorithm is automatically picked based on the API version.
     *                    It uses RenderEffect on API 31+, and RenderScriptBlur on older versions.
     * @param scaleFactor a scale factor to downscale the view snapshot before blurring.
     *                    Helps achieving stronger blur and potentially better performance at the expense of blur precision.
     *                    The blur radius is essentially the radius * scaleFactor.
     * @param applyNoise  optional blue noise texture over the blurred content to make it look more natural. True by default.
     * @return {@link BlurView} to setup needed params.
     */
    public BlurViewFacade setupWith(@NonNull BlurTarget rootView, float scaleFactor, boolean applyNoise) {
        BlurAlgorithm algorithm;
        if (BlurTarget.canUseHardwareRendering) {
            // Ignores the blur algorithm, always uses RenderNodeBlurController and RenderEffect
            algorithm = null;
        } else {
            algorithm = new RenderScriptBlur(getContext());
        }
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

    @Override
    public void setRotation(float rotation) {
        super.setRotation(rotation);
        notifyRotationChanged(rotation);
    }

    @SuppressLint("NewApi")
    public void notifyRotationChanged(float rotation) {
        if (usingRenderNode()) {
            ((RenderNodeBlurController) blurController).updateRotation(rotation);
        }
    }

    @SuppressLint("NewApi")
    public void notifyScaleXChanged(float scaleX) {
        if (usingRenderNode()) {
            ((RenderNodeBlurController) blurController).updateScaleX(scaleX);
        }
    }

    @SuppressLint("NewApi")
    public void notifyScaleYChanged(float scaleY) {
        if (usingRenderNode()) {
            ((RenderNodeBlurController) blurController).updateScaleY(scaleY);
        }
    }

    private boolean usingRenderNode() {
        return blurController instanceof RenderNodeBlurController;
    }
}
