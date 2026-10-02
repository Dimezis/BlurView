package eightbitlab.com.blurview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RecordingCanvas;
import android.graphics.RenderNode;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * A FrameLayout that records a snapshot of its children on a RenderNode.
 * This snapshot is used by the BlurView to apply blur effect.
 */
public class BlurTarget extends FrameLayout {
    // RenderNode is available from API 29. The snapshot it records is used by both the RenderEffect
    // (31+) and the hardware capture path for the OpenGL (29-30) blur.
    static final boolean canRecordRenderNode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    // RenderEffect is available from API 31, enabling the fully hardware-accelerated blur path.
    static final boolean canUseHardwareRendering = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    // API 29-30: RenderEffect is not available, so the OpenGL path is used.
    // This is the only path that reads contentGeneration; the other paths leave it untouched.
    static final boolean usesOpenGLBlur = canRecordRenderNode && !canUseHardwareRendering;

    @Nullable
    RenderNode renderNode;

    // Content version counter, read across frames by the OpenGL blur controller (UI thread) to skip
    // re-capturing when nothing changed. Bumped on every change to the target's drawn content:
    // a re-record in dispatchDraw, and a descendant invalidation (which does not re-run dispatchDraw).
    // Sibling BlurView overlays are not descendants, so their own redraws do not bump it - without
    // that property the controller's invalidate() on each blurred frame would re-trigger itself.
    int contentGeneration;

    {
        if (canRecordRenderNode) {
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
        if (canRecordRenderNode && canvas.isHardwareAccelerated()) {
            renderNode.setPosition(0, 0, getWidth(), getHeight());
            RecordingCanvas recordingCanvas = renderNode.beginRecording();
            super.dispatchDraw(recordingCanvas);
            renderNode.endRecording();
            if (usesOpenGLBlur) {
                contentGeneration++;
            }
            canvas.drawRenderNode(renderNode);
        } else {
            super.dispatchDraw(canvas);
        }
    }

    // A descendant invalidation (e.g. a scrolling list) changes the target's content without
    // re-running dispatchDraw, so the OpenGL path needs this as a separate capture trigger.
    @Override
    public void onDescendantInvalidated(@NonNull View child, @NonNull View descendant) {
        super.onDescendantInvalidated(child, descendant);
        if (usesOpenGLBlur) {
            contentGeneration++;
        }
    }
}
