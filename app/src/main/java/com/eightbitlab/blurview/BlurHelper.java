package com.eightbitlab.blurview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

//TODO draw only needed part of View hierarchy
public class BlurHelper {
    public static final float SCALE_FACTOR = 8f;
    private static final int BLUR_RADIUS = 8;

    private Canvas internalCanvas;
    private Bitmap internalBitmap;
    private Bitmap overlay;
    private View rootView;
    private BlurView blurView;
    /**
     * By default, window's background is not drawn on canvas. We need to draw in manually
     */
    private Drawable windowBackground;

    public BlurHelper(BlurView blurView) {
        //downscale bitmap
        overlay = Bitmap.createBitmap((int) (blurView.getMeasuredWidth() / SCALE_FACTOR),
                (int) (blurView.getMeasuredHeight() / SCALE_FACTOR), Bitmap.Config.ARGB_8888);

        this.blurView = blurView;
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
        Canvas overlayCanvas = new Canvas(overlay);
        //move to blurView's position
        overlayCanvas.translate(-blurView.getLeft() / SCALE_FACTOR, -blurView.getTop() / SCALE_FACTOR);
        overlayCanvas.scale(1 / SCALE_FACTOR, 1 / SCALE_FACTOR);
        overlayCanvas.drawBitmap(internalBitmap, 0, 0, null);

        return FastBlur.doBlur(overlay, BLUR_RADIUS, true);
    }

    public void destroy() {
        rootView = null;
        blurView = null;
        overlay.recycle();
        internalBitmap.recycle();
    }

    public void setRootView(View view) {
        rootView = view;
        internalBitmap = Bitmap.createBitmap(rootView.getMeasuredWidth(), rootView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        internalCanvas = new Canvas(internalBitmap);
    }

    public void setWindowBackground(Drawable windowBackground) {
        this.windowBackground = windowBackground;
    }
}
