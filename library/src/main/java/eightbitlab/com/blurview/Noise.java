package eightbitlab.com.blurview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;

import androidx.annotation.NonNull;

class Noise {
    // Built lazily from the application Context so no Activity/View context is retained process-wide.
    private static Paint noisePaint;

    static void apply(Canvas canvas, Context context, int width, int height) {
        if (noisePaint == null) {
            noisePaint = createNoisePaint(context.getApplicationContext());
        }
        canvas.drawRect(0, 0, width, height, noisePaint);
    }

    @NonNull
    private static Paint createNoisePaint(@NonNull Context appContext) {
        Bitmap alphaBitmap = getNoiseBitmap(appContext);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
        paint.setShader(new BitmapShader(alphaBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
        return paint;
    }

    @NonNull
    private static Bitmap getNoiseBitmap(@NonNull Context appContext) {
        Bitmap bitmap = BitmapFactory.decodeResource(appContext.getResources(), com.eightbitlab.blurview.R.drawable.blue_noise);
        Bitmap alphaBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(alphaBitmap);
        Paint paint = new Paint();
        paint.setAlpha(38); // 15% opacity
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return alphaBitmap;
    }
}
