package com.eightbitlab.blurview;

import android.graphics.Canvas;

public interface BlurController {
    boolean isSystemCanvas(Canvas canvas);

    void drawBlurredContent(Canvas canvas);

    void onDrawEnd(Canvas canvas);

    void updateBlur();

    void destroy();
}
