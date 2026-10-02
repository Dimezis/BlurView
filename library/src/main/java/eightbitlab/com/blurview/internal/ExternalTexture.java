package eightbitlab.com.blurview.internal;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * A {@code GL_TEXTURE_EXTERNAL_OES} texture bound to a {@link SurfaceTexture}, used as the
 * snapshot input for the hardware-capture blur path on API 29-30.
 * <p>
 * A {@link android.graphics.HardwareRenderer} renders the {@link android.graphics.RenderNode}
 * snapshot into {@link #getSurface()}, which feeds into the SurfaceTexture's buffer queue. Before
 * each blur pass, {@link #updateAndGetTransform()} calls
 * {@link SurfaceTexture#updateTexImage()} to advance to the latest frame and returns the
 * {@link SurfaceTexture#getTransformMatrix transform matrix} that the horizontal pass shader must
 * apply to UV coordinates.
 * <p>
 * All GL methods ({@link #init}, {@link #updateAndGetTransform}, {@link #release}) must be called
 * on the thread that owns the EGL context (i.e. after {@link EglCore#makeCurrent}).
 */
@RequiresApi(Build.VERSION_CODES.Q)
public final class ExternalTexture {

    private int textureId;
    private SurfaceTexture surfaceTexture;
    private Surface surface;
    private final float[] transformMatrix = new float[16];

    /** Creates the OES texture and SurfaceTexture. Must be called with the GL context current. */
    public void init() {
        int[] texIds = new int[1];
        GLES20.glGenTextures(1, texIds, 0);
        textureId = texIds[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        surfaceTexture = new SurfaceTexture(textureId);
        surface = new Surface(surfaceTexture);
    }

    /** Tells the producer ({@link android.graphics.HardwareRenderer}) what resolution to render at. */
    public void setSize(int width, int height) {
        surfaceTexture.setDefaultBufferSize(width, height);
    }

    /** Returns the {@link Surface} the HardwareRenderer should render into. */
    @NonNull
    public Surface getSurface() {
        return surface;
    }

    /**
     * Advances the texture to the latest frame from the HardwareRenderer and returns the transform
     * matrix to apply to UV coordinates in the shader. Must be called with the GL context current.
     */
    @NonNull
    public float[] updateAndGetTransform() {
        surfaceTexture.updateTexImage();
        surfaceTexture.getTransformMatrix(transformMatrix);
        return transformMatrix;
    }

    /** Returns the OES texture ID. */
    public int id() {
        return textureId;
    }

    /** Releases all GL and Surface resources. Must be called with the GL context current. */
    public void release() {
        if (surface != null) {
            surface.release();
            surface = null;
        }
        if (surfaceTexture != null) {
            surfaceTexture.release();
            surfaceTexture = null;
        }
        if (textureId != 0) {
            GLES20.glDeleteTextures(1, new int[]{textureId}, 0);
            textureId = 0;
        }
    }
}
