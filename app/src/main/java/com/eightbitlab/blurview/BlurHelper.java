package com.eightbitlab.blurview;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.view.View;

//TODO optimize memory allocation
//TODO add fast blur option, to ger rid of renderscript Bitmap copy
public class BlurHelper {
    public static final float SCALE_FACTOR = 4f;
    private static final int RADIUS = 10;

    private RenderScript renderScript;
    private Canvas internalCanvas;
    private Bitmap internalBitmap;
    private Bitmap overlay;
    private ScriptIntrinsicBlur blurScript;
    private View rootView;
    private Context context;

    public BlurHelper(Context context, BlurView blurView) {
        this.context = context;
        renderScript = RenderScript.create(context);
        //downscale bitmap
        overlay = Bitmap.createBitmap((int) (blurView.getMeasuredWidth() / SCALE_FACTOR),
                (int) (blurView.getMeasuredHeight() / SCALE_FACTOR), Bitmap.Config.ARGB_8888);

        blurScript = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        blurScript.setRadius(RADIUS);
    }

    public boolean isInternalCanvas(Canvas canvas) {
        return internalCanvas == canvas;
    }

    public void setRootView(View view) {
        rootView = view;
        internalBitmap = Bitmap.createBitmap(rootView.getMeasuredWidth(), rootView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
    }

    public void prepare() {
        internalCanvas = new Canvas(internalBitmap);
        //draw whole view hierarchy on canvas
        rootView.draw(internalCanvas);
    }

    public Bitmap getInternalBitmap() {
        return internalBitmap;
    }

    public Bitmap blur(Bitmap background, View view) {
        Canvas canvas = new Canvas(overlay);
        //TODO count margin/padding. or get rid of full rootView
        canvas.translate(-view.getLeft() / SCALE_FACTOR,
                (-view.getTop() - getNavigationBarHeight() - getStatusBarHeight()) / SCALE_FACTOR);
        canvas.scale(1 / SCALE_FACTOR, 1 / SCALE_FACTOR);
        canvas.drawBitmap(background, 0, 0, null);

        return FastBlur.doBlur(overlay, RADIUS, true);
//        return renderScriptBlur();
    }

    private Bitmap renderScriptBlur() {
        Allocation overlayAllocation = Allocation.createFromBitmap(renderScript, overlay);
        blurScript.setInput(overlayAllocation);
        blurScript.forEach(overlayAllocation);
        overlayAllocation.copyTo(overlay);
        return overlay;
    }

    public void destroy() {
        renderScript.destroy();
        context = null;
        rootView = null;
    }

    //TODO probably remove this later
    private int getNavigationBarHeight() {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    //TODO probably remove this later
    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}
