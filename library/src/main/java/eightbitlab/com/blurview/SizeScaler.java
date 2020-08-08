package eightbitlab.com.blurview;

import static eightbitlab.com.blurview.BlurController.DEFAULT_SCALE_FACTOR;

public class SizeScaler {

    // Bitmap size should be divisible by ROUNDING_VALUE to meet stride requirement.
    // This will help avoiding an extra bitmap allocation when passing the bitmap to RenderScript for blur.
    // Usually it's 16, but on Samsung devices it's 64 for some reason.
    private static final int ROUNDING_VALUE = 64;

    private final float scaleFactor = DEFAULT_SCALE_FACTOR;
    private float roundingWidthScaleFactor = 1f;
    private float roundingHeightScaleFactor = 1f;

    Size roundSize(int width, int height) {
        int nonRoundedScaledWidth = downscaleSize(width);
        int nonRoundedScaledHeight = downscaleSize(height);

        int scaledWidth = roundSize(nonRoundedScaledWidth);
        int scaledHeight = roundSize(nonRoundedScaledHeight);

        //TODO get rid of this ugly state?
        roundingWidthScaleFactor = (float) nonRoundedScaledWidth / scaledWidth;
        roundingHeightScaleFactor = (float) nonRoundedScaledHeight / scaledHeight;

        return new Size(scaledWidth, scaledHeight);
    }

    boolean isZeroSized(int measuredWidth, int measuredHeight) {
        return downscaleSize(measuredHeight) == 0 || downscaleSize(measuredWidth) == 0;
    }

    float widthScaleFactor() {
        return scaleFactor * roundingWidthScaleFactor;
    }

    float heightScaleFactor() {
        return scaleFactor * roundingHeightScaleFactor;
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
