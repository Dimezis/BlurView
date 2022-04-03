package eightbitlab.com.blurview;

import android.graphics.Bitmap;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * Leverages the new RenderEffect.createBlurEffect API to perform blur.
 * Hardware acceleration is supported.
 * Its performance and stability is not yet well studied, use at own risk.
 */
@RequiresApi(Build.VERSION_CODES.S)
public class RenderEffectBlur implements BlurAlgorithm {

    /**
     * This View uses the snapshot of the View hierarchy as a background, so then the RenderEffect
     * can be applied to it. The RenderEffect can't be applied to the BlurView, because it also blurs
     * ViewGroup's children.
     */
    private final View backgroundView;

    public RenderEffectBlur(BlurView blurView) {
        backgroundView = new View(blurView.getContext());

        blurView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                blurView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                ViewGroup.LayoutParams params = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        blurView.getMeasuredHeight()
                );
                blurView.addView(backgroundView, 0, params);
            }
        });
    }

    @Override
    public Bitmap blur(Bitmap bitmap, float blurRadius) {
        if (backgroundView.getBackground() == null) {
            BitmapDrawable background = new BitmapDrawable(backgroundView.getResources(), bitmap);
            backgroundView.setBackground(background);
        }
        // TODO alternatively, it's possible to blur a downscaled bitmap by creating a chain of
        //  createBitmapEffect -> createBlurEffect with BitmapEffect as an input.
        //  It will reduce the amount of pixels to blur, but will demonstrate the downscaling artifacts
        //  similar to other blur methods.
        //  It's also not clear whether having this kind of RenderEffect chain is more performant than
        //  a single createBlurEffect call, as it's quite hard to measure.
        RenderEffect blurEffect = RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.MIRROR);
        backgroundView.setRenderEffect(blurEffect);
        backgroundView.invalidate();
        return bitmap;
    }

    @Override
    public void destroy() {
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
        // There's no benefit in downscaling for this algorithm, because its input is the whole View's content
        return 1;
    }
}
