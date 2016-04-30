package com.eightbitlab.blurview;

import android.graphics.Bitmap;

public interface BlurAlgorithm {
    Bitmap blur(Bitmap bitmap, int blurRadius);
}
