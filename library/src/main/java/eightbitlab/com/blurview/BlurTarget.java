package eightbitlab.com.blurview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RecordingCanvas;
import android.graphics.RenderNode;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * A FrameLayout that records a snapshot of its children on a RenderNode.
 * This snapshot is used by the BlurView to apply blur effect.
 */
public class BlurTarget extends FrameLayout {
    // Need both RenderNode (API 29) and RenderEffect (API 31) to be available for a full hardware rendering pipeline
    static final boolean canUseHardwareRendering = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;

    RenderNode renderNode;

    {
        if (canUseHardwareRendering) {
            renderNode = new RenderNode("BlurViewHost node");
        }
    }

    public BlurTarget(@NonNull Context context) {
        super(context);
    }

    public BlurTarget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BlurTarget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public BlurTarget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        if (canUseHardwareRendering && canvas.isHardwareAccelerated()) {
            renderNode.setPosition(0, 0, getWidth(), getHeight());
            RecordingCanvas recordingCanvas = renderNode.beginRecording();
            super.dispatchDraw(recordingCanvas);
            renderNode.endRecording();
            canvas.drawRenderNode(renderNode);
        } else {
            super.dispatchDraw(canvas);
        }
    }
}
