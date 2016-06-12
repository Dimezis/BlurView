package eightbitlab.com.blurview;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;

/**
 * Blur using RenderScript. Currently the fastest blur algorithm
 * but has additional overhead because of Bitmap copying to Allocation object.
 */
public final class RenderScriptBlur implements BlurAlgorithm {
    private RenderScript renderScript;
    private ScriptIntrinsicBlur blurScript;
    private boolean canModifyBitmap;

    public RenderScriptBlur(Context context, boolean canModifyBitmap) {
        this.canModifyBitmap = canModifyBitmap;
        renderScript = RenderScript.create(context);
        blurScript = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
    }

    /**
     * @param bitmap     bitmap to blur
     * @param blurRadius blur radius (1..25)
     * @return blurred bitmap
     */
    @Override
    public final Bitmap blur(Bitmap bitmap, float blurRadius) {
        Allocation inAllocation = Allocation.createFromBitmap(renderScript, bitmap);
        Bitmap outputBitmap;

        if (canModifyBitmap) {
            outputBitmap = bitmap;
        } else {
            outputBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        }

        //do not use inAllocation in forEach. it will cause visual artifacts on blurred Bitmap
        Allocation outAllocation = Allocation.createTyped(renderScript, inAllocation.getType());

        blurScript.setRadius(blurRadius);
        blurScript.setInput(inAllocation);
        blurScript.forEach(outAllocation);
        outAllocation.copyTo(outputBitmap);

        inAllocation.destroy();
        outAllocation.destroy();
        return outputBitmap;
    }

    @Override
    public void destroy() {
        blurScript.destroy();
        renderScript.destroy();
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
