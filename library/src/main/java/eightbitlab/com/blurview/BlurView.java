package eightbitlab.com.blurview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * FrameLayout that blurs its underlying content.
 * Can have children and draw them over blurred background.
 */
public class BlurView extends FrameLayout {

    private static final String TAG = BlurView.class.getSimpleName();
    @ColorInt
    private static final int TRANSPARENT = 0x00000000;

    BlurController blurController = createStubController();

    @ColorInt
    private int overlayColor;

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
    public void draw(Canvas canvas) {
        //draw only on system's hardware accelerated canvas
        if (canvas.isHardwareAccelerated()) {
            blurController.draw(canvas);
            if (overlayColor != TRANSPARENT) {
                canvas.drawColor(overlayColor);
            }
        }
        super.draw(canvas);
    }

    /**
     * Can be used to stop blur auto update or resume if it was stopped before.
     * Enabled by default.
     */
    public BlurView setBlurAutoUpdate(final boolean enabled) {
        post(new Runnable() {
            @Override
            public void run() {
                blurController.setBlurAutoUpdate(enabled);
            }
        });
        return this;
    }

    /**
     * Enables/disables the blur. Enabled by default
     *
     * @param enabled true to enable, false otherwise
     */
    public BlurView setBlurEnabled(final boolean enabled) {
        post(new Runnable() {
            @Override
            public void run() {
                blurController.setBlurEnabled(enabled);
            }
        });
        return this;
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
            Log.e(TAG, "BlurView can't be used in not hardware-accelerated window!");
        } else {
            blurController.setBlurAutoUpdate(true);
        }
    }

    private void setBlurController(@NonNull BlurController blurController) {
        this.blurController.destroy();
        this.blurController = blurController;
    }

    /**
     * Sets the color overlay to be drawn on top of blurred content
     *
     * @param overlayColor int color
     */
    public BlurView setOverlayColor(@ColorInt int overlayColor) {
        if (overlayColor != this.overlayColor) {
            this.overlayColor = overlayColor;
            invalidate();
        }
        return this;
    }

    /**
     * Can be set to true to optimize position calculation before blur.
     * By default, BlurView calculates its translation, rotation and scale before each draw call.
     * If you are not changing these properties (for example, during animation), this behavior can be changed
     * to calculate them only once during initialization.
     *
     * @param hasFixedTransformationMatrix indicates if this BlurView has fixed transformation Matrix.
     * @return {@link BlurView}
     */
    public BlurView setHasFixedTransformationMatrix(boolean hasFixedTransformationMatrix) {
        blurController.setHasFixedTransformationMatrix(hasFixedTransformationMatrix);
        return this;
    }

    /**
     * @param rootView root to start blur from.
     *                 Can be Activity's root content layout (android.R.id.content)
     *                 or (preferably) some of your layouts. The lower amount of View are in a root, the better for performance.
     *                 BlurView's position will be calculated as a relative position to the rootView (not to the direct parent)
     *                 This means that BlurView will choose a content to blur based on this relative position.
     * @return {@link BlurView} to setup needed params.
     */
    public BlurView setupWith(@NonNull ViewGroup rootView) {
        BlurController blurController = new BlockingBlurController(this, rootView);
        setBlurController(blurController);

        if (!isHardwareAccelerated()) {
            blurController.setBlurAutoUpdate(false);
        }

        return this;
    }

    /**
     * @param radius sets the blur radius
     *               Default implementation uses field {@link BlurController#DEFAULT_BLUR_RADIUS}
     * @return {@link BlurView}
     */
    public BlurView setBlurRadius(float radius) {
        blurController.setBlurRadius(radius);
        return this;
    }

    /**
     * @param algorithm sets the blur algorithm
     *                  Default implementation uses {@link NoOpBlurAlgorithm}
     * @return {@link BlurView}
     */
    public BlurView setBlurAlgorithm(BlurAlgorithm algorithm) {
        blurController.setBlurAlgorithm(algorithm);
        return this;
    }

    /**
     * @param frameClearDrawable sets the drawable to draw before view hierarchy.
     *                           Can be used to draw Activity's window background if your root layout doesn't provide any background
     *                           Optional, by default frame is cleared with a transparent color.
     * @return {@link BlurView}
     */
    public BlurView setFrameClearDrawable(@Nullable Drawable frameClearDrawable) {
        blurController.setFrameClearDrawable(frameClearDrawable);
        return this;
    }

    //Used in edit mode and in case if no BlurController was set
    private BlurController createStubController() {
        return new BlurController() {
            @Override
            public void draw(Canvas canvas) {
            }

            @Override
            public void updateBlurViewSize() {
            }

            @Override
            public void setBlurRadius(float radius) {
            }

            @Override
            public void setBlurAlgorithm(BlurAlgorithm algorithm) {
            }

            @Override
            public void setFrameClearDrawable(@Nullable Drawable windowBackground) {
            }

            @Override
            public void destroy() {
            }

            @Override
            public void setBlurEnabled(boolean enabled) {
            }

            @Override
            public void setBlurAutoUpdate(boolean enabled) {
            }

            @Override
            public void setHasFixedTransformationMatrix(boolean hasFixedTransformationMatrix) {
            }
        };
    }
}
