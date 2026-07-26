package eightbitlab.com.blurview.internal;

import android.graphics.Bitmap;
import android.hardware.HardwareBuffer;
import android.media.Image;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Off-screen GL pipeline that blurs a snapshot and returns the result as a
 * {@link HardwareBuffer}-backed {@link Bitmap}. Owns the EGL context ({@link EglCore}), the output
 * {@link SwapChain}, the {@link SourceTexture}, the {@link RenderTexture} and the
 * {@link BlurProgram}; all must be created and used on a single thread - the caller's. The blur
 * strength is a {@link GaussianKernel} recomputed only when the radius changes.
 * <p>
 * Two input paths:
 * <ul>
 *   <li>{@link #render}: uploads a software {@link Bitmap} into a {@link SourceTexture} (CPU→GPU),
 *       then runs the two-pass Gaussian blur. Used on API &lt; 29 and as a fallback.</li>
 *   <li>{@link #renderExternal}: reads from an {@link ExternalTexture} that is fed by a
 *       {@link android.graphics.HardwareRenderer} via a {@link android.graphics.SurfaceTexture}
 *       (GPU→GPU, no CPU copy). Used on API 29-30.</li>
 * </ul>
 * <p>
 * Per frame (bitmap path): upload snapshot → horizontal Gaussian into {@link RenderTexture} →
 * vertical Gaussian to output surface → read back as HardwareBuffer bitmap.
 */
@RequiresApi(Build.VERSION_CODES.Q)
public final class OpenGLBlurPipeline {

    private static final int OUTPUT_FRAMEBUFFER = 0;
    private static final int[] OUTPUT_DISCARD = {GLES30.GL_COLOR};

    private final EglCore eglCore = new EglCore();
    private final SwapChain swapChain;
    private final GaussianKernel kernel = new GaussianKernel();
    private final SourceTexture source = new SourceTexture();
    private final RenderTexture horizontalPassTarget = new RenderTexture();

    private int outputWidth;
    private int outputHeight;

    private BlurProgram blurProgram;
    private BlurProgram externalBlurProgram;
    private ExternalTexture externalSource;
    private int quadVbo;
    private int quadVao;

    public OpenGLBlurPipeline(int bufferCount) {
        swapChain = new SwapChain(eglCore, bufferCount);
    }

    /**
     * Sets the output (and blur) resolution, rebuilding the output surface on change.
     */
    public void setSize(int newWidth, int newHeight) {
        if (newWidth == outputWidth && newHeight == outputHeight) {
            return;
        }
        outputWidth = newWidth;
        outputHeight = newHeight;
        swapChain.setSize(outputWidth, outputHeight);
    }

    /**
     * Blurs the given software bitmap and returns the result. The bitmap must match the resolution
     * set by the most recent {@link #setSize}.
     */
    @Nullable
    public Lease render(Bitmap bitmap, float blurRadius) {
        if (!swapChain.makeCurrent()) {
            return null;
        }
        ensureGlObjects();
        horizontalPassTarget.ensureSize(outputWidth, outputHeight);
        GLES30.glBindVertexArray(quadVao);

        source.upload(bitmap);
        kernel.ensureRadius(blurRadius);

        horizontalPassTarget.bind();
        blurProgram.draw(kernel, source.id(), outputWidth, outputHeight, BlurProgram.Axis.HORIZONTAL, false);

        bindOutputSurface();
        blurProgram.draw(kernel, horizontalPassTarget.texture(), outputWidth, outputHeight, BlurProgram.Axis.VERTICAL, true);

        swapChain.swapBuffers();
        GLES20.glFinish();
        return swapChain.acquireLease();
    }

    /**
     * Initialises the {@link ExternalTexture} (OES texture + SurfaceTexture) for the hardware
     * capture path, sets its buffer size to {@code width × height}, and returns the
     * {@link Surface} the caller should pass to
     * {@link android.graphics.HardwareRenderer#setSurface}. The Surface remains valid until
     * {@link #release} is called; subsequent calls with a different size only resize the buffer.
     * Must be called with the GL context current (via {@link SwapChain#makeCurrent}).
     */
    @Nullable
    public Surface getCaptureSurface(int width, int height) {
        if (!swapChain.makeCurrent()) {
            return null;
        }
        ensureGlObjects();
        if (externalSource == null) {
            externalSource = new ExternalTexture();
            externalSource.init();
            externalBlurProgram = new BlurProgram(true);
        }
        externalSource.setSize(width, height);
        return externalSource.getSurface();
    }

    /**
     * Blurs the latest frame from the {@link ExternalTexture} (produced by
     * {@link android.graphics.HardwareRenderer#createRenderRequest}) and returns the result.
     * Calls {@link android.graphics.SurfaceTexture#updateTexImage} to advance to the latest
     * frame — if the HardwareRenderer hasn't finished yet this uses the previous frame (one frame
     * behind), which is imperceptible for a blur effect.
     */
    @Nullable
    public Lease renderExternal(float blurRadius) {
        if (!swapChain.makeCurrent()) {
            return null;
        }
        ensureGlObjects();
        if (externalSource == null) {
            return null;
        }
        horizontalPassTarget.ensureSize(outputWidth, outputHeight);
        GLES30.glBindVertexArray(quadVao);

        float[] transform = externalSource.updateAndGetTransform();
        kernel.ensureRadius(blurRadius);

        // Horizontal pass: sample from OES texture (GPU-to-GPU, no CPU copy)
        horizontalPassTarget.bind();
        externalBlurProgram.drawExternal(kernel, externalSource.id(), transform, outputWidth, outputHeight);

        // Vertical pass: flipY=false because the SurfaceTexture transform matrix already maps the
        // content to GL Y-up orientation. The software path needs flipY=true to compensate for
        // GLUtils.texImage2D not flipping Y, but the OES path has no such inversion.
        bindOutputSurface();
        blurProgram.draw(kernel, horizontalPassTarget.texture(), outputWidth, outputHeight, BlurProgram.Axis.VERTICAL, false);

        swapChain.swapBuffers();
        GLES20.glFinish();
        return swapChain.acquireLease();
    }

    public void release() {
        swapChain.makeCurrent();
        if (blurProgram != null) {
            blurProgram.release();
            GLES20.glDeleteBuffers(1, new int[]{quadVbo}, 0);
            GLES30.glDeleteVertexArrays(1, new int[]{quadVao}, 0);
            blurProgram = null;
            quadVbo = 0;
            quadVao = 0;
        }
        if (externalBlurProgram != null) {
            externalBlurProgram.release();
            externalBlurProgram = null;
        }
        if (externalSource != null) {
            externalSource.release();
            externalSource = null;
        }
        source.release();
        horizontalPassTarget.release();
        swapChain.release();
        eglCore.release();
    }

    private void bindOutputSurface() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, OUTPUT_FRAMEBUFFER);
        GLES20.glViewport(0, 0, outputWidth, outputHeight);
        GLES30.glInvalidateFramebuffer(GLES30.GL_FRAMEBUFFER, 1, OUTPUT_DISCARD, 0);
    }

    private void ensureGlObjects() {
        if (blurProgram != null) {
            return;
        }
        blurProgram = new BlurProgram();
        quadVbo = GlUtils.createFullscreenQuad();
        quadVao = GlUtils.createQuadVao(quadVbo);
    }

    /**
     * A produced frame. The bitmap is valid until {@link #close} returns its image to the reader.
     */
    public static final class Lease {
        public final Bitmap bitmap;
        private final Image image;
        private final HardwareBuffer buffer;

        Lease(Image image, HardwareBuffer buffer, Bitmap bitmap) {
            this.image = image;
            this.buffer = buffer;
            this.bitmap = bitmap;
        }

        public void close() {
            bitmap.recycle();
            buffer.close();
            image.close();
        }
    }
}
