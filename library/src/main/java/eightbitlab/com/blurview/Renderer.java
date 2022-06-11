package eightbitlab.com.blurview;

import android.graphics.Canvas;

public interface Renderer {
    void init(int width, int height);

    Canvas canvas();

    Canvas prepareForViewSnapshot();

    void drawBlurredSnapshot(Canvas canvas);

    void blur(float blurRadius);

    boolean shouldSkipDrawing(Canvas canvas);
}