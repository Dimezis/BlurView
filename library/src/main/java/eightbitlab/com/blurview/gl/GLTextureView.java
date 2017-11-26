package eightbitlab.com.blurview.gl;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

class GLTextureView extends TextureView implements TextureView.SurfaceTextureListener {

    private static final String TAG = GLTextureView.class.getSimpleName();
    private static final int EGL_OPENGL_ES2_BIT = 4;
    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

    EGLDisplay eglDisplay;
    EGLSurface eglSurface;
    EGLContext eglContext;
    EGL10 egl10;

    private RenderThread renderThread;
    Renderer renderer;

    TextureView.SurfaceTextureListener surfaceTextureListener;

    public void setRenderer(@NonNull Renderer renderer) {
        this.renderer = renderer;
    }

    public void setOnSurfaceTextureAvailableListener(SurfaceTextureListener surfaceTextureListener) {
        this.surfaceTextureListener = surfaceTextureListener;
    }

    @Override
    public void onDrawForeground(Canvas canvas) {
        super.onDrawForeground(canvas);
    }

    class RenderThread extends Thread {

        private final Object lock = new Object();
        private final SurfaceTexture surfaceTexture;

        private boolean shouldRun = true;
        private boolean paused = false;

        RenderThread(SurfaceTexture surfaceTexture) {
            this.surfaceTexture = surfaceTexture;
            setName("BlurViewRenderThread");
        }

        void finish() {
            shouldRun = false;
        }

        @Override
        public void run() {
            initGL(surfaceTexture);

            surfaceTextureListener.onSurfaceTextureAvailable(surfaceTexture, getWidth(), getHeight());

            while (shouldRun) {
                if (paused) {
                    waitForUnpause();
                }
                long start = System.currentTimeMillis();

                renderer.onDrawFrame();
                egl10.eglSwapBuffers(eglDisplay, eglSurface);

                long end = System.currentTimeMillis();
//                Log.d(TAG, " " + (end - start));
            }

            renderer.onDestroy();
        }

        private void waitForUnpause() {
            synchronized(lock) {
                try {
                    lock.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }

        void onStart() {
            synchronized(lock) {
                paused = false;
                lock.notify();
            }
        }

        void onStop() {
            synchronized(lock) {
                paused = true;
            }
        }
    }

    public GLTextureView(Context context) {
        super(context);
    }

    public GLTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GLTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    {
        super.setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
        renderThread = new RenderThread(texture);
        renderThread.start();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        surfaceTextureListener.onSurfaceTextureSizeChanged(surface, width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        surfaceTextureListener.onSurfaceTextureDestroyed(surface);
        if (renderThread != null) {
            renderThread.finish();
            renderThread.interrupt();
            try {
                renderThread.join();
            } catch (InterruptedException ignored) {
            }
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        surfaceTextureListener.onSurfaceTextureUpdated(surface);
    }

    public void onStop() {
        if (renderThread != null) {
            renderThread.onStop();
        }
    }

    public void onStart() {
        if (renderThread != null) {
            renderThread.onStart();
        }
    }

    void initGL(SurfaceTexture texture) {
        egl10 = (EGL10) EGLContext.getEGL();

        eglDisplay = egl10.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        if (eglDisplay == EGL10.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed " +
                    android.opengl.GLUtils.getEGLErrorString(egl10.eglGetError()));
        }

        int[] version = new int[2];
        if (!egl10.eglInitialize(eglDisplay, version)) {
            throw new RuntimeException("eglInitialize failed " +
                    android.opengl.GLUtils.getEGLErrorString(egl10.eglGetError()));
        }

        int[] configsCount = new int[1];
        EGLConfig[] configs = new EGLConfig[1];
        int[] configSpec = {
                EGL10.EGL_RENDERABLE_TYPE,
                EGL_OPENGL_ES2_BIT,
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 0,
                EGL10.EGL_STENCIL_SIZE, 0,
                EGL10.EGL_NONE
        };

        EGLConfig eglConfig = null;
        if (!egl10.eglChooseConfig(eglDisplay, configSpec, configs, 1, configsCount)) {
            throw new IllegalArgumentException("eglChooseConfig failed " +
                    android.opengl.GLUtils.getEGLErrorString(egl10.eglGetError()));
        } else if (configsCount[0] > 0) {
            eglConfig = configs[0];
        }
        if (eglConfig == null) {
            throw new RuntimeException("eglConfig not initialized");
        }

        int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE};
        eglContext = egl10.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
        eglSurface = egl10.eglCreateWindowSurface(eglDisplay, eglConfig, texture, null);

        if (eglSurface == null || eglSurface == EGL10.EGL_NO_SURFACE) {
            int error = egl10.eglGetError();
            if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
                Log.e(TAG, "eglCreateWindowSurface returned EGL10.EGL_BAD_NATIVE_WINDOW");
                return;
            }
            throw new RuntimeException("eglCreateWindowSurface failed " +
                    android.opengl.GLUtils.getEGLErrorString(error));
        }

        if (!egl10.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            throw new RuntimeException("eglMakeCurrent failed " +
                    android.opengl.GLUtils.getEGLErrorString(egl10.eglGetError()));
        }
    }
}
