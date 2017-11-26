package eightbitlab.com.blurview.gl;

public interface Renderer {
    void onSurfaceChanged(int width, int height);

    void onDrawFrame();

    void onDestroy();
}
