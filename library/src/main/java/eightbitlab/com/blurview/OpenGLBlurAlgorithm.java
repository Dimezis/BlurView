package eightbitlab.com.blurview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import eightbitlab.com.blurview.internal.EglSetupException;
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

    // Latch set when EGL/pipeline bring-up fails, so we stop retrying every frame and fall back to the
    // unblurred snapshot instead of crashing. Reset on detach so a reattach (which rebuilds the
    // pipeline) can retry after a transient failure - e.g. a surface lost while backgrounded.
    private boolean pipelineUnavailable;

    @Override
    public Bitmap blur(@NonNull Bitmap snapshot, float blurRadius) {
        if (pipelineUnavailable) {
            return snapshot;
        }
        if (pipeline == null) {
            try {
                pipeline = new OpenGLBlurPipeline(BUFFER_COUNT);
            } catch (EglSetupException eglSetupFailed) {
                // Known EGL bring-up failure. Latch off and fall back to the unblurred snapshot so
                // blurring never crashes the host view.
                Log.e("BlurView", "OpenGL blur unavailable, falling back to unblurred snapshot", eglSetupFailed);
                pipelineUnavailable = true;
                return snapshot;
            }
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
        // The pipeline is rebuilt lazily on the next blur, so clear the latch to allow a retry after
        // a transient bring-up failure.
        pipelineUnavailable = false;
    }

    @Override
    public void destroy() {
        onDetached();
    }
}
