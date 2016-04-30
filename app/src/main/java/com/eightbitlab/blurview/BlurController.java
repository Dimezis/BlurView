package com.eightbitlab.blurview;

import android.graphics.Bitmap;
import android.graphics.Canvas;

public interface BlurController {
    Bitmap getBlurredBitmap();

    boolean isInternalCanvas(Canvas canvas);

    void drawBlurredContent(Canvas canvas);

    void onDrawEnd(Canvas canvas);

    void destroy();
}
