package eightbitlab.com.blurview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class BlurView extends FrameLayout {
    private static final String TAG = BlurView.class.getSimpleName();

    protected BlurController blurController;

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
        if (isInEditMode()) {
            createStubControllerForEditMode();
        }
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.BlurView, defStyleAttr, 0);
        int defaultColor = ContextCompat.getColor(getContext(), android.R.color.transparent);
        overlayColor = a.getColor(R.styleable.BlurView_overlayColor, defaultColor);
        a.recycle();
    }

    @Override
    public void draw(Canvas canvas) {
        if (!blurController.isInternalCanvas(canvas)) {
            blurController.drawBlurredContent(canvas);
            drawColorOverlay(canvas);
            super.draw(canvas);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        blurController.onDrawEnd(canvas);
    }

    protected void drawColorOverlay(Canvas canvas) {
        canvas.drawColor(overlayColor);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        blurController.destroy();
    }

    public void setBlurController(@NonNull BlurController blurController) {
        this.blurController = blurController;
    }

    public void setOverlayColor(@ColorInt int overlayColor) {
        this.overlayColor = overlayColor;
        invalidate();
    }

    private void createStubControllerForEditMode() {
        blurController = new BlurController() {
            @Override
            public boolean isInternalCanvas(Canvas canvas) {
                return false;
            }

            @Override
            public void drawBlurredContent(Canvas canvas) {}

            @Override
            public void onDrawEnd(Canvas canvas) {}

            @Override
            public void updateBlur() {}

            @Override
            public void destroy() {}
        };
    }
}
