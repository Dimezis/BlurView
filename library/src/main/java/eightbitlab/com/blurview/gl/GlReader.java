package eightbitlab.com.blurview.gl;

import android.graphics.Bitmap;

public class GlReader {
    static {
        System.loadLibrary("gl-reader");
    }

    public static native void readToBitmap(Bitmap srcBitmap);
}
