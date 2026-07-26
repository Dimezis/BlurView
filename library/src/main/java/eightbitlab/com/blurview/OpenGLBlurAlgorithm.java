package eightbitlab.com.blurview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import eightbitlab.com.blurview.internal.OpenGLBlurPipeline;

/**
 * Blur algorithm for API 29-30. The snapshot is captured into a software bitmap by the controller,
 * blurred by OpenGL into a HardwareBuffer-backed bitmap, and drawn the same frame. All work runs on
 * the UI thread, so the blur never trails the content. The OpenGL blur replaces the deprecated
 * RenderScript blur; the snapshot is a software draw, with the same limitations as the RenderScript
 * path (TextureView content and hardware bitmaps may not render).
 */
@RequiresApi(Build.VERSION_CODES.Q)
class OpenGLBlurAlgorithm implements BlurAlgorithm {

    // Swap-chain depth. Two leases are held at once (current + previous, since HWUI's RenderThread may
    // still sample the previous frame's buffer), and the next frame is acquired while both are held -
    // 3 images acquired at the peak. The +1 leaves the EGL producer a buffer to render into. Fewer
    // than 3 makes acquireLatestImage throw once all are held.
    private static final int BUFFER_COUNT = 4;

    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);

    private OpenGLBlurPipeline pipeline;
    private OpenGLBlurPipeline.Lease currentLease;
    private OpenGLBlurPipeline.Lease previousLease;

    @Override
    public Bitmap blur(@NonNull Bitmap snapshot, float blurRadius) {
        if (pipeline == null) {
            pipeline = new OpenGLBlurPipeline(BUFFER_COUNT);
        }
        pipeline.setSize(snapshot.getWidth(), snapshot.getHeight());
        OpenGLBlurPipeline.Lease lease = pipeline.render(snapshot, blurRadius);
        if (lease == null) {
            // Context could not be made current or no frame was available - draw the unblurred snapshot.
            return snapshot;
        }
        // Hold the previous lease one extra frame: HWUI's RenderThread may still sample it.
        if (previousLease != null) {
            previousLease.close();
        }
        previousLease = currentLease;
        currentLease = lease;
        return lease.bitmap;
    }

    @Override
    public boolean canModifyBitmap() {
        return false;
    }

    @NonNull
    @Override
    public Bitmap.Config getSupportedBitmapConfig() {
        return Bitmap.Config.ARGB_8888;
    }

    @Override
    public void render(@NonNull Canvas canvas, @NonNull Bitmap bitmap) {
        canvas.drawBitmap(bitmap, 0f, 0f, paint);
    }

    // Released on detach, rebuilt lazily on the next blur.
    @Override
    public void onDetached() {
        if (currentLease != null) {
            currentLease.close();
            currentLease = null;
        }
        if (previousLease != null) {
            previousLease.close();
            previousLease = null;
        }
        if (pipeline != null) {
            pipeline.release();
            pipeline = null;
        }
    }

    @Override
    public void destroy() {
        onDetached();
    }

    /**
     * Initialises the hardware capture path: sets up the internal {@link OpenGLBlurPipeline}'s
     * {@link eightbitlab.com.blurview.internal.ExternalTexture} at the given size and returns the
     * {@link Surface} that a {@link android.graphics.HardwareRenderer} should render into.
     * Call once on setup and again whenever the capture size changes.
     */
    @Nullable
    Surface getCaptureSurface(int width, int height) {
        if (pipeline == null) {
            pipeline = new OpenGLBlurPipeline(BUFFER_COUNT);
        }
        pipeline.setSize(width, height);
        return pipeline.getCaptureSurface(width, height);
    }

    /**
     * Blurs the latest frame produced by the {@link android.graphics.HardwareRenderer} into the
     * pipeline's {@link eightbitlab.com.blurview.internal.ExternalTexture}. Returns the blurred
     * HardwareBuffer-backed bitmap, or {@code null} if no frame was available.
     */
    @Nullable
    Bitmap blurExternal(float blurRadius) {
        if (pipeline == null) {
            return null;
        }
        OpenGLBlurPipeline.Lease lease = pipeline.renderExternal(blurRadius);
        if (lease == null) {
            return null;
        }
        if (previousLease != null) {
            previousLease.close();
        }
        previousLease = currentLease;
        currentLease = lease;
        return lease.bitmap;
    }
}
