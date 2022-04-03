package eightbitlab.com.blurview.gl;

import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.annotation.NonNull;

import com.otaliastudios.opengl.core.EglCore;
import com.otaliastudios.opengl.draw.GlDrawable;
import com.otaliastudios.opengl.draw.GlSquare;
import com.otaliastudios.opengl.program.GlFlatProgram;
import com.otaliastudios.opengl.surface.EglOffscreenSurface;

import eightbitlab.com.blurview.BlurAlgorithm;

public class GlBlur implements BlurAlgorithm {

    private EglCore eglCore;
    private int lastBitmapWidth = -1;
    private int lastBitmapHeight = -1;
    private GlDrawable quad;
    private GlFlatProgram shader;

    public GlBlur() {
    }

    @Override
    public Bitmap blur(Bitmap bitmap, float blurRadius) {
        if (!canReuseAllocation(bitmap)) {
            destroy();
            eglCore = new EglCore();
            EglOffscreenSurface glSurface = new EglOffscreenSurface(eglCore, bitmap.getWidth(), bitmap.getHeight());
            glSurface.makeCurrent();
            shader = new GlFlatProgram();
            // TODO not blurring anything, just testing the rendering pipeline
            shader.setColor(Color.RED);
            quad = new GlSquare();
            lastBitmapWidth = bitmap.getWidth();
            lastBitmapHeight = bitmap.getHeight();
        }
        shader.draw(quad);
        // TODO this is slow. Around 2ms for a ~192x36 image on SDK 31 emulator
        GlReader.readToBitmap(bitmap);

        return bitmap;
    }

    private boolean canReuseAllocation(Bitmap bitmap) {
        return bitmap.getHeight() == lastBitmapHeight && bitmap.getWidth() == lastBitmapWidth;
    }

    @Override
    public void destroy() {
        if (eglCore != null) {
            eglCore.release();
            shader.release();
            quad.release();
            eglCore = null;
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
}
