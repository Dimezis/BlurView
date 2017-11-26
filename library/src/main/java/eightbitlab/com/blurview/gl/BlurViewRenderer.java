package eightbitlab.com.blurview.gl;

import android.annotation.TargetApi;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.Build;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import java.util.concurrent.LinkedBlockingQueue;

import javax.microedition.khronos.opengles.GL10;

//TODO remove listener
public class BlurViewRenderer implements Renderer {

    static final Object SIGNAL = new Object();

    private final View blurView;
    private final SizeProvider sizeProvider;

    private ViewGroup rootView;

    Surface backgroundSurface;
    private SurfaceTexture backgroundTexture;
    private BlurShader blurShader;
    private Drawable windowBackground;
    private Rect relativeViewBounds = new Rect();

    private boolean isBlurEnabled = true;
    private boolean shouldTryToOffsetCoords = true;

    final LinkedBlockingQueue<Object> q = new LinkedBlockingQueue<>();

    //TODO ensure not called before onSurfaceChanged
    private final ViewTreeObserver.OnPreDrawListener drawListener = new ViewTreeObserver.OnPreDrawListener() {
        @Override
        public boolean onPreDraw() {
            if (backgroundSurface != null) {
                drawViewHierarchy();
                q.offer(SIGNAL);
            }
            return true;
        }
    };

    @TargetApi(Build.VERSION_CODES.N)
    BlurViewRenderer(ViewGroup rootView, View blurView, SizeProvider sizeProvider) {
        this.blurView = blurView;
        this.sizeProvider = sizeProvider;
        this.rootView = rootView;
    }

    void drawViewHierarchy() {
        if (isBlurEnabled) {
            Canvas canvas = backgroundSurface.lockCanvas(null);
            int restoreCount = canvas.save();
            setupCanvasMatrix(canvas);
            drawUnderlyingViews(canvas);
            canvas.restoreToCount(restoreCount);
            backgroundSurface.unlockCanvasAndPost(canvas);
        }
    }

    /**
     * setup matrix to draw starting from blurView's position
     */
    private void setupCanvasMatrix(Canvas canvas) {
        blurView.getDrawingRect(relativeViewBounds);

        if (shouldTryToOffsetCoords) {
            try {
                rootView.offsetDescendantRectToMyCoords(blurView, relativeViewBounds);
            } catch (IllegalArgumentException e) {
                // BlurView is not a child of the rootView (i.e. it's in Dialog)
                // Fallback to regular coordinates system
                shouldTryToOffsetCoords = false;
            }
        }

        float scaleFactorX = sizeProvider.widthScaleFactor();
        float scaleFactorY = sizeProvider.heightScaleFactor();

        float scaledLeftPosition = -relativeViewBounds.left / scaleFactorX;
        float scaledTopPosition = -relativeViewBounds.top / scaleFactorY;

        float scaledTranslationX = blurView.getTranslationX() / scaleFactorX;
        float scaledTranslationY = blurView.getTranslationY() / scaleFactorY;

        canvas.translate(scaledLeftPosition - scaledTranslationX, scaledTopPosition - scaledTranslationY);
        canvas.scale(1f / scaleFactorX, 1f / scaleFactorY);
    }

    /**
     * Draws whole view hierarchy on internal canvas
     */
    private void drawUnderlyingViews(Canvas canvas) {
        if (windowBackground != null) {
            windowBackground.draw(canvas);
        }
        blurView.setVisibility(View.INVISIBLE);
        rootView.draw(canvas);
        blurView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        int downscaledWidth = sizeProvider.downscaledWidth();
        int downscaledHeight = sizeProvider.downscaledHeight();

        int textureId = genTexture();

        backgroundTexture = new SurfaceTexture(textureId);
        backgroundTexture.setDefaultBufferSize(downscaledWidth, downscaledHeight);
        backgroundSurface = new Surface(backgroundTexture);

        blurShader = new BlurShader(downscaledWidth, downscaledHeight, textureId, sizeProvider);
    }

    //Called from background thread
    @Override
    public void onDrawFrame() {
        try {
            q.take();
        } catch (InterruptedException ignored) {
        }
        backgroundTexture.updateTexImage();
        blurShader.draw();
    }

    private int genTexture() {
        int[] textureRef = new int[1];
        GLES20.glGenTextures(1, textureRef, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureRef[0]);

        // Set texture default draw parameters
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        return textureRef[0];
    }

    @Override
    public void onDestroy() {
        backgroundSurface.release();
        backgroundTexture.release();
    }

    void setRootView(ViewGroup rootView) {
        this.rootView = rootView;
        rootView.getViewTreeObserver().addOnPreDrawListener(drawListener);
    }

    void setWindowBackground(@Nullable Drawable windowBackground) {
        this.windowBackground = windowBackground;
    }
}
