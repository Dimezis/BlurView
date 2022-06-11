package eightbitlab.com.blurview;

import android.graphics.Canvas;
import android.graphics.RecordingCanvas;
import android.graphics.RenderNode;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.HashSet;
import java.util.Set;

@RequiresApi(Build.VERSION_CODES.S)
class HardwareRenderer implements Renderer {

    private static final Set<Canvas> usedCanvases = new HashSet<>();

    private final RenderEffectBlur blur;
    private final RenderNode node = new RenderNode("BlurViewNode");
    private RecordingCanvas current;

    public HardwareRenderer(RenderEffectBlur blur) {
        this.blur = blur;
    }

    @Override
    public void init(int width, int height) {
        node.setPosition(0, 0, width, height);
    }

    @Override
    public Canvas canvas() {
        return current;
    }

    @Override
    public Canvas prepareForViewSnapshot() {
        usedCanvases.remove(current);
        RecordingCanvas canvas = node.beginRecording();
        usedCanvases.add(canvas);
        current = canvas;
        return canvas;
    }

    @Override
    public void drawBlurredSnapshot(Canvas canvas) {
        canvas.drawRenderNode(node);
    }

    @Override
    public void blur(float blurRadius) {
        node.endRecording();
        usedCanvases.remove(current);
        blur.hardwareBlur(blurRadius, node);
    }

    @Override
    public boolean shouldSkipDrawing(Canvas canvas) {
        return usedCanvases.contains(canvas);
    }
}
