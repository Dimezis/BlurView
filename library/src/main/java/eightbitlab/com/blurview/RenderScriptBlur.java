package eightbitlab.com.blurview;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;

/**
 * Blur using RenderScript, processed on GPU. Currently the fastest blur algorithm.
 */
public final class RenderScriptBlur implements BlurAlgorithm {
    private RenderScript renderScript;
    private ScriptIntrinsicBlur blurScript;
    private Allocation outAllocation;

    private boolean canModifyBitmap;

    private int lastBitmapWidth = -1;
    private int lastBitmapHeight = -1;

    public RenderScriptBlur(Context context, boolean canModifyBitmap) {
        this.canModifyBitmap = canModifyBitmap;
        renderScript = RenderScript.create(context);
        blurScript = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
    }

    private boolean canReuseAllocation(Bitmap bitmap) {
        return bitmap.getHeight() == lastBitmapHeight && bitmap.getWidth() == lastBitmapWidth;
    }

    /**
     * @param bitmap     bitmap to blur
     * @param blurRadius blur radius (1..25)
     * @return blurred bitmap
     */
    @Override
    public final Bitmap blur(Bitmap bitmap, float blurRadius) {
        //Allocation will use the same backing array of pixels as bitmap if created with USAGE_SHARED flag
        Allocation inAllocation = Allocation.createFromBitmap(renderScript, bitmap);
        Bitmap outputBitmap;

        if (canModifyBitmap) {
            outputBitmap = bitmap;
        } else {
            outputBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        }

        if (!canReuseAllocation(bitmap)) {
            if (outAllocation != null) {
                outAllocation.destroy();
            }
            outAllocation = Allocation.createTyped(renderScript, inAllocation.getType());
            lastBitmapWidth = bitmap.getWidth();
            lastBitmapHeight = bitmap.getHeight();
        }

        blurScript.setRadius(blurRadius);
        blurScript.setInput(inAllocation);
        //do not use inAllocation in forEach. it will cause visual artifacts on blurred Bitmap
        blurScript.forEach(outAllocation);
        outAllocation.copyTo(outputBitmap);

        inAllocation.destroy();
        return outputBitmap;
    }

    @Override
    public void destroy() {
        blurScript.destroy();
        renderScript.destroy();
        if (outAllocation != null) {
            outAllocation.destroy();
        }
    }

    @Override
    public boolean canModifyBitmap() {
        return canModifyBitmap;
    }

    @NonNull
    @Override
    public Bitmap.Config getSupportedBitmapConfig() {
        return Bitmap.Config.ARGB_8888;
    }
}
