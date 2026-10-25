package eightbitlab.com.blurview;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Regression test for https://github.com/Dimezis/BlurView/issues/273
 * <p>
 * When the BlurView is resized synchronously while the root hierarchy is being drawn into the
 * blur snapshot, {@code init()} replaces {@code internalCanvas} between the {@code save()} and
 * {@code restore()} in {@code updateBlur()}. On the buggy implementation the {@code restore()}
 * runs on a fresh canvas with an empty save stack and throws
 * {@code IllegalStateException: Underflow in restore}.
 * <p>
 * This test reproduces that mechanism deterministically: a custom {@link BlurTarget} relays out
 * the BlurView to a new height from inside its own {@code draw()} - the point at which
 * {@code updateBlur()} is drawing the root into the snapshot canvas.
 * <p>
 * Runs only below API 31, where {@link PreDrawBlurController} is used; on API 31+ the
 * RenderEffect path is used and this scenario does not apply.
 */
@RunWith(AndroidJUnit4.class)
public class ResizeDuringDrawTest {

    private static final int WIDTH = 400;
    private static final int INITIAL_HEIGHT = 300;
    private static final int RESIZED_HEIGHT = 200;

    /**
     * A BlurTarget that synchronously resizes the sibling BlurView from inside its
     * draw pass to reproduce the reentrancy the issue describes.
     */
    static class ResizingTarget extends BlurTarget {
        View blurViewToResize;
        boolean armed;

        ResizingTarget(Context context) {
            super(context);
        }

        @Override
        public void draw(Canvas canvas) {
            super.draw(canvas);
            // Only react to the blur snapshot pass, and only once, to avoid infinite reentrancy.
            if (armed && canvas instanceof BlurViewCanvas) {
                armed = false;
                int h = blurViewToResize.getHeight() == RESIZED_HEIGHT ? INITIAL_HEIGHT : RESIZED_HEIGHT;
                // Synchronous layout -> BlurView.onSizeChanged -> updateBlurViewSize -> init(),
                // which replaces internalCanvas mid-draw.
                blurViewToResize.measure(
                        View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
                blurViewToResize.layout(0, 0, WIDTH, h);
            }
        }
    }

    @Test
    public void resize_during_draw_does_not_crash_with_underflow() throws Throwable {
        if (BlurTarget.canUseHardwareRendering) {
            // API 31+ uses RenderEffect, not PreDrawBlurController. Nothing to test here.
            return;
        }

        final AtomicReference<Throwable> thrown = new AtomicReference<>();

        try (ActivityScenario<ResizeDuringDrawTestActivity> scenario =
                     ActivityScenario.launch(ResizeDuringDrawTestActivity.class)) {

            scenario.onActivity(activity -> {
                try {
                    runScenario(activity, thrown);
                } catch (Throwable t) {
                    thrown.set(t);
                }
            });
        }

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        Throwable t = thrown.get();
        if (t != null) {
            if (t instanceof IllegalStateException
                    && String.valueOf(t.getMessage()).contains("Underflow in restore")) {
                fail(t.getMessage());
            }
            throw t;
        }
    }

    private void runScenario(Activity activity, AtomicReference<Throwable> thrown) {
        FrameLayout root = ((ResizeDuringDrawTestActivity) activity).contentRoot;
        Context context = activity;

        ResizingTarget target = new ResizingTarget(context);
        target.setLayoutParams(new FrameLayout.LayoutParams(WIDTH, INITIAL_HEIGHT));

        BlurView blurView = new BlurView(context);
        blurView.setLayoutParams(new ViewGroup.LayoutParams(WIDTH, INITIAL_HEIGHT));
        target.blurViewToResize = blurView;

        // BlurView must be a sibling positioned over the target. Put both under contentRoot,
        // and the BlurView also inside target so the target draw pass covers the same area.
        root.addView(target);
        root.addView(blurView);

        // Force a measure/layout so the controller initializes with a non-zero size.
        int wSpec = View.MeasureSpec.makeMeasureSpec(WIDTH, View.MeasureSpec.EXACTLY);
        int hSpec = View.MeasureSpec.makeMeasureSpec(INITIAL_HEIGHT, View.MeasureSpec.EXACTLY);
        root.measure(wSpec, hSpec);
        root.layout(0, 0, WIDTH, INITIAL_HEIGHT);

        BlurViewFacade facade = blurView.setupWith(target).setBlurRadius(10f);

        // Sanity: on API 29-30 this must be the PreDrawBlurController path.
        assertTrue("Expected PreDrawBlurController on this API level",
                blurView.blurController instanceof PreDrawBlurController);

        PreDrawBlurController controller = (PreDrawBlurController) blurView.blurController;

        // Arm the reentrant resize and run the blur snapshot pass. On the buggy code this throws
        // IllegalStateException: Underflow in restore inside updateBlur().
        target.armed = true;
        try {
            controller.updateBlur();
        } catch (Throwable t) {
            thrown.set(t);
        }
    }
}
