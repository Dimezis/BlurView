package com.eightbitlab.blurview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

//TODO redraw when underlying content changes
public class BlurView extends FrameLayout {
    public static final String TAG = "BlurView";
    private BlurHelper blurHelper;
    private Paint bitmapPaint;

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
                bitmapPaint = new Paint();
                blurHelper.prepare(getRootView());
                setDrawingCacheEnabled(false);
                setLayerType(LAYER_TYPE_SOFTWARE, null);
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    @Override
    public void draw(Canvas canvas) {
        Log.d(TAG, "draw()");
        if (isInEditMode()) {
            super.draw(canvas);
            return;
        }
        if (blurHelper.isInternalCanvas(canvas)) {
            super.draw(canvas);
        } else {
            canvas.drawBitmap(blurHelper.blur(blurHelper.getInternalBitmap(), this), getMatrix(), bitmapPaint);
        }
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
}
