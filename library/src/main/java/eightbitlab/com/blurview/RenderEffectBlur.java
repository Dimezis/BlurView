package eightbitlab.com.blurview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Leverages the new RenderEffect.createBlurEffect API to perform blur.
 * Hardware accelerated.
 * Blur is performed on a separate thread - native RenderThread.
 * It doesn't block the Main thread, however it can still cause an FPS drop,
 * because it's just in a different part of the rendering pipeline.
 */
@RequiresApi(Build.VERSION_CODES.S)
public class RenderEffectBlur implements BlurAlgorithm {

    private final RenderNode node = new RenderNode("BlurViewNode");

    private int height, width;
    private float lastBlurRadius = 1f;

    @Nullable
    public BlurAlgorithm fallbackAlgorithm;
    private Context context;

    public RenderEffectBlur() {
    }

    @Override
    public Bitmap blur(@NonNull Bitmap bitmap, float blurRadius) {
        lastBlurRadius = blurRadius;

        if (bitmap.getHeight() != height || bitmap.getWidth() != width) {
            height = bitmap.getHeight();
            width = bitmap.getWidth();
            node.setPosition(0, 0, width, height);
        }
        Canvas canvas = node.beginRecording();
        canvas.drawBitmap(bitmap, 0, 0, null);
        node.endRecording();
        node.setRenderEffect(RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.MIRROR));
        // returning not blurred bitmap, because the rendering relies on the RenderNode
        return bitmap;
    }

    @Override
    public void destroy() {
        node.discardDisplayList();
        if (fallbackAlgorithm != null) {
            fallbackAlgorithm.destroy();
        }
    }

    @Override
    public boolean canModifyBitmap() {
        return true;
    }

    @NonNull
    @Override
    public Bitmap.Config getSupportedBitmapConfig() {
        return Bitmap.Config.ARGB_8888;
    }

    @Override
    public float scaleFactor() {
        return BlurController.DEFAULT_SCALE_FACTOR;
    }

    @Override
    public void render(@NonNull Canvas canvas, @NonNull Bitmap bitmap) {
        if (canvas.isHardwareAccelerated()) {
            canvas.drawRenderNode(node);
        } else {
            if (fallbackAlgorithm == null) {
                fallbackAlgorithm = new RenderScriptBlur(context);
            }
            fallbackAlgorithm.blur(bitmap, lastBlurRadius);
            fallbackAlgorithm.render(canvas, bitmap);
        }
    }

    void setContext(@NonNull Context context) {
        this.context = context;
    }
}
