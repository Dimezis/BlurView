package eightbitlab.com.blurview;

import android.graphics.Bitmap;

public interface BlurAlgorithm {
    /**
     * @param bitmap bitmap to be blurred
     * @param blurRadius blur radius
     * @return blurred bitmap
     */
    Bitmap blur(Bitmap bitmap, int blurRadius);

    /**
     * Frees allocated resources
     */
    void destroy();

    /**
     * @return true if sent bitmap can be modified, false otherwise
     */
    boolean canModifyBitmap();
}
