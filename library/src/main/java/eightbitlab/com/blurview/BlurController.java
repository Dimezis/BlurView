package eightbitlab.com.blurview;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

interface BlurController {
    float DEFAULT_SCALE_FACTOR = 8f;
    float DEFAULT_BLUR_RADIUS = 16f;

    /**
     * Draws blurred content on given canvas
     */
    void drawBlurredContent(Canvas canvas);

    /**
     * Must be used to notify Controller when BlurView's size has changed
     */
    void updateBlurViewSize();

    /**
     * Must be called by View when it ends its drawing
     */
    void onDrawEnd(Canvas canvas);

    /**
     * @param radius sets the blur radius
     */
    void setBlurRadius(float radius);

    /**
     * @param algorithm sets the blur algorithm
     */
    void setBlurAlgorithm(BlurAlgorithm algorithm);

    /**
     * @param windowBackground sets the background to draw before view hierarchy.
     *                         Can be used to draw Activity's window background if your root layout doesn't provide any background
     */
    void setWindowBackground(@Nullable Drawable windowBackground);

    /**
     * Frees allocated resources
     */
    void destroy();

    /**
     * Enables/disables the blur. Enabled by default
     */
    void setBlurEnabled(boolean enabled);

    /**
     * Can be used to stop blur auto update or resume if it was stopped before.
     * Enabled by default.
     */
    void setBlurAutoUpdate(boolean enabled);
}
