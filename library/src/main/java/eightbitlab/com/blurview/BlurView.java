package eightbitlab.com.blurview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import static eightbitlab.com.blurview.BlockingBlurController.TRANSPARENT;

/**
 * FrameLayout that blurs its underlying content.
 * Can have children and draw them over blurred background.
 */
public class BlurView extends FrameLayout {

    private static final String TAG = BlurView.class.getSimpleName();

    BlurController blurController = new NoOpController();

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
            Log.e(TAG, "BlurView can't be used in not hardware-accelerated window!");
        } else {
            blurController.setBlurAutoUpdate(true);
        }
    }

    /**
     * @param rootView root to start blur from.
     *                 Can be Activity's root content layout (android.R.id.content)
     *                 or (preferably) some of your layouts. The lower amount of Views are in the root, the better for performance.
     * @return {@link BlurView} to setup needed params.
     */
    public BlurViewFacade setupWith(@NonNull ViewGroup rootView) {
        BlurController blurController = new BlockingBlurController(this, rootView, overlayColor);
        this.blurController.destroy();
        this.blurController = blurController;

        return blurController;
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
        return blurController.setBlurAutoUpdate(enabled);
    }

    /**
     * @see BlurViewFacade#setBlurEnabled(boolean)
     */
    public BlurViewFacade setBlurEnabled(boolean enabled) {
        return blurController.setBlurEnabled(enabled);
    }
}
