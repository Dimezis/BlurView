package eightbitlab.com.blurview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;

import androidx.annotation.NonNull;

/** Servers as a marker of a Canvas used in BlurView to skip drawing
 * itself and other BlurViews on the View hierarchy snapshot
 *
 * Also used to avoid BlurView crashing when the {@code rootView} call
 * {@link Canvas#save()} and {@link Canvas#restore()} an unequal amount of times.
 * */
public class BlurViewCanvas extends Canvas {
    private int saveCountInitial;

    public BlurViewCanvas(@NonNull Bitmap bitmap) {
        super(bitmap);
    }

    public void saveRoot() {
        saveCountInitial = this.save();
    }

    @Override
    public void restore() {
        if (getSaveCount() <= saveCountInitial) {
            Log.e("BlurView", "rootView has more restores than saves",
                    new IllegalStateException("Underflow in restore - more restores than saves"));
            return;
        }
        super.restore();
    }

    @Override
    public void restoreToCount(int saveCount) {
        super.restoreToCount(Math.max(saveCount, saveCountInitial));
    }

    public void restoreRoot() {
        if (getSaveCount() > saveCountInitial + 1) {
            Log.e("BlurView", "rootView has more saves than restores");
        }
        super.restoreToCount(saveCountInitial);
        saveCountInitial = 0;
    }
}
