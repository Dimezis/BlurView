package eightbitlab.com.blurview;

/**
 * Scales width and height by [scaleFactor],
 * and then rounds the size proportionally so the width is divisible by [ROUNDING_VALUE]
 */
public class SizeScaler {

    // Bitmap size should be divisible by ROUNDING_VALUE to meet stride requirement.
    // This will help avoiding an extra bitmap allocation when passing the bitmap to RenderScript for blur.
    // Usually it's 16, but on Samsung devices it's 64 for some reason.
    private static final int ROUNDING_VALUE = 64;
    private final float scaleFactor;

    public SizeScaler(float scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    Size scale(int width, int height) {
        int nonRoundedScaledWidth = downscaleSize(width);
        int scaledWidth = roundSize(nonRoundedScaledWidth);
        //Only width has to be aligned to ROUNDING_VALUE
        float roundingScaleFactor = (float) width / scaledWidth;
        //Ceiling because rounding or flooring might leave empty space on the View's bottom
        int scaledHeight = (int) Math.ceil(height / roundingScaleFactor);

        return new Size(scaledWidth, scaledHeight, roundingScaleFactor);
    }

    boolean isZeroSized(int measuredWidth, int measuredHeight) {
        return downscaleSize(measuredHeight) == 0 || downscaleSize(measuredWidth) == 0;
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

    private int downscaleSize(float value) {
        return (int) Math.ceil(value / scaleFactor);
    }

    static class Size {

        final int width;
        final int height;
        // TODO this is probably not needed anymore
        final float scaleFactor;

        Size(int width, int height, float scaleFactor) {
            this.width = width;
            this.height = height;
            this.scaleFactor = scaleFactor;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Size size = (Size) o;

            if (width != size.width) return false;
            if (height != size.height) return false;
            return Float.compare(size.scaleFactor, scaleFactor) == 0;
        }

        @Override
        public int hashCode() {
            int result = width;
            result = 31 * result + height;
            result = 31 * result + (scaleFactor != +0.0f ? Float.floatToIntBits(scaleFactor) : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Size{" +
                    "width=" + width +
                    ", height=" + height +
                    ", scaleFactor=" + scaleFactor +
                    '}';
        }
    }
}
