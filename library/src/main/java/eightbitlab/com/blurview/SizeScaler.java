package eightbitlab.com.blurview;

import java.util.Objects;

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
    private final boolean noStrideAlignment;

    public SizeScaler(float scaleFactor) {
        this(scaleFactor, false);
    }

    public SizeScaler(float scaleFactor, boolean noStrideAlignment) {
        this.scaleFactor = scaleFactor;
        this.noStrideAlignment = noStrideAlignment;
    }

    Size scale(int width, int height) {
        int nonRoundedScaledWidth = downscaleSize(width);
        int scaledWidth = roundSize(nonRoundedScaledWidth);
        //Only width has to be aligned to ROUNDING_VALUE
        float roundingScaleFactor = (float) width / scaledWidth;
        //Ceiling because rounding or flooring might leave empty space on the View's bottom
        int scaledHeight = (int) Math.ceil(height / roundingScaleFactor);

        return new Size(scaledWidth, scaledHeight);
    }

    Size scale(Size size) {
        return scale(size.width, size.height);
    }

    boolean isZeroSized(int measuredWidth, int measuredHeight) {
        return downscaleSize(measuredHeight) == 0 || downscaleSize(measuredWidth) == 0;
    }

    /**
     * Rounds a value to the nearest divisible by {@link #ROUNDING_VALUE} to meet stride requirement
     */
    private int roundSize(int value) {
        if (noStrideAlignment) {
            return value;
        }
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

        Size(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Size size = (Size) o;
            return width == size.width && height == size.height;
        }

        @Override
        public int hashCode() {
            return Objects.hash(width, height);
        }

        @Override
        public String toString() {
            return "Size{" +
                    "width=" + width +
                    ", height=" + height +
                    '}';
        }
    }
}
