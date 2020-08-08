package eightbitlab.com.blurview;

import static eightbitlab.com.blurview.BlurController.DEFAULT_SCALE_FACTOR;

public class SizeScaler {

    // Bitmap size should be divisible by ROUNDING_VALUE to meet stride requirement.
    // This will help avoiding an extra bitmap allocation when passing the bitmap to RenderScript for blur.
    // Usually it's 16, but on Samsung devices it's 64 for some reason.
    private static final int ROUNDING_VALUE = 64;

    private final float scaleFactor = DEFAULT_SCALE_FACTOR;
    private float roundingScaleFactor = 1f;

    Size roundSize(int width, int height) {
        int nonRoundedScaledWidth = downscaleSize(width);
        int nonRoundedScaledHeight = downscaleSize(height);

        //Only width has to be aligned to ROUNDING_VALUE
        int scaledWidth = roundSize(nonRoundedScaledWidth);

        //TODO get rid of this ugly state?
        roundingScaleFactor = (float) nonRoundedScaledWidth / scaledWidth;
        int scaledHeight = (int) (nonRoundedScaledHeight / roundingScaleFactor);

        return new Size(scaledWidth, scaledHeight);
    }

    boolean isZeroSized(int measuredWidth, int measuredHeight) {
        return downscaleSize(measuredHeight) == 0 || downscaleSize(measuredWidth) == 0;
    }

    float scaleFactor() {
        return scaleFactor * roundingScaleFactor;
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

    int downscaleSize(float value) {
        return (int) Math.ceil(value / scaleFactor);
    }

    static class Size {

        final int width;
        final int height;

        Size(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}
