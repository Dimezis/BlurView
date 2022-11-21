package eightbitlab.com.blurview;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import androidx.annotation.NonNull;

// Serves purely as a marker of a Canvas used in BlurView
// to skip drawing itself and other BlurViews on the View hierarchy snapshot
public class BlurViewCanvas extends Canvas {
    public BlurViewCanvas(@NonNull Bitmap bitmap) {
        super(bitmap);
    }
}
