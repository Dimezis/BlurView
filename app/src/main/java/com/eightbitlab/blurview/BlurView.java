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
    public static final String TAG = "BlurView";
    private BlurHelper blurHelper;
    private Paint bitmapPaint;
    private View rootView;
    private Drawable windowBackground;

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

        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                blurHelper = new BlurHelper(getContext(), BlurView.this);
                blurHelper.setWindowBackground(windowBackground);
                bitmapPaint = new Paint();
                bitmapPaint.setFlags(Paint.FILTER_BITMAP_FLAG);
                blurHelper.setRootView(rootView);
                blurHelper.prepare();
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    public void setRootView(View view) {
        rootView = view;
    }

    public void setDependencyView(View view) {
        view.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                Log.d(TAG, "listener onScrollChanged()");
                reBlur();
            }
        });

        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Log.d(TAG, "listener onGlobalLayout()");
                reBlur();
            }
        });

        view.getViewTreeObserver().addOnDrawListener(new ViewTreeObserver.OnDrawListener() {
            @Override
            public void onDraw() {
                Log.d(TAG, "listener onDraw()");
                reBlur();
            }
        });
    }

    protected void reBlur() {
        if (blurHelper != null) {
            blurHelper.prepare();
            invalidate();
        }
    }

    @Override
    public void draw(Canvas canvas) {
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
    protected void onDraw(Canvas canvas) {
        Log.d(TAG, "onDraw()");
        super.onDraw(canvas);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        blurHelper.destroy();
    }

    public void setWindowBackground(Drawable windowBackgroundDrawable) {
        this.windowBackground = windowBackgroundDrawable;
    }
}
