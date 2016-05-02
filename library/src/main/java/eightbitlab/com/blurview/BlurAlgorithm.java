package eightbitlab.com.blurview;

import android.graphics.Bitmap;

public interface BlurAlgorithm {
    Bitmap blur(Bitmap bitmap, int blurRadius);

    /**
     * Free your resources here
     */
    void destroy();

    boolean canReuseBitmap();
}
