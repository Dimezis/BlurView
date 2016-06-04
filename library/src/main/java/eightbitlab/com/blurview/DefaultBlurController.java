package eightbitlab.com.blurview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
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
 */
class DefaultBlurController implements BlurController {
    private static final String TAG = DefaultBlurController.class.getSimpleName();

    private float scaleFactor = DEFAULT_SCALE_FACTOR;
    private float blurRadius = DEFAULT_BLUR_RADIUS;

    private BlurAlgorithm blurAlgorithm;
    @Nullable
    private Paint blurredBitmapPaint;

    private Canvas internalCanvas;
    private Canvas overlayCanvas;

    /**
     * View hierarchy is drawn here
     */
    private Bitmap internalBitmap;
    /**
     * Blurred content is drawn here
     */
    private Bitmap blurredOverlay;

    private View blurView;
    private View rootView;
    private ViewTreeObserver.OnPreDrawListener drawListener;

    /**
     * Used to distinct parent draw() calls from Controller's draw() calls
     */
    private boolean isMeDrawingNow;

    @NonNull
    private Handler handler;

    //must be set from message queue
    @NonNull
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
    public DefaultBlurController(@NonNull View blurView, @NonNull View rootView) {
        blurredBitmapPaint = new Paint();
        blurredBitmapPaint.setFlags(Paint.FILTER_BITMAP_FLAG);

        handler = new Handler(Looper.getMainLooper());

        this.rootView = rootView;
        this.blurView = blurView;
        this.blurAlgorithm = new StackBlur(true);

        int measuredWidth = blurView.getMeasuredWidth();
        int measuredHeight = blurView.getMeasuredHeight();

        if (isZeroSized(measuredWidth, measuredHeight)) {
            deferBitmapsCreation();
            return;
        }

        init(measuredWidth, measuredHeight);
    }

    private int downScaleSize(float value) {
        return (int) Math.ceil(value / scaleFactor);
    }

    private void init(int measuredWidth, int measuredHeight) {
        if (isZeroSized(measuredWidth, measuredHeight)) {
            blurView.setWillNotDraw(true);
            return;
        }
        blurView.setWillNotDraw(false);
        allocateBitmaps(measuredWidth, measuredHeight);
        overlayCanvas = new Canvas(blurredOverlay);
        internalCanvas = new Canvas(internalBitmap);
        observeDrawCalls();
        updateBlur();
    }

    private boolean isZeroSized(int measuredWidth, int measuredHeight) {
        return downScaleSize(measuredHeight) == 0 || downScaleSize(measuredWidth) == 0;
    }

    private void observeDrawCalls() {
        if (drawListener != null) {
            return;
        }
        drawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (!isMeDrawingNow) {
                    updateBlur();
                }
                return true;
            }
        };
        rootView.getViewTreeObserver().addOnPreDrawListener(drawListener);
    }

    @Override
    public void stopAutoBlurUpdate() {
        rootView.getViewTreeObserver().removeOnPreDrawListener(drawListener);
    }

    @Override
    public void updateBlur() {
        isMeDrawingNow = true;
        internalCanvas.save();
        setupInternalCanvasMatrix();
        drawUnderlyingViews();
        blurView.invalidate();
        internalCanvas.restore();
    }

    /**
     * Deferring initialization until view is laid out
     */
    private void deferBitmapsCreation() {
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

    private void allocateBitmaps(int measuredWidth, int measuredHeight) {
        //downscale overlay (blurred) bitmap
        int scaledWidth = downScaleSize(measuredWidth);
        int scaledHeight = downScaleSize(measuredHeight);

        blurredOverlay = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);
        internalBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);
    }

    private void setupInternalCanvasMatrix() {
        //draw starting from blurView's position
        float scaledLeftPosition = -blurView.getLeft() / scaleFactor;
        float scaledTopPosition = -blurView.getTop() / scaleFactor;

        float scaledTranslationX = blurView.getTranslationX() / scaleFactor;
        float scaledTranslationY = blurView.getTranslationY() / scaleFactor;

        internalCanvas.translate(scaledLeftPosition - scaledTranslationX, scaledTopPosition - scaledTranslationY);
        float scaleX = blurView.getScaleX() / scaleFactor;
        float scaleY = blurView.getScaleY() / scaleFactor;
        internalCanvas.scale(scaleX, scaleY);
    }

    @Override
    public boolean isInternalCanvas(Canvas canvas) {
        return internalCanvas == canvas;
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
        prepareOverlayForBlur();
        blurAndSave();
        draw(canvas);
    }

    private void draw(Canvas canvas) {
        canvas.save();
        canvas.scale(scaleFactor, scaleFactor);
        canvas.drawBitmap(blurredOverlay, 0, 0, blurredBitmapPaint);
        canvas.restore();
    }

    @Override
    public void onDrawEnd(Canvas canvas) {
        handler.post(onDrawEndTask);
    }

    private void blurAndSave() {
        blurredOverlay = blurAlgorithm.blur(blurredOverlay, blurRadius);
        if (!blurAlgorithm.canModifyBitmap()) {
            overlayCanvas.setBitmap(blurredOverlay);
        }
    }

    private void prepareOverlayForBlur() {
        overlayCanvas.drawBitmap(internalBitmap, 0, 0, null);
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
        drawListener = null;
        rootView = null;
        blurView = null;
        blurAlgorithm.destroy();
        blurredOverlay.recycle();
        internalBitmap.recycle();
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
}
