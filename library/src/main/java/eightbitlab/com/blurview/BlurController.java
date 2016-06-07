package eightbitlab.com.blurview;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;

interface BlurController {
    float DEFAULT_SCALE_FACTOR = 8f;
    int DEFAULT_BLUR_RADIUS = 8;

    /**
     * Used to distinct BlurController's Canvas from System Canvas.
     * A View that uses BlurController should draw only on System Canvas.
     * Otherwise their content will be blurred too.
     */
    boolean isInternalCanvas(Canvas canvas);

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
     * Can be called to redraw blurred content manually
     */
    void updateBlur();

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
     * Can be used to stop blur auto update
     */
    void stopAutoBlurUpdate();

    /**
     * Can be used to start blur auto update
     */
    void startAutoBlurUpdate();

    /**
     * Frees allocated resources
     */
    void destroy();
}
