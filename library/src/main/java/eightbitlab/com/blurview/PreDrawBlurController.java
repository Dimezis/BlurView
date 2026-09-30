package eightbitlab.com.blurview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Blur Controller that handles all blur logic for the attached View.
 * It honors View size changes, View animation and Visibility changes.
 * <p>
 * The basic idea is to draw the view hierarchy on a bitmap, excluding the attached View,
 * then blur and draw it on the system Canvas.
 * <p>
 * It uses {@link ViewTreeObserver.OnPreDrawListener} to detect when
 * blur should be updated.
 * <p>
 */
public final class PreDrawBlurController implements BlurController {

    @ColorInt
    public static final int TRANSPARENT = 0;

    private float blurRadius = DEFAULT_BLUR_RADIUS;

    private final BlurAlgorithm blurAlgorithm;
    private final float scaleFactor;
    private final boolean applyNoise;
    private BlurViewCanvas internalCanvas;
    // The software snapshot the rootView is drawn into. Reused across frames as the blur input.
    private Bitmap internalBitmap;
    // The algorithm's blur result, drawn by draw(). Same instance as internalBitmap for in-place
    // algorithms (RenderScript), a separate HardwareBuffer bitmap for OpenGL.
    @Nullable
    private Bitmap displayBitmap;

    @SuppressWarnings("WeakerAccess")
    final View blurView;
    private int overlayColor;
    private final BlurTarget rootView;
    private final int[] rootLocation = new int[2];
    private final int[] blurViewLocation = new int[2];

    // Capture-skipping state, see shouldUpdate.
    private int lastGeneration = -1;
    private int lastLeft = Integer.MIN_VALUE;
    private int lastTop = Integer.MIN_VALUE;
    private float lastScaleX = Float.NaN;
    private float lastScaleY = Float.NaN;
    private float lastRotation = Float.NaN;
    private boolean forceNextCapture;

    // Scale factors captured during the last setupInternalCanvasMatrix call, used to compensate
    // blur radius so the perceived blur strength stays constant regardless of view scale.
    private float capturedScaleX = 1f;
    private float capturedScaleY = 1f;

    private final ViewTreeObserver.OnPreDrawListener drawListener = new ViewTreeObserver.OnPreDrawListener() {
        @Override
        public boolean onPreDraw() {
            if (shouldUpdate()) {
                updateBlur();
            }
            return true;
        }
    };

    // Lets the algorithm free transient resources while detached, rebuilt lazily on the next blur.
    private final View.OnAttachStateChangeListener attachStateListener = new View.OnAttachStateChangeListener() {
        @Override
        public void onViewAttachedToWindow(@NonNull View view) {
        }

        @Override
        public void onViewDetachedFromWindow(@NonNull View view) {
            blurAlgorithm.onDetached();
            if (!blurAlgorithm.canModifyBitmap()) {
                // The displayed bitmap was just released by the algorithm; drop the dangling reference.
                displayBitmap = null;
            }
        }
    };

    private boolean blurEnabled = true;
    private boolean initialized;
    // Tracks the current auto-update preference so it survives re-initialization on size changes
    private boolean blurAutoUpdate = true;

    @Nullable
    private Drawable frameClearDrawable;

    /**
     * @param blurView    View which will draw it's blurred underlying content
     * @param rootView    Root View where blurView's underlying content starts drawing.
     *                    Can be Activity's root content layout (android.R.id.content)
     * @param algorithm   sets the blur algorithm
     * @param scaleFactor a scale factor to downscale the view snapshot before blurring.
     *                    Helps achieving stronger blur and potentially better performance at the expense of blur precision.
     * @param applyNoise  optional blue noise texture over the blurred content to make it look more natural. True by default.
     */
    public PreDrawBlurController(@NonNull View blurView,
                                 @NonNull BlurTarget rootView,
                                 @ColorInt int overlayColor,
                                 BlurAlgorithm algorithm,
                                 float scaleFactor,
                                 boolean applyNoise) {
        this.rootView = rootView;
        this.blurView = blurView;
        this.overlayColor = overlayColor;
        this.blurAlgorithm = algorithm;
        this.scaleFactor = scaleFactor;
        this.applyNoise = applyNoise;
        blurView.addOnAttachStateChangeListener(attachStateListener);

        int measuredWidth = blurView.getMeasuredWidth();
        int measuredHeight = blurView.getMeasuredHeight();

        init(measuredWidth, measuredHeight);
    }

    @SuppressWarnings("WeakerAccess")
    void init(int measuredWidth, int measuredHeight) {
        setBlurAutoUpdate(blurAutoUpdate);
        SizeScaler sizeScaler = new SizeScaler(scaleFactor);
        if (sizeScaler.isZeroSized(measuredWidth, measuredHeight)) {
            // Will be initialized later when the View reports a size change
            blurView.setWillNotDraw(true);
            return;
        }

        blurView.setWillNotDraw(false);
        SizeScaler.Size bitmapSize = sizeScaler.scale(measuredWidth, measuredHeight);
        internalBitmap = Bitmap.createBitmap(bitmapSize.width, bitmapSize.height, blurAlgorithm.getSupportedBitmapConfig());
        internalCanvas = new BlurViewCanvas(internalBitmap);
        initialized = true;
        // Usually it's not needed, because `onPreDraw` updates the blur anyway.
        // But it handles cases when the PreDraw listener is attached to a different Window, for example
        // when the BlurView is in a Dialog window, but the root is in the Activity.
        // Previously it was done in `draw`, but it was causing potential side effects and Jetpack Compose crashes
        updateBlur();
    }

    @SuppressWarnings("WeakerAccess")
    void updateBlur() {
        if (!blurEnabled || !initialized) {
            return;
        }

        if (frameClearDrawable == null) {
            internalBitmap.eraseColor(Color.TRANSPARENT);
        } else {
            frameClearDrawable.draw(internalCanvas);
        }

        internalCanvas.save();
        setupInternalCanvasMatrix();
        try {
            rootView.draw(internalCanvas);
        } catch (Exception e) {
            // Can potentially fail on rendering Hardware Bitmaps or something like that
            Log.e("BlurView", "Error during snapshot capturing", e);
        }
        internalCanvas.restore();

        blurAndSave();
    }

    /**
     * Set up matrix to draw starting from blurView's position
     */
    private void setupInternalCanvasMatrix() {
        rootView.getLocationOnScreen(rootLocation);
        blurView.getLocationOnScreen(blurViewLocation);

        BlurViewTransform t = BlurViewTransform.compute(blurView, blurViewLocation, rootLocation);

        float rootCenterX = t.layoutLeft + blurView.getWidth() / 2f;
        float rootCenterY = t.layoutTop + blurView.getHeight() / 2f;

        float scaleFactorH = (float) blurView.getHeight() / internalBitmap.getHeight();
        float scaleFactorW = (float) blurView.getWidth() / internalBitmap.getWidth();
        float bitmapCenterX = internalBitmap.getWidth() / 2f;
        float bitmapCenterY = internalBitmap.getHeight() / 2f;

        // Capture the root content that is visually beneath the BlurView:
        //  1. Shift root so the BlurView's invariant center is at the origin
        //  2. Counter-rotate by -R so the captured slice is axis-aligned in the bitmap
        //  3. Scale down to bitmap size, also accounting for the view's own scale so the full
        //     visual area (layout bounds × scaleX/Y) fits into the bitmap
        //  4. Shift to bitmap center
        // When draw() renders this bitmap onto the view's local canvas and the view system then
        // applies the view's own rotation R and scale, the content matches the background exactly.
        internalCanvas.translate(bitmapCenterX, bitmapCenterY);
        internalCanvas.rotate(-t.rotationDeg);
        internalCanvas.scale(1f / (scaleFactorW * t.scaleX), 1f / (scaleFactorH * t.scaleY));
        internalCanvas.translate(-rootCenterX, -rootCenterY);

        capturedScaleX = t.scaleX;
        capturedScaleY = t.scaleY;
    }

    @Override
    public boolean draw(Canvas canvas) {
        if (!blurEnabled || !initialized || displayBitmap == null) {
            return true;
        }
        // Not blurring itself or other BlurViews to not cause recursive draw calls
        // Related: https://github.com/Dimezis/BlurView/issues/110
        if (canvas instanceof BlurViewCanvas) {
            return false;
        }

        // https://github.com/Dimezis/BlurView/issues/128
        float scaleFactorH = (float) blurView.getHeight() / displayBitmap.getHeight();
        float scaleFactorW = (float) blurView.getWidth() / displayBitmap.getWidth();

        canvas.save();
        // Don't draw outside of the BlurView bounds if parent has clipChildren = false
        canvas.clipRect(0f, 0f, blurView.getWidth(), blurView.getHeight());
        canvas.save();
        canvas.scale(scaleFactorW, scaleFactorH);
        blurAlgorithm.render(canvas, displayBitmap);
        // restore scale so we don't upscale the noise texture
        canvas.restore();
        if (applyNoise) {
            Noise.apply(canvas, blurView.getContext(), blurView.getWidth(), blurView.getHeight());
        }
        if (overlayColor != TRANSPARENT) {
            canvas.drawColor(overlayColor);
        }
        // restore clip rect
        canvas.restore();
        return true;
    }

    private void blurAndSave() {
        // The bitmap packs scaleX*scaleY more content than at scale=1. Divide the blur radius by
        // the average scale so the perceived blur strength stays constant on screen.
        float scaleCompensation = (capturedScaleX + capturedScaleY) / 2f;
        displayBitmap = blurAlgorithm.blur(internalBitmap, blurRadius / scaleCompensation);
        if (!blurAlgorithm.canModifyBitmap()) {
            // New bitmap each frame, so re-record the display list to draw it. In-place algorithms
            // keep the same instance, which HWUI re-samples without an invalidate.
            blurView.invalidate();
        }
    }

    // In-place algorithms never invalidate, so onPreDraw fires only on real frames - capture every
    // time. Algorithms that produce a new bitmap invalidate on each blur, re-entering onPreDraw, so
    // gate them on a real content or position change to break that loop and skip redundant work.
    private boolean shouldUpdate() {
        if (blurAlgorithm.canModifyBitmap()) {
            return true;
        }
        int generation = rootView.contentGeneration;
        rootView.getLocationOnScreen(rootLocation);
        blurView.getLocationOnScreen(blurViewLocation);
        int left = blurViewLocation[0] - rootLocation[0];
        int top = blurViewLocation[1] - rootLocation[1];
        float scaleX = blurView.getScaleX();
        float scaleY = blurView.getScaleY();
        float rotation = blurView.getRotation();
        boolean changed = forceNextCapture
                || generation != lastGeneration
                || left != lastLeft
                || top != lastTop
                || scaleX != lastScaleX
                || scaleY != lastScaleY
                || rotation != lastRotation;
        forceNextCapture = false;
        lastGeneration = generation;
        lastLeft = left;
        lastTop = top;
        lastScaleX = scaleX;
        lastScaleY = scaleY;
        lastRotation = rotation;
        return changed;
    }

    @Override
    public void updateBlurViewSize() {
        int measuredWidth = blurView.getMeasuredWidth();
        int measuredHeight = blurView.getMeasuredHeight();

        init(measuredWidth, measuredHeight);
    }

    @Override
    public void destroy() {
        setBlurAutoUpdate(false);
        blurView.removeOnAttachStateChangeListener(attachStateListener);
        blurAlgorithm.destroy();
        initialized = false;
    }

    @Override
    public BlurViewFacade setBlurRadius(float radius) {
        this.blurRadius = radius;
        if (!blurAlgorithm.canModifyBitmap()) {
            // A radius change doesn't bump the content generation, so force the gated path to re-blur.
            forceNextCapture = true;
            blurView.invalidate();
        }
        return this;
    }

    @Override
    public BlurViewFacade setFrameClearDrawable(@Nullable Drawable frameClearDrawable) {
        this.frameClearDrawable = frameClearDrawable;
        return this;
    }

    @Override
    public BlurViewFacade setBlurEnabled(boolean enabled) {
        this.blurEnabled = enabled;
        // Re-enabling doesn't bump the content generation, so force the gated path to re-capture.
        forceNextCapture = enabled;
        setBlurAutoUpdate(enabled);
        blurView.invalidate();
        return this;
    }

    public BlurViewFacade setBlurAutoUpdate(final boolean enabled) {
        blurAutoUpdate = enabled;
        rootView.getViewTreeObserver().removeOnPreDrawListener(drawListener);
        blurView.getViewTreeObserver().removeOnPreDrawListener(drawListener);
        if (enabled) {
            rootView.getViewTreeObserver().addOnPreDrawListener(drawListener);
            // Track changes in the blurView window too, for example if it's in a bottom sheet dialog
            if (rootView.getWindowId() != blurView.getWindowId()) {
                blurView.getViewTreeObserver().addOnPreDrawListener(drawListener);
            }
        }
        return this;
    }

    @Override
    public BlurViewFacade setOverlayColor(int overlayColor) {
        if (this.overlayColor != overlayColor) {
            this.overlayColor = overlayColor;
            blurView.invalidate();
        }
        return this;
    }
}
