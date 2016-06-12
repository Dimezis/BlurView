package eightbitlab.com.blurview;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

public interface BlurAlgorithm {
    /**
     * @param bitmap     bitmap to be blurred
     * @param blurRadius blur radius
     * @return blurred bitmap
     */
    Bitmap blur(Bitmap bitmap, float blurRadius);

    /**
     * Frees allocated resources
     */
    void destroy();

    /**
     * @return true if sent bitmap can be modified, false otherwise
     */
    boolean canModifyBitmap();

    /**
     * Retrieve the {@link android.graphics.Bitmap.Config} on which the {@link BlurAlgorithm}
     * can actually work.
     *
     * @return bitmap config supported by the given blur algorithm.
     */
    @NonNull
    Bitmap.Config getSupportedBitmapConfig();
}
