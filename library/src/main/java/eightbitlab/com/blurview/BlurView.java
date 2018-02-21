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

        //we need to draw even without background set
        setWillNotDraw(false);
    }

    @Override
    public void draw(Canvas canvas) {
        //draw only on system's hardware accelerated canvas
        if (canvas.isHardwareAccelerated()) {
            blurController.drawBlurredContent(canvas);
            canvas.drawColor(overlayColor);
            super.draw(canvas);
        } else if (!isHardwareAccelerated()) {
            //if view is in a not hardware accelerated window, don't draw blur
            super.draw(canvas);
        }
    }

    /**
     * Can be called to redraw blurred content manually
     */
    public void updateBlur() {
        invalidate();
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
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        blurController.onDrawEnd(canvas);
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
     * <p>
     * This also resolves the issue with distortion of vector drawables.
     *
     * @param hasFixedTransformationMatrix indicates if this BlurView has fixed transformation Matrix.
     * @return {@link BlurView}
     * @see <a href="https://github.com/Dimezis/BlurView/issues/41">Distortion of vector drawables</a>
     */
    public BlurView setHasFixedTransformationMatrix(boolean hasFixedTransformationMatrix) {
        blurController.setHasFixedTransformationMatrix(hasFixedTransformationMatrix);
        return this;
    }

    /**
     * @param rootView Root View where BlurView's underlying content starts drawing.
     *                 Can be Activity's root content layout (android.R.id.content)
     *                 or (preferably) some of your root layouts.
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

    //TODO `set` prefix should be added to methods below, but for now leaving it like this for compatibility
    /**
     * @param radius sets the blur radius
     *               Default implementation uses field {@link BlurController#DEFAULT_BLUR_RADIUS}
     * @return {@link BlurView}
     */
    public BlurView blurRadius(float radius) {
        blurController.setBlurRadius(radius);
        return this;
    }

    /**
     * @param algorithm sets the blur algorithm
     *                  Default implementation uses {@link NoOpBlurAlgorithm}
     * @return {@link BlurView}
     */
    public BlurView blurAlgorithm(BlurAlgorithm algorithm) {
        blurController.setBlurAlgorithm(algorithm);
        return this;
    }

    /**
     * @param windowBackground sets the background to draw before view hierarchy.
     *                         Can be used to draw Activity's window background if your root layout doesn't provide any background
     * @return {@link BlurView}
     */
    public BlurView windowBackground(@Nullable Drawable windowBackground) {
        blurController.setWindowBackground(windowBackground);
        return this;
    }

    //Used in edit mode and in case if no BlurController was set
    private BlurController createStubController() {
        return new BlurController() {
            @Override
            public void drawBlurredContent(Canvas canvas) {
            }

            @Override
            public void updateBlurViewSize() {
            }

            @Override
            public void onDrawEnd(Canvas canvas) {
            }

            @Override
            public void setBlurRadius(float radius) {
            }

            @Override
            public void setBlurAlgorithm(BlurAlgorithm algorithm) {
            }

            @Override
            public void setWindowBackground(@Nullable Drawable windowBackground) {
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
