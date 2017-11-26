package eightbitlab.com.blurview.gl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class GLBlurView extends FrameLayout implements TextureView.SurfaceTextureListener {

    private SizeProvider sizeProvider;
    private ViewGroup rootView;
    private BlurViewRenderer renderer;
    private GLTextureView glTextureView;
    private Drawable windowBackground;

    public GLBlurView(Context context) {
        super(context);
    }

    public GLBlurView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GLBlurView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    {
        glTextureView = new GLTextureView(getContext(), null, 0);
        glTextureView.setOnSurfaceTextureAvailableListener(this);
        addView(glTextureView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    public void setRootView(@NonNull ViewGroup rootView) {
        this.rootView = rootView;
        //TODO set here to renderer too, but avoid setting it 2 times here and in onSurfaceTextureAvailable
    }

    //TODO texture not used
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
        sizeProvider = new SizeProvider(width, height);
        renderer = new BlurViewRenderer(rootView, this, sizeProvider);
        glTextureView.setRenderer(renderer);
        renderer.setWindowBackground(this.windowBackground);
        renderer.setRootView(rootView);
        renderer.onSurfaceChanged(width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    public void setWindowBackground(@Nullable Drawable windowBackground) {
        this.windowBackground = windowBackground;
    }

    public void onStart() {
        glTextureView.onStart();
    }

    public void onStop() {
        glTextureView.onStop();
    }
}
