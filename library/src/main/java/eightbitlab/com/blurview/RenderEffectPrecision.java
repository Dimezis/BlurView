package eightbitlab.com.blurview;

public enum RenderEffectPrecision {
    /**
     * Blur will be applied to the exact View snapshot without any prior downscaling.
     * Produces the smoothest blur during movement,
     * but potentially at the cost of performance (not verified)
     */
    EXACT,
    /**
     * Blur will be applied to the downscaled View snapshot.
     * Produces a less smooth blur during movement, but potentially faster (not verified)
     */
    DOWNSCALED
}
