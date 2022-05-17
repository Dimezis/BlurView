package eightbitlab.com.blurview;

import android.graphics.*;
import android.view.*;

import androidx.annotation.*;

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
 * Blur is done on the main thread.
 */
final class BlockingBlurController implements BlurController {

    @ColorInt
    static final int TRANSPARENT = 0;

    private float blurRadius = DEFAULT_BLUR_RADIUS;

    private BlurAlgorithm blurAlgorithm;
    private BlurViewCanvas internalCanvas;
    private Bitmap internalBitmap;

    @SuppressWarnings("WeakerAccess")
    final BlurView blurView;
    private int overlayColor;
    private final ViewGroup rootView;
    private final int[] rootLocation = new int[2];
    private final int[] blurViewLocation = new int[2];

    private final ViewTreeObserver.OnPreDrawListener drawListener = new ViewTreeObserver.OnPreDrawListener() {
        @Override
        public boolean onPreDraw() {
            // Not invalidating a View here, just updating the Bitmap.
            // This relies on the HW accelerated bitmap drawing behavior in Android
            // If the bitmap was drawn on HW accelerated canvas, it holds a reference to it and on next
            // drawing pass the updated content of the bitmap will be rendered on the screen

            updateBlur();
            return true;
        }
    };

    private boolean blurEnabled = true;
    private boolean initialized;

    @Nullable
    private View frameClearDrawable;
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);

    /**
     * @param blurView View which will draw it's blurred underlying content
     * @param rootView Root View where blurView's underlying content starts drawing.
     *                 Can be Activity's root content layout (android.R.id.content)
     *                 or some of your custom root layouts.
     */
    BlockingBlurController(@NonNull BlurView blurView, @NonNull ViewGroup rootView, @ColorInt int overlayColor) {
        this.rootView = rootView;
        this.blurView = blurView;
        this.overlayColor = overlayColor;
        this.blurAlgorithm = new NoOpBlurAlgorithm();

        int measuredWidth = blurView.getMeasuredWidth();
        int measuredHeight = blurView.getMeasuredHeight();

        init(measuredWidth, measuredHeight);
    }

    @SuppressWarnings("WeakerAccess")
    void init(int measuredWidth, int measuredHeight) {
        SizeScaler sizeScaler = new SizeScaler(blurAlgorithm.scaleFactor());
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
    }

    @SuppressWarnings("WeakerAccess")
    void updateBlur() {
        if (!blurEnabled || !initialized) {
            return;
        }

        if (frameClearDrawable == null) {
            internalBitmap.eraseColor(Color.TRANSPARENT);
        } else {
            frameClearDrawable.getBackground().draw(internalCanvas);
        }

        internalCanvas.save();
        setupInternalCanvasMatrix();
        rootView.draw(internalCanvas);
        internalCanvas.restore();

        blurAndSave();
    }

    /**
     * Set up matrix to draw starting from blurView's position
     */
    private void setupInternalCanvasMatrix() {
        rootView.getLocationOnScreen(rootLocation);
        blurView.getLocationOnScreen(blurViewLocation);

        int left = blurViewLocation[0] - rootLocation[0];
        int top = blurViewLocation[1] - rootLocation[1];

        // https://github.com/Dimezis/BlurView/issues/128
        float scaleFactorH = (float) blurView.getHeight() / internalBitmap.getHeight();
        float scaleFactorW = (float) blurView.getWidth() / internalBitmap.getWidth();

        float scaledLeftPosition = -left / scaleFactorW;
        float scaledTopPosition = -top / scaleFactorH;

        internalCanvas.translate(scaledLeftPosition, scaledTopPosition);
        internalCanvas.scale(1 / scaleFactorW, 1 / scaleFactorH);
    }

    @Override
    public boolean draw(Canvas canvas) {
        if (!blurEnabled || !initialized) {
            return true;
        }
        // Not blurring itself or other BlurViews to not cause recursive draw calls
        // Related: https://github.com/Dimezis/BlurView/issues/110
        //          https://github.com/Dimezis/BlurView/issues/110
        if (canvas instanceof BlurViewCanvas) {
            return false;
        }

        updateBlur();

        // TODO This is hacky. Maybe BlurAlgorithm should have a method to control this instead?
        if (!(blurAlgorithm instanceof RenderEffectBlur)) {
            // https://github.com/Dimezis/BlurView/issues/128
            float scaleFactorH = (float) blurView.getHeight() / internalBitmap.getHeight();
            float scaleFactorW = (float) blurView.getWidth() / internalBitmap.getWidth();

            canvas.save();
            canvas.scale(scaleFactorW, scaleFactorH);
            canvas.drawBitmap(internalBitmap, 0, 0, paint);
            canvas.restore();
        }
        if (overlayColor != TRANSPARENT) {
            canvas.drawColor(overlayColor);
        }
        return true;
    }

    private void blurAndSave() {
        internalBitmap = blurAlgorithm.blur(internalBitmap, blurRadius);
        if (!blurAlgorithm.canModifyBitmap()) {
            internalCanvas.setBitmap(internalBitmap);
        }
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
        blurAlgorithm.destroy();
        initialized = false;
    }

    // TODO this should rather be a part of the blur algo
    @Override
    public BlurViewFacade setBlurRadius(float radius) {
        this.blurRadius = radius;
        return this;
    }

    // TODO should be immutable
    @Override
    public BlurViewFacade setBlurAlgorithm(BlurAlgorithm algorithm) {
        this.blurAlgorithm = algorithm;
        return this;
    }

    @Override
    public BlurViewFacade setFrameClearDrawable(@Nullable View frameClearDrawable) {
        this.frameClearDrawable = frameClearDrawable;
        return this;
    }

    @Override
    public BlurViewFacade setBlurEnabled(boolean enabled) {
        this.blurEnabled = enabled;
        setBlurAutoUpdate(enabled);
        blurView.invalidate();
        return this;
    }

    public BlurViewFacade setBlurAutoUpdate(final boolean enabled) {
        rootView.getViewTreeObserver().removeOnPreDrawListener(drawListener);
        if (enabled) {
            rootView.getViewTreeObserver().addOnPreDrawListener(drawListener);
        }
        return this;
    }

    @Override
    @Deprecated
    public BlurViewFacade setHasFixedTransformationMatrix(boolean hasFixedTransformationMatrix) {
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
