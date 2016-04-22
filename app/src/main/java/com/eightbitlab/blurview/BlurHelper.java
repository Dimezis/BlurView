package com.eightbitlab.blurview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.view.View;

//TODO optimize memory allocation
public class BlurHelper {
    private RenderScript renderScript;
    private Canvas internalCanvas;
    private Bitmap internalBitmap;
    private Bitmap overlay;

    public BlurHelper(Context context, BlurView blurView) {
        renderScript = RenderScript.create(context);
        overlay = Bitmap.createBitmap(blurView.getMeasuredWidth(), blurView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
    }

    public boolean isInternalCanvas(Canvas canvas) {
        return internalCanvas == canvas;
    }

    public void prepare(View rootView) {
        internalBitmap = Bitmap.createBitmap(rootView.getMeasuredWidth(), rootView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        internalCanvas = new Canvas(internalBitmap);
        //draw whole view hierarchy on canvas
        rootView.draw(internalCanvas);
    }

    public Bitmap getInternalBitmap() {
        return internalBitmap;
    }

    public Bitmap blur(Bitmap background, View view) {
        final float radius = 20;

        Canvas canvas = new Canvas(overlay);
        canvas.translate(-view.getLeft(), -view.getTop());
        canvas.drawBitmap(background, 0, 0, null);

        Allocation overlayAllocation = Allocation.createFromBitmap(renderScript, overlay);
        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(renderScript, overlayAllocation.getElement());

        blur.setInput(overlayAllocation);
        blur.setRadius(radius);
        blur.forEach(overlayAllocation);
        overlayAllocation.copyTo(overlay);
        return overlay;
    }

    public void destroy() {
        renderScript.destroy();
    }
}
