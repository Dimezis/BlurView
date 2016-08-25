package eightbitlab.com.blurview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

/**
 * FrameLayout that blurs its underlying content.
 * Can have children and draw them over blurred background.
 */
public class BlurView extends FrameLayout {
    private static final String TAG = BlurView.class.getSimpleName();
    @ColorInt
    private static final int TRANSPARENT = 0x00000000;

    private BlurController blurController;

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
        createStubController();
        TypedArray a =
                getContext().obtainStyledAttributes(attrs, R.styleable.BlurView, defStyleAttr, 0);
        overlayColor = a.getColor(R.styleable.BlurView_blurOverlayColor, TRANSPARENT);
        a.recycle();

        //we need to draw even without background set
        setWillNotDraw(false);
    }

    @Override
    public void draw(Canvas canvas) {
        if (!blurController.isInternalCanvas(canvas)) {
            blurController.drawBlurredContent(canvas);
            drawColorOverlay(canvas);
            super.draw(canvas);
        }
    }

    /**
     * Can be used to stop blur auto update
     */
    public void stopAutoBlurUpdate() {
        blurController.stopAutoBlurUpdate();
    }

    /**
     * Can be used to resume blur auto update if it was stopped before
     */
    public void startAutoBlurUpdate() {
        blurController.startBlurAutoUpdate();
    }

    public void enableBlur() {
        blurController.enableBlur();
        blurController.updateBlurViewSize();
    }

    public void disableBlur() {
        blurController.disableBlur();
        blurController.updateBlurViewSize();
    }

    /**
     * Can be called to redraw blurred content manually
     */
    public void updateBlur() {
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        blurController.updateBlurViewSize();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        blurController.onDrawEnd(canvas);
    }

    private void drawColorOverlay(Canvas canvas) {
        canvas.drawColor(overlayColor);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAutoBlurUpdate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAutoBlurUpdate();
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
    public void setOverlayColor(@ColorInt int overlayColor) {
        this.overlayColor = overlayColor;
        invalidate();
    }

    /**
     * Check if blur effect enabled or disabled
     *
     * @return true if blur effect enabled or false if blur disabled
     */
    public boolean isEnabled() {
        return this.blurController.isBlurEnabled();
    }

    /**
     * @param rootView Root View where BlurView's underlying content starts drawing.
     *                 Can be Activity's root content layout (android.R.id.content)
     *                 or some of your custom root layouts.
     * @return ControllerSettings to setup needed params.
     */
    public ControllerSettings setupWith(View rootView) {
        BlurController blurController = new DefaultBlurController(this, rootView);
        setBlurController(blurController);
        return new ControllerSettings(blurController);
    }

    public static class ControllerSettings {
        BlurController blurController;

        private ControllerSettings(BlurController blurController) {
            this.blurController = blurController;
        }

        /**
         * @param radius sets the blur radius
         *               Default implementation uses field {@link DefaultBlurController#DEFAULT_BLUR_RADIUS}
         */
        public ControllerSettings blurRadius(float radius) {
            blurController.setBlurRadius(radius);
            return this;
        }

        /**
         * @param algorithm sets the blur algorithm
         *                  Default implementation uses {@link StackBlur}
         */
        public ControllerSettings blurAlgorithm(BlurAlgorithm algorithm) {
            blurController.setBlurAlgorithm(algorithm);
            return this;
        }

        /**
         * @param windowBackground sets the background to draw before view hierarchy.
         *                         Can be used to draw Activity's window background if your root layout doesn't provide any
         *                         background
         */
        public ControllerSettings windowBackground(@Nullable Drawable windowBackground) {
            blurController.setWindowBackground(windowBackground);
            return this;
        }

        /**
         * @param enabled enable or disable blur effect on start
         */

        public ControllerSettings enabledOnStart(boolean enabled) {
            if (enabled)
                blurController.enableBlur();
            else
                blurController.disableBlur();
            return this;
        }

    }

    /**
     * Used in edit mode and in case if no BlurController was set
     */
    private void createStubController() {
        blurController = new BlurController() {
            @Override
            public boolean isInternalCanvas(Canvas canvas) {
                return false;
            }

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
            public void stopAutoBlurUpdate() {
            }

            @Override
            public void startBlurAutoUpdate() {
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
            public void enableBlur() {

            }

            @Override
            public void disableBlur() {

            }

            @Override
            public boolean isBlurEnabled() {
                return true;
            }
        };
    }
}
