package eightbitlab.com.blurview;

import android.graphics.Canvas;

public interface BlurController {
    /**
     * Used to distinct BlurController's Canvas from System Canvas.
     * A View that uses BlurController should draw only on System Canvas.
     * Otherwise their content will be blurred too.
     */
    boolean isInternalCanvas(Canvas canvas);

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

    void destroy();
}
