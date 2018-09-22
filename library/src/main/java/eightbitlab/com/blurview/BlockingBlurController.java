package eightbitlab.com.blurview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

/**
 * Blur Controller that handles all blur logic for attached View.
 * It honors View size changes, View animation and Visibility changes.
 * <p>
 * The basic idea is to draw view hierarchy on internal bitmap, excluding the attached View.
 * After that, BlurController blurs this bitmap and draws it on system Canvas.
 * Default implementation uses {@link ViewTreeObserver.OnPreDrawListener} to detect when
 * blur should be redrawn.
 * <p>
 * Blur is done in the main thread.
 */
class BlockingBlurController implements BlurController {

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

    @SuppressWarnings("WeakerAccess")
    final View blurView;
    private final ViewGroup rootView;
    private final Rect relativeViewBounds = new Rect();
    private final int[] locationInWindow = new int[2];

    private final ViewTreeObserver.OnPreDrawListener drawListener = new ViewTreeObserver.OnPreDrawListener() {
        @Override
        public boolean onPreDraw() {
            if (!isMeDrawingNow) {
                updateBlur();
            }
            return true;
        }
    };

    //Used to distinct parent draw() calls from Controller's draw() calls
    @SuppressWarnings("WeakerAccess")
    boolean isMeDrawingNow;
    private boolean isBlurEnabled = true;

    //must be set from message queue
    private final Runnable onDrawEndTask = new Runnable() {
        @Override
        public void run() {
            isMeDrawingNow = false;
        }
    };

    //By default, window's background is not drawn on canvas. We need to draw it manually
    @Nullable
    private Drawable frameClearDrawable;
    private boolean shouldTryToOffsetCoords = true;
    private boolean hasFixedTransformationMatrix;
    private final Paint paint = new Paint();

    /**
     * @param blurView View which will draw it's blurred underlying content
     * @param rootView Root View where blurView's underlying content starts drawing.
     *                 Can be Activity's root content layout (android.R.id.content)
     *                 or some of your custom root layouts.
     */
    BlockingBlurController(@NonNull View blurView, @NonNull ViewGroup rootView) {
        this.rootView = rootView;
        this.blurView = blurView;
        this.blurAlgorithm = new NoOpBlurAlgorithm();
        paint.setFilterBitmap(true);

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

    @SuppressWarnings("WeakerAccess")
    void init(int measuredWidth, int measuredHeight) {
        if (isZeroSized(measuredWidth, measuredHeight)) {
            isBlurEnabled = false;
            blurView.setWillNotDraw(true);
            setBlurAutoUpdate(false);
            return;
        }

        isBlurEnabled = true;
        blurView.setWillNotDraw(false);
        allocateBitmap(measuredWidth, measuredHeight);
        internalCanvas = new Canvas(internalBitmap);
        setBlurAutoUpdate(true);
        if (hasFixedTransformationMatrix) {
            setupInternalCanvasMatrix();
        }
    }

    private boolean isZeroSized(int measuredWidth, int measuredHeight) {
        return downScaleSize(measuredHeight) == 0 || downScaleSize(measuredWidth) == 0;
    }

    @SuppressWarnings("WeakerAccess")
    void updateBlur() {
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
                    legacyRemoveOnGlobalLayoutListener();
                }

                int measuredWidth = blurView.getMeasuredWidth();
                int measuredHeight = blurView.getMeasuredHeight();

                init(measuredWidth, measuredHeight);
            }

            @SuppressWarnings("deprecation")
            void legacyRemoveOnGlobalLayoutListener() {
                blurView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        });
    }

    private void allocateBitmap(int measuredWidth, int measuredHeight) {
        int nonRoundedScaledWidth = downScaleSize(measuredWidth);
        int nonRoundedScaledHeight = downScaleSize(measuredHeight);

        int scaledWidth = roundSize(nonRoundedScaledWidth);
        int scaledHeight = roundSize(nonRoundedScaledHeight);

        roundingHeightScaleFactor = (float) nonRoundedScaledHeight / scaledHeight;
        roundingWidthScaleFactor = (float) nonRoundedScaledWidth / scaledWidth;

        internalBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, blurAlgorithm.getSupportedBitmapConfig());
    }

    /**
     * setup matrix to draw starting from blurView's position
     */
    private void setupInternalCanvasMatrix() {
        blurView.getDrawingRect(relativeViewBounds);

        if (shouldTryToOffsetCoords) {
            try {
                rootView.offsetDescendantRectToMyCoords(blurView, relativeViewBounds);
            } catch (IllegalArgumentException e) {
                // BlurView is not a child of the rootView (i.e. it's in Dialog)
                // Fallback to regular coordinates system
                shouldTryToOffsetCoords = false;
            }
        } else {
            blurView.getLocationInWindow(locationInWindow);
            relativeViewBounds.offset(locationInWindow[0], locationInWindow[1]);
        }

        float scaleFactorX = scaleFactor * roundingWidthScaleFactor;
        float scaleFactorY = scaleFactor * roundingHeightScaleFactor;

        float scaledLeftPosition = -relativeViewBounds.left / scaleFactorX;
        float scaledTopPosition = -relativeViewBounds.top / scaleFactorY;

        float scaledTranslationX = blurView.getTranslationX() / scaleFactorX;
        float scaledTranslationY = blurView.getTranslationY() / scaleFactorY;

        internalCanvas.translate(scaledLeftPosition - scaledTranslationX, scaledTopPosition - scaledTranslationY);
        internalCanvas.scale(1f / scaleFactorX, 1f / scaleFactorY);
    }

    @Override
    public void drawBlurredContent(Canvas canvas) {
        isMeDrawingNow = true;

        if (isBlurEnabled) {
            if (frameClearDrawable == null) {
                internalBitmap.eraseColor(Color.TRANSPARENT);
            } else {
                frameClearDrawable.draw(internalCanvas);
            }
            if (hasFixedTransformationMatrix) {
                rootView.draw(internalCanvas);
            } else {
                internalCanvas.save();
                setupInternalCanvasMatrix();
                rootView.draw(internalCanvas);
                internalCanvas.restore();
            }

            blurAndSave();
            draw(canvas);
        }
    }

    private void draw(Canvas canvas) {
        canvas.save();
        canvas.scale(scaleFactor * roundingWidthScaleFactor, scaleFactor * roundingHeightScaleFactor);
        canvas.drawBitmap(internalBitmap, 0, 0, paint);
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
        setBlurAutoUpdate(false);
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
    public void setFrameClearDrawable(@Nullable Drawable frameClearDrawable) {
        this.frameClearDrawable = frameClearDrawable;
    }

    @Override
    public void setBlurEnabled(boolean enabled) {
        this.isBlurEnabled = enabled;
        setBlurAutoUpdate(enabled);
        blurView.invalidate();
    }

    @Override
    public void setBlurAutoUpdate(boolean enabled) {
        blurView.getViewTreeObserver().removeOnPreDrawListener(drawListener);
        if (enabled) {
            blurView.getViewTreeObserver().addOnPreDrawListener(drawListener);
        }
    }

    @Override
    public void setHasFixedTransformationMatrix(boolean hasFixedTransformationMatrix) {
        this.hasFixedTransformationMatrix = hasFixedTransformationMatrix;
    }
}
