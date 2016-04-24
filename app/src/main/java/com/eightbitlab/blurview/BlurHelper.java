package com.eightbitlab.blurview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

public class BlurHelper {
    public float scaleFactor = 10f;
    public int blurRadius = 6;

    private Canvas internalCanvas;
    private Canvas overlayCanvas;
    private Bitmap internalBitmap;
    private Bitmap overlay;
    private View rootView;
    /**
     * By default, window's background is not drawn on canvas. We need to draw in manually
     */
    private Drawable windowBackground;

    public BlurHelper(BlurView blurView) {
        int measuredWidth = blurView.getMeasuredWidth();
        int measuredHeight = blurView.getMeasuredHeight();

        //downscale overlay (blurred) bitmap
        overlay = Bitmap.createBitmap((int) (measuredWidth / scaleFactor),
                (int) (measuredHeight / scaleFactor), Bitmap.Config.ARGB_8888);
        overlayCanvas = new Canvas(overlay);

        //draw starting from blurView's position
        internalBitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888);
        internalCanvas = new Canvas(internalBitmap);
        internalCanvas.translate(-blurView.getLeft() / scaleFactor, -blurView.getTop() / scaleFactor);
        internalCanvas.scale(1 / scaleFactor, 1 / scaleFactor);
    }

    public boolean isInternalCanvas(Canvas canvas) {
        return internalCanvas == canvas;
    }

    public void drawUnderlyingViews() {
        //draw whole view hierarchy on canvas
        windowBackground.draw(internalCanvas);
        rootView.draw(internalCanvas);
    }

    public Bitmap getInternalBitmap() {
        return internalBitmap;
    }

    public Bitmap blur() {
        overlayCanvas.drawBitmap(internalBitmap, 0, 0, null);
        return FastBlur.doBlur(overlay, blurRadius, true);
    }

    public void destroy() {
        rootView = null;
        overlay.recycle();
        internalBitmap.recycle();
    }

    public void setRootView(View view) {
        rootView = view;
    }

    public void setWindowBackground(Drawable windowBackground) {
        this.windowBackground = windowBackground;
    }
}
