package eightbitlab.com.blurview;

import static java.lang.Math.min;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * Blur using RenderScript, processed on GPU when device drivers support it.
 * Requires API 17+
 *
 * @deprecated because RenderScript is deprecated and its hardware acceleration is not guaranteed.
 * On API 31+ an alternative hardware accelerated blur implementation is automatically used.
 */
@Deprecated
public class RenderScriptBlur implements BlurAlgorithm {
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final RenderScript renderScript;
    private final ScriptIntrinsicBlur blurScript;
    private Allocation outAllocation;

    private int lastBitmapWidth = -1;
    private int lastBitmapHeight = -1;

    /**
     * @param context Context to create the {@link RenderScript}
     */
    public RenderScriptBlur(@NonNull Context context) {
        renderScript = RenderScript.create(context);
        blurScript = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
    }

    private boolean canReuseAllocation(@NonNull Bitmap bitmap) {
        return bitmap.getHeight() == lastBitmapHeight && bitmap.getWidth() == lastBitmapWidth;
    }

    /**
     * @param bitmap     bitmap to blur
     * @param blurRadius blur radius (1..25)
     * @return blurred bitmap
     */
    @Override
    public Bitmap blur(@NonNull Bitmap bitmap, float blurRadius) {
        try {
            //Allocation will use the same backing array of pixels as bitmap if created with USAGE_SHARED flag
            Allocation inAllocation = Allocation.createFromBitmap(renderScript, bitmap);

            if (!canReuseAllocation(bitmap)) {
                if (outAllocation != null) {
                    outAllocation.destroy();
                }
                outAllocation = Allocation.createTyped(renderScript, inAllocation.getType());
                lastBitmapWidth = bitmap.getWidth();
                lastBitmapHeight = bitmap.getHeight();
            }

            blurScript.setRadius(min(blurRadius, 25f));
            blurScript.setInput(inAllocation);
            //do not use inAllocation in forEach. it will cause visual artifacts on blurred Bitmap
            blurScript.forEach(outAllocation);
            outAllocation.copyTo(bitmap);

            inAllocation.destroy();
        } catch (Exception e) {
            // Can potentially crash because RenderScript context was released by someone else via RenderScript.releaseAllContexts()
            // Some Glide transformations can cause this.
            Log.e("BlurView", "RenderScript blur failed. Rendering unblurred snapshot", e);
        }
        return bitmap;
    }

    @Override
    public final void destroy() {
        blurScript.destroy();
        renderScript.destroy();
        if (outAllocation != null) {
            outAllocation.destroy();
        }
    }

    @Override
    public boolean canModifyBitmap() {
        return true;
    }

    @NonNull
    @Override
    public Bitmap.Config getSupportedBitmapConfig() {
        return Bitmap.Config.ARGB_8888;
    }

    @Override
    public void render(@NonNull Canvas canvas, @NonNull Bitmap bitmap) {
        canvas.drawBitmap(bitmap, 0f, 0f, paint);
    }
}
