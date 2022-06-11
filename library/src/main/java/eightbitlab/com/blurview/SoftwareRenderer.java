package eightbitlab.com.blurview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

class SoftwareRenderer implements Renderer {

    private final BlurAlgorithm blurAlgorithm;
    private BlurViewCanvas canvas;
    private Bitmap bitmap;
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);

    public SoftwareRenderer(BlurAlgorithm blurAlgorithm) {
        this.blurAlgorithm = blurAlgorithm;
    }

    @Override
    public void init(int width, int height) {
        bitmap = Bitmap.createBitmap(width, height, blurAlgorithm.getSupportedBitmapConfig());
        canvas = new BlurViewCanvas(bitmap);
    }

    @Override
    public Canvas canvas() {
        return canvas;
    }

    @Override
    public Canvas prepareForViewSnapshot() {
        // Nothing special needed
        return canvas;
    }

    @Override
    public void drawBlurredSnapshot(Canvas canvas) {
        canvas.drawBitmap(bitmap, 0, 0, paint);
    }

    @Override
    public void blur(float blurRadius) {
        bitmap = blurAlgorithm.blur(bitmap, blurRadius);
        if (!blurAlgorithm.canModifyBitmap()) {
            canvas.setBitmap(bitmap);
        }
    }

    @Override
    public boolean shouldSkipDrawing(Canvas canvas) {
        return canvas instanceof BlurViewCanvas;
    }
}
