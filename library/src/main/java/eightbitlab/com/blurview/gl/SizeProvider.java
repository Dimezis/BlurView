package eightbitlab.com.blurview.gl;

class SizeProvider {

    private static final int ROUNDING_VALUE = 16;
    private static final float SCALE_FACTOR = 4f;

    private final int width;
    private final int height;

    private float widthScaleFactor = 1f;
    private float heightScaleFactor = 1f;

    private final int downscaledWidth;
    private final int downscaledHeight;

    SizeProvider(int width, int height) {
        this.width = width;
        this.height = height;
        int nonRoundedScaledWidth = downScaleSize(width);
        int nonRoundedScaledHeight = downScaleSize(height);

        downscaledWidth = roundSize(nonRoundedScaledWidth);
        downscaledHeight = roundSize(nonRoundedScaledHeight);

        heightScaleFactor = (float) nonRoundedScaledHeight / downscaledHeight * SCALE_FACTOR;
        widthScaleFactor = (float) nonRoundedScaledWidth / downscaledWidth * SCALE_FACTOR;
    }

    int downscaledWidth() {
        return downscaledWidth;
    }

    int downscaledHeight() {
        return downscaledHeight;
    }

    int width() {
        return width;
    }

    int height() {
        return height;
    }

    float widthScaleFactor() {
        return widthScaleFactor;
    }

    float heightScaleFactor() {
        return heightScaleFactor;
    }

    private int downScaleSize(float value) {
        return (int) Math.ceil(value / SCALE_FACTOR);
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
}
