package eightbitlab.com.blurview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * Blur Controller that handles all blur logic for attached View.
 * It honors View size changes, View animation and Visibility changes.
 *
 * The basic idea is to draw view hierarchy on internal bitmap, excluding the attached View.
 * After that, BlurController blurs this bitmap and draws it on system Canvas.
 * Default implementation uses {@link ViewTreeObserver.OnPreDrawListener} to detect when
 * blur should be redrawn.
 *
 * Blur is done in the main thread.
 */
class BlockingBlurController implements BlurController {
    private static final String TAG = BlockingBlurController.class.getSimpleName();
    //Bitmap size should be divisible by 16 to meet stride requirement
    private static final int ROUNDING_VALUE = 16;

    private final float scaleFactor = DEFAULT_SCALE_FACTOR;
    private float blurRadius = DEFAULT_BLUR_RADIUS;
    private float roundingWidthScaleFactor = 1f;
    private float roundingHeightScaleFactor = 1f;

    private BlurAlgorithm blurAlgorithm;
    private Canvas internalCanvas;

    /**
     * Draw view hierarchy here.
     * Blur it.
     * Draw it on BlurView's canvas.
     */
    private Bitmap internalBitmap;

    private final View blurView;
    private final View rootView;

    private final ViewTreeObserver.OnPreDrawListener drawListener = new ViewTreeObserver.OnPreDrawListener() {
        @Override
        public boolean onPreDraw() {
            if (!isMeDrawingNow && isBlurEnabled) {
                updateBlur();
            }
            return true;
        }
    };

    /**
     * Used to distinct parent draw() calls from Controller's draw() calls
     */
    private boolean isMeDrawingNow;
    private boolean isBlurEnabled = true;

    //must be set from message queue
    private final Runnable onDrawEndTask = new Runnable() {
        @Override
        public void run() {
            isMeDrawingNow = false;
        }
    };

    /**
     * By default, window's background is not drawn on canvas. We need to draw it manually
     */
    @Nullable
    private Drawable windowBackground;

    /**
     * @param blurView    View which will draw it's blurred underlying content
     * @param rootView    Root View where blurView's underlying content starts drawing.
     *                    Can be Activity's root content layout (android.R.id.content)
     *                    or some of your custom root layouts.
     */
    public BlockingBlurController(@NonNull View blurView, @NonNull View rootView) {
        this.rootView = rootView;
        this.blurView = blurView;
        this.blurAlgorithm = new RenderScriptBlur(blurView.getContext(), true);

        int measuredWidth = blurView.getMeasuredWidth();
        int measuredHeight = blurView.getMeasuredHeight();

        if (isZeroSized(measuredWidth, measuredHeight)) {
            deferBitmapCreation();
            return;
        }

        init(measuredWidth, measuredHeight);
    }

    private int downScaleSize(float value) {
        return (int) Math.ceil(value / scaleFactor);
    }

    /**
     * Rounds a value to the nearest divisible by {@link #ROUNDING_VALUE} to meet stride requirement
     */
    private int roundSize(int value) {
        if (value % ROUNDING_VALUE == 0) {
            return value;
        }
        return value - (value % ROUNDING_VALUE) + ROUNDING_VALUE;
    }

    private void init(int measuredWidth, int measuredHeight) {
        if (isZeroSized(measuredWidth, measuredHeight)) {
            blurView.setWillNotDraw(true);
            stopAutoBlurUpdate();
            return;
        }
        blurView.setWillNotDraw(false);
        allocateBitmap(measuredWidth, measuredHeight);
        internalCanvas = new Canvas(internalBitmap);
        startBlurAutoUpdate();
    }

    private boolean isZeroSized(int measuredWidth, int measuredHeight) {
        return downScaleSize(measuredHeight) == 0 || downScaleSize(measuredWidth) == 0;
    }

    @Override
    public void stopAutoBlurUpdate() {
        rootView.getViewTreeObserver().removeOnPreDrawListener(drawListener);
    }

    @Override
    public void startBlurAutoUpdate() {
        stopAutoBlurUpdate(); //just in case if listener was already attached
        rootView.getViewTreeObserver().addOnPreDrawListener(drawListener);
    }

    private void updateBlur() {
        isMeDrawingNow = true;
        blurView.invalidate();
    }

    /**
     * Deferring initialization until view is laid out
     */
    private void deferBitmapCreation() {
        blurView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    blurView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    blurView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                int measuredWidth = blurView.getMeasuredWidth();
                int measuredHeight = blurView.getMeasuredHeight();

                init(measuredWidth, measuredHeight);
            }
        });
    }

    private void allocateBitmap(int measuredWidth, int measuredHeight) {
        //downscale overlay (blurred) bitmap
        int nonRoundedScaledWidth = downScaleSize(measuredWidth);
        int nonRoundedScaledHeight = downScaleSize(measuredHeight);

        int scaledWidth = roundSize(nonRoundedScaledWidth);
        int scaledHeight = roundSize(nonRoundedScaledHeight);

        roundingHeightScaleFactor = (float) nonRoundedScaledHeight / scaledHeight;
        roundingWidthScaleFactor = (float) nonRoundedScaledWidth  / scaledWidth;

        internalBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, blurAlgorithm.getSupportedBitmapConfig());
    }

    //draw starting from blurView's position
    private void setupInternalCanvasMatrix() {
        float scaleFactorX = scaleFactor * roundingWidthScaleFactor;
        float scaleFactorY = scaleFactor * roundingHeightScaleFactor;

        float scaledLeftPosition = -blurView.getLeft() / scaleFactorX;
        float scaledTopPosition = -blurView.getTop() / scaleFactorY;

        float scaledTranslationX = blurView.getTranslationX() / scaleFactorX;
        float scaledTranslationY = blurView.getTranslationY() / scaleFactorY;

        internalCanvas.translate(scaledLeftPosition - scaledTranslationX, scaledTopPosition - scaledTranslationY);
        internalCanvas.scale(1f / scaleFactorX, 1f / scaleFactorY);
    }

    /**
     * Draws whole view hierarchy on internal canvas
     */
    private void drawUnderlyingViews() {
        //draw activity window background
        if (windowBackground != null) {
            windowBackground.draw(internalCanvas);
        }
        rootView.draw(internalCanvas);
    }

    @Override
    public void drawBlurredContent(Canvas canvas) {
        isMeDrawingNow = true;

        if (isBlurEnabled) {
            internalCanvas.save();
            setupInternalCanvasMatrix();
            drawUnderlyingViews();
            internalCanvas.restore();

            blurAndSave();
            draw(canvas);
        }
    }

    private void draw(Canvas canvas) {
        canvas.save();
        canvas.scale(scaleFactor * roundingWidthScaleFactor, scaleFactor * roundingHeightScaleFactor);
        canvas.drawBitmap(internalBitmap, 0, 0, null);
        canvas.restore();
    }

    @Override
    public void onDrawEnd(Canvas canvas) {
        blurView.post(onDrawEndTask);
    }

    private void blurAndSave() {
        internalBitmap = blurAlgorithm.blur(internalBitmap, blurRadius);
    }

    @Override
    public void updateBlurViewSize() {
        int measuredWidth = blurView.getMeasuredWidth();
        int measuredHeight = blurView.getMeasuredHeight();

        init(measuredWidth, measuredHeight);
    }

    @Override
    public void destroy() {
        stopAutoBlurUpdate();
        blurAlgorithm.destroy();
        if (internalBitmap != null) {
            internalBitmap.recycle();
        }
    }

    @Override
    public void setBlurRadius(float radius) {
        this.blurRadius = radius;
    }

    @Override
    public void setBlurAlgorithm(BlurAlgorithm algorithm) {
        this.blurAlgorithm = algorithm;
    }

    @Override
    public void setWindowBackground(@Nullable Drawable windowBackground) {
        this.windowBackground = windowBackground;
    }

    @Override
    public void setBlurEnabled(boolean enabled) {
        this.isBlurEnabled = enabled;
        blurView.invalidate();
    }
}
