package eightbitlab.com.blurview.internal;

/**
 * Linear-sample Gaussian taps for one separable blur axis, computed on the CPU. The kernel is
 * symmetric, so only the right half is stored and each stored tap serves the +/- pair; the t=0 tap is
 * implicit with unit weight. Recomputed only when the radius changes (cheap, even when animated).
 * <p>
 * Each tap samples <em>between</em> two adjacent texels at a weight-biased offset so a single
 * GL_LINEAR fetch returns both texels' contribution - halving fetches versus one-per-texel. Taps run
 * out to +/-3 sigma; sigma = radius * 0.57735 + 0.5 (Skia's SkBlurMask::ConvertRadiusToSigma,
 * 0.57735 ~= 1/sqrt(3)), the radius->sigma mapping RenderScript and RenderEffect use, so a given
 * radius matches their strength across API levels.
 */
final class GaussianKernel {

    /**
     * Max stored taps (right half). Must match the {@code uOffsets}/{@code uWeights} array size in
     * {@link BlurShaders#BLUR_FRAGMENT}. Radii whose +/-3 sigma reach exceeds this clamp the kernel's
     * tail; the shader renormalizes by the in-bounds weight, so the result stays smooth.
     * <p>
     * This cap also bounds the <em>effective</em> blur strength: once a radius is large enough that its
     * +/-3 sigma reach would need more than {@code MAX_SAMPLES} taps, the extra taps are never stored,
     * so {@link #sampleCount()} saturates at {@code MAX_SAMPLES} and further increasing the radius no
     * longer widens the kernel. Visually the blur strength plateaus past that point rather than growing
     * without bound. This is a deliberate ceiling for the OpenGL path and is not changed by callers; to
     * blur harder beyond it, downscale the snapshot more (a larger scale factor) rather than raising the
     * radius alone. Note the RenderEffect and RenderScript paths have no such shared cap, so their
     * appearance can diverge from this path at large radii.
     */
    static final int MAX_SAMPLES = 24;

    private final float[] offsets = new float[MAX_SAMPLES];
    private final float[] weights = new float[MAX_SAMPLES];
    private int sampleCount;
    private float radius = Float.NaN;

    /**
     * Recomputes the right-half taps when the radius changes; a no-op otherwise (compared exactly,
     * with the {@code NaN} seed forcing the first call to run).
     * <p>
     * {@code sigma = radius * 0.57735 + 0.5} maps a pixel radius to a Gaussian standard deviation:
     * {@code 0.57735 ~= 1/sqrt(3)} is the Skia / RenderScript / RenderEffect convention, and the
     * {@code +0.5} keeps a sub-pixel radius from collapsing sigma to 0. The Gaussian
     * {@code exp(-t^2 / 2*sigma^2)} is sampled at integer texel offsets {@code t = 1, 2, ...} out to
     * {@code 3*sigma}, past which its weight is negligible; {@code twoSigmaSquared} is that
     * denominator, precomputed once.
     * <p>
     * Adjacent texel pairs {@code (t, t+1)} are then fused into one bilinear tap: a single GL_LINEAR
     * fetch at the weight-biased offset {@code (t*w0 + (t+1)*w1) / (w0 + w1)} returns exactly
     * {@code w0*texel(t) + w1*texel(t+1)} once scaled by the stored pair weight {@code w0 + w1}, so
     * two texels cost one fetch. An odd trailing texel with no partner is stored as a plain tap at its
     * integer offset. Weights are left unnormalized - the shader divides by their in-bounds sum.
     */
    void ensureRadius(float blurRadius) {
        if (blurRadius == radius) {
            return;
        }
        radius = blurRadius;
        float sigma = blurRadius * 0.57735f + 0.5f;
        float twoSigmaSquared = 2f * sigma * sigma;
        int radiusTexels = Math.max(1, (int) Math.ceil(3f * sigma));
        int count = 0;
        int texel = 1;
        while (texel <= radiusTexels && count < MAX_SAMPLES) {
            float firstWeight = (float) Math.exp(-(double) (texel * texel) / twoSigmaSquared);
            if (texel + 1 <= radiusTexels) {
                float secondWeight = (float) Math.exp(-(double) ((texel + 1) * (texel + 1)) / twoSigmaSquared);
                float pairWeight = firstWeight + secondWeight;
                offsets[count] = (texel * firstWeight + (texel + 1) * secondWeight) / pairWeight;
                weights[count] = pairWeight;
                texel += 2;
            } else {
                // Odd trailing texel: no partner to fuse, sample it directly at its integer offset.
                offsets[count] = texel;
                weights[count] = firstWeight;
                texel += 1;
            }
            count++;
        }
        sampleCount = count;
    }

    int sampleCount() {
        return sampleCount;
    }

    float[] offsets() {
        return offsets;
    }

    float[] weights() {
        return weights;
    }
}
