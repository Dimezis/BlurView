package com.eightbitlab.blurview;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewTreeObserver;

public class DefaultBlurController implements BlurController {
    private static final String TAG = DefaultBlurController.class.getSimpleName();
    public static final float DEFAULT_SCALE_FACTOR = 10f;
    public static final int DEFAULT_BLUR_RADIUS = 6;

    private float scaleFactor = DEFAULT_SCALE_FACTOR;
    private int blurRadius = DEFAULT_BLUR_RADIUS;

    private BlurAlgorithm blurAlgorithm = new StackBlur(true);
    private Paint bitmapPaint;

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

    private boolean isMeDrawingNow;

    private Handler handler;
    //must be set from message queue
    private Runnable onDrawEndTask = new Runnable() {
        @Override
        public void run() {
            isMeDrawingNow = false;
        }
    };

    /**
     * By default, window's background is not drawn on canvas. We need to draw in manually
     */
    private Drawable windowBackground;

    private DefaultBlurController(View blurView, View rootView) {
        bitmapPaint = new Paint();
        bitmapPaint.setFlags(Paint.FILTER_BITMAP_FLAG);

        handler = new Handler(Looper.getMainLooper());

        this.rootView = rootView;
        this.blurView = blurView;

        int measuredWidth = blurView.getMeasuredWidth();
        int measuredHeight = blurView.getMeasuredHeight();

        if (measuredWidth == 0 || measuredHeight == 0) {
            deferBitmapsCreation();
            return;
        }

        init(measuredWidth, measuredHeight);
    }

    private void init(int measuredWidth, int measuredHeight) {
        allocateBitmaps(measuredWidth, measuredHeight);
        overlayCanvas = new Canvas(blurredOverlay);
        setupInternalCanvas(blurView);
        observeDrawCalls();
    }

    private void observeDrawCalls() {
        drawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (!isMeDrawingNow) {
                    reBlur();
                }
                return true;
            }
        };
        blurView.getViewTreeObserver().addOnPreDrawListener(drawListener);
    }

    private void reBlur() {
        isMeDrawingNow = true;
        drawUnderlyingViews();
        blurView.invalidate();
    }

    /**
     * Deferring initialization until view is laid out
     */
    private void deferBitmapsCreation() {
        blurView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                blurView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                int measuredWidth = blurView.getMeasuredWidth();
                int measuredHeight = blurView.getMeasuredHeight();

                init(measuredWidth, measuredHeight);
            }
        });
    }

    private void allocateBitmaps(int measuredWidth, int measuredHeight) {
        //downscale overlay (blurred) bitmap
        int scaledWidth = (int) (measuredWidth / scaleFactor);
        int scaledHeight = (int) (measuredHeight / scaleFactor);

        blurredOverlay = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);
        internalBitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888);
    }

    private void setupInternalCanvas(View blurView) {
        internalCanvas = new Canvas(internalBitmap);
        //draw starting from blurView's position
        float scaledLeftPosition = -blurView.getLeft() / scaleFactor;
        float scaledTopPosition = -blurView.getTop() / scaleFactor;

        internalCanvas.translate(scaledLeftPosition, scaledTopPosition);
        internalCanvas.scale(1 / scaleFactor, 1 / scaleFactor);
    }

    @Override
    public boolean isInternalCanvas(Canvas canvas) {
        return internalCanvas == canvas;
    }

    protected void drawUnderlyingViews() {
        //draw activity window background
        if (windowBackground != null) {
            windowBackground.draw(internalCanvas);
        }
        //draw whole view hierarchy on canvas
        rootView.draw(internalCanvas);
    }

    @Override
    public void drawBlurredContent(Canvas canvas) {
        canvas.save();
        canvas.scale(scaleFactor, scaleFactor);
        canvas.drawBitmap(getBlurredBitmap(), 0, 0, bitmapPaint);
        canvas.restore();
    }

    @Override
    public void onDrawEnd(Canvas canvas) {
        handler.post(onDrawEndTask);
    }

    @Override
    public Bitmap getBlurredBitmap() {
        overlayCanvas.drawBitmap(internalBitmap, 0, 0, null);
        blurredOverlay = blurAlgorithm.blur(blurredOverlay, blurRadius);
        return blurredOverlay;
    }

    @Override
    public void destroy() {
        rootView.getViewTreeObserver().removeOnPreDrawListener(drawListener);
        rootView = null;
        blurView = null;
        blurredOverlay.recycle();
        internalBitmap.recycle();
    }

    static class Builder {
        private DefaultBlurController instance;

        /**
         * @param blurView View which will draw it's blurred underlying content
         * @param rootView Root View where blurView's underlying content starts drawing.
         * Can be Activity's root content layout (android.R.id.content)
         * @return Builder
         */
        public Builder withViews(View blurView, View rootView) {
            instance = new DefaultBlurController(blurView, rootView);
            return this;
        }

        /**
         * @param paint sets the Paint to draw blurred bitmap.
         * Default implementation uses flag {@link Paint#FILTER_BITMAP_FLAG}
         * @return Builder
         */
        public Builder bitmapPaint(Paint paint) {
            instance.bitmapPaint = paint;
            return this;
        }

        /**
         * @param radius sets the blur radius
         * Default implementation uses field {@link DefaultBlurController#DEFAULT_BLUR_RADIUS}
         * @return Builder
         */
        public Builder blurRadius(int radius) {
            instance.blurRadius = radius;
            return this;
        }

        /**
         * @param scaleFactor sets scale factor to downscale blurred bitmap for faster calculations
         * Default implementation uses field {@link DefaultBlurController#DEFAULT_SCALE_FACTOR}
         * @return Builder
         */
        public Builder scaleFactor(float scaleFactor) {
            instance.scaleFactor = scaleFactor;
            return this;
        }

        /**
         * @param algorithm sets the blur algorithm
         * Default implementation uses {@link StackBlur}
         * @return Builder
         */
        public Builder algorithm(BlurAlgorithm algorithm) {
            instance.blurAlgorithm = algorithm;
            return this;
        }

        /**
         * @param windowBackground sets the background to draw before view hierarchy.
         * Can be used to draw Activity's window background if your root layout doesn't provide any background
         * @return Builder
         */
        public Builder windowBackground(Drawable windowBackground) {
            instance.windowBackground = windowBackground;
            return this;
        }

        public DefaultBlurController build() {
            return instance;
        }
    }
}
