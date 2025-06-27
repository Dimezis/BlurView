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
    private static Paint noisePaint;

    static void apply(Canvas canvas, Context context, int width, int height) {
        initPaint(context);
        canvas.drawRect(0, 0, width, height, noisePaint);
    }

    private static void initPaint(Context context) {
        if (noisePaint == null) {
            Bitmap alphaBitmap = getNoiseBitmap(context);
            noisePaint = new Paint();
            noisePaint.setAntiAlias(true);
            noisePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
            noisePaint.setShader(new BitmapShader(alphaBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
        }
    }

    @NonNull
    private static Bitmap getNoiseBitmap(Context context) {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), com.eightbitlab.blurview.R.drawable.blue_noise);
        Bitmap alphaBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(alphaBitmap);
        Paint paint = new Paint();
        paint.setAlpha(38); // 15% opacity
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return alphaBitmap;
    }
}
