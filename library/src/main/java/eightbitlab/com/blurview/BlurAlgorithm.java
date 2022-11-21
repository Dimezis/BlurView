package eightbitlab.com.blurview;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import androidx.annotation.NonNull;

public interface BlurAlgorithm {
    /**
     * @param bitmap     bitmap to be blurred
     * @param blurRadius blur radius
     * @return blurred bitmap
     */
    Bitmap blur(@NonNull Bitmap bitmap, @NonNull float blurRadius);

    /**
     * Frees allocated resources
     */
    void destroy();

    /**
     * @return true if this algorithm returns the same instance of bitmap as it accepted
     * false if it creates a new instance.
     * <p>
     * If you return false from this method, you'll be responsible to swap bitmaps in your
     * {@link BlurAlgorithm#blur(Bitmap, float)} implementation
     * (assign input bitmap to your field and return the instance algorithm just blurred).
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

    float scaleFactor();

    void render(@NonNull Canvas canvas, @NonNull Bitmap bitmap);
}
