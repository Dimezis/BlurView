package eightbitlab.com.blurview;

/**
 * The part of a {@link BlurTarget} that a BlurView's blur reads: the target under the BlurView,
 * mapped the way the blur node maps it (after the BlurView's scale and rotation), plus how far
 * the blur kernel reaches past it, cut off at the target's edges.
 * <p>
 * Recording only this part keeps the blur node about the size of the BlurView instead of the
 * size of the whole target. Nothing outside it reaches the visible pixels, so the output should
 * match recording the whole target; in testing it differed by a few levels at most.
 * <p>
 * The area's edges are snapped to a {@link #GRID} in the target's coordinates, so that it
 * changes only when the BlurView crosses a grid line, not with every pixel it moves.
 */
final class BlurArea {

    // HWUI's conversion from a RenderEffect blur radius to a Gaussian sigma
    // (Blur::convertRadiusToSigma in frameworks/base/libs/hwui/utils/Blur.cpp).
    private static final float SIGMA_PER_RADIUS = 0.57735f;
    // Skia cuts its Gaussian kernel off at three sigmas, so the blur reads no further.
    private static final float KERNEL_SIGMAS = 3f;
    // Skia blurs a wide sigma on a copy downscaled by halves until sigma is at most this.
    private static final float MAX_UNSCALED_SIGMA = 4f;
    static final int GRID = 64;

    private BlurArea() {
    }

    /**
     * How far, in pixels, a RenderEffect blur of this radius reads outside the pixels it writes.
     */
    static int reach(float blurRadius) {
        if (blurRadius <= 0f) {
            return 0;
        }
        float sigma = SIGMA_PER_RADIUS * blurRadius + 0.5f;
        int downscale = 1;
        for (float scaled = sigma; scaled > MAX_UNSCALED_SIGMA; scaled /= 2f) {
            downscale *= 2;
        }
        // Downscaling and scaling back up with bilinear filtering read up to two
        // downscaled pixels further
        int resampling = downscale > 1 ? 2 * downscale : 0;
        return (int) Math.ceil(sigma * KERNEL_SIGMAS) + resampling;
    }

    /**
     * Computes the area of the target the blur needs, in the target's coordinates.
     *
     * @param layoutLeft  the BlurView's left before its scale and rotation, in target coordinates
     * @param layoutTop   the BlurView's top before its scale and rotation, in target coordinates
     * @param width       the BlurView's width
     * @param height      the BlurView's height
     * @param pivotX      the BlurView's pivot, in its own coordinates
     * @param pivotY      the BlurView's pivot, in its own coordinates
     * @param scaleX      the BlurView's scale
     * @param scaleY      the BlurView's scale
     * @param rotationDeg the BlurView's rotation, in degrees
     * @param reach       see {@link #reach(float)}
     * @param out         receives left, top, right, bottom
     * @return false if the BlurView does not overlap the target, and there is nothing to record
     */
    static boolean compute(float layoutLeft, float layoutTop, int width, int height,
                           float pivotX, float pivotY, float scaleX, float scaleY, float rotationDeg,
                           int reach, int targetWidth, int targetHeight, int[] out) {
        double radians = Math.toRadians(rotationDeg);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        // The blur node is counter-transformed by RenderNode's matrix (translate, rotate, scale, in
        // that order), so inverted it shows the target point layout + pivot + Scale × Rotate ×
        // (c - pivot) at a corner c of the view: rotated first, then scaled.
        for (int corner = 0; corner < 4; corner++) {
            float dx = ((corner & 1) == 0 ? 0 : width) - pivotX;
            float dy = ((corner & 2) == 0 ? 0 : height) - pivotY;
            float rx = dx * cos - dy * sin;
            float ry = dx * sin + dy * cos;
            float x = layoutLeft + pivotX + rx * scaleX;
            float y = layoutTop + pivotY + ry * scaleY;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        if (targetWidth <= 0 || targetHeight <= 0) {
            return false;
        }
        int left = (int) Math.floor(minX) - reach;
        int top = (int) Math.floor(minY) - reach;
        int right = (int) Math.ceil(maxX) + reach;
        int bottom = (int) Math.ceil(maxY) + reach;
        if (right <= 0 || bottom <= 0 || left >= targetWidth || top >= targetHeight) {
            return false;
        }
        out[0] = Math.max(Math.floorDiv(left, GRID) * GRID, 0);
        out[1] = Math.max(Math.floorDiv(top, GRID) * GRID, 0);
        out[2] = Math.min(Math.floorDiv(right + GRID - 1, GRID) * GRID, targetWidth);
        out[3] = Math.min(Math.floorDiv(bottom + GRID - 1, GRID) * GRID, targetHeight);
        return true;
    }
}
