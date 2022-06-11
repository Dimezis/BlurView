package eightbitlab.com.blurview;

import android.graphics.Bitmap;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * Leverages the new RenderEffect.createBlurEffect API to perform blur.
 * Hardware acceleration is supported.
 * Its performance and stability is not yet well studied, use at own risk.
 * There's a known downside - this BlurAlgorithm constantly triggers a redraw of the BlurView.
 */
@RequiresApi(Build.VERSION_CODES.S)
public class RenderEffectBlur implements BlurAlgorithm {

    public RenderEffectBlur() {
    }

    @Override
    public Bitmap blur(Bitmap bitmap, float blurRadius) {
        // No-op, not used
        return bitmap;
    }

    public void hardwareBlur(float blurRadius, RenderNode renderNode) {
        renderNode.setRenderEffect(RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.MIRROR));
    }

    @Override
    public void destroy() {
        // No-op
    }

    @Override
    public boolean canModifyBitmap() {
        // Not used
        return true;
    }

    @NonNull
    @Override
    public Bitmap.Config getSupportedBitmapConfig() {
        // Not used
        return Bitmap.Config.ARGB_8888;
    }

    @Override
    public float scaleFactor() {
        return 2;
    }
}
