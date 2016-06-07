package eightbitlab.com.blurview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

/**
 * FrameLayout that blurs its underlying content.
 * Can have children and draw them over blurred background.
 * <p/>
 * Must have {@link BlurController} to be set to work properly
 */
public class BlurView extends FrameLayout {
    private static final String TAG = BlurView.class.getSimpleName();

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
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.BlurView, defStyleAttr, 0);
        int defaultColor = ContextCompat.getColor(getContext(), android.R.color.transparent);
        overlayColor = a.getColor(R.styleable.BlurView_overlayColor, defaultColor);
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
     * Can be used to start blur auto update
     */
    public void startAutoBlurUpdate() {
        blurController.startAutoBlurUpdate();
    }


    /**
     * Can be called to redraw blurred content manually
     */
    public void updateBlur() {
        blurController.updateBlur();
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
        blurController.destroy();
    }

    public void setBlurController(@NonNull BlurController blurController) {
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

    //TODO mb move this into ControllerSettings
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

    //TODO mb add interface for this
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
         *                         Can be used to draw Activity's window background if your root layout doesn't provide any background
         */
        public ControllerSettings windowBackground(@Nullable Drawable windowBackground) {
            blurController.setWindowBackground(windowBackground);
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
            public void startAutoBlurUpdate() {

            }

            @Override
            public void updateBlur() {
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
        };
    }
}
