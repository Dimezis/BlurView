package com.eightbitlab.blurview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

public class BlurView extends FrameLayout {
    private static final String TAG = "BlurView";

    protected BlurHelper blurHelper;
    protected Paint bitmapPaint;

    private View rootView;
    private Drawable windowBackground;
    private boolean isDrawing;

    private Runnable setNotDrawingTask = new Runnable() {
        @Override
        public void run() {
            isDrawing = false;
        }
    };

    public BlurView(Context context) {
        super(context);
        init();
    }

    public BlurView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BlurView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        if (isInEditMode()) {
            return;
        }

        bitmapPaint = new Paint();
        bitmapPaint.setFlags(Paint.FILTER_BITMAP_FLAG);

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                //can create blurHelper only after layout completion
                blurHelper = new BlurHelper(BlurView.this);
                blurHelper.setWindowBackground(windowBackground);
                blurHelper.setRootView(rootView);
                blurHelper.drawUnderlyingViews();
                observeDrawCalls();
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    private void observeDrawCalls() {
        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                //ignore onPreDraw calls from this view
                if (!isDrawing) {
                    Log.d(TAG, "listener onPreDraw()");
                    reBlur();
                }
                return true;
            }
        });
    }

    protected void reBlur() {
        isDrawing = true;
        blurHelper.drawUnderlyingViews();
        invalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        isDrawing = true;
        Log.d(TAG, "draw()");
        if (isInEditMode()) {
            super.draw(canvas);
            return;
        }
        if (!blurHelper.isInternalCanvas(canvas)) {
            drawBlurredContent(canvas);
            super.draw(canvas);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        post(setNotDrawingTask);
    }

    protected void drawBlurredContent(Canvas canvas) {
        canvas.scale(1 * BlurHelper.SCALE_FACTOR, 1 * BlurHelper.SCALE_FACTOR);
        canvas.drawBitmap(blurHelper.blur(), getMatrix(), bitmapPaint);
        canvas.scale(1 / BlurHelper.SCALE_FACTOR, 1 / BlurHelper.SCALE_FACTOR);
        drawColorOverlay(canvas);
    }

    protected void drawColorOverlay(Canvas canvas) {
        canvas.drawColor(getContext().getResources().getColor(R.color.colorOverlay));
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        blurHelper.destroy();
    }

    public void setRootView(View view) {
        rootView = view;
    }

    /**
     * Use this method to pass windowBackground from your activity
     */
    public void setWindowBackground(Drawable windowBackgroundDrawable) {
        this.windowBackground = windowBackgroundDrawable;
    }
}
