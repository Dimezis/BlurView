package eightbitlab.com.blurview.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;

/**
 * Feature: blurview-bugfixes, Property 2: Kernel sample count never exceeds the cap
 *
 * <p>Validates: Requirements 8.1, 8.3
 *
 * <p>{@link GaussianKernel} is plain Java (java.lang.Math only), so it runs on the JVM directly.
 * The tests call {@link GaussianKernel#ensureRadius(float)} then {@link GaussianKernel#sampleCount()}
 * and assert the stored tap count never exceeds {@link GaussianKernel#MAX_SAMPLES} (value 24), and
 * saturates at the cap once a radius's +/-3 sigma reach would require more taps.
 */
class GaussianKernelTest {

    @ParameterizedTest
    @ValueSource(floats = {0f, 0.5f, 1f, 2f, 5f, 10f, 24f, 25f, 100f, 1000f, 100000f})
    void sample_count_never_exceeds_cap_for_representative_radii(float radius) {
        GaussianKernel kernel = new GaussianKernel();
        kernel.ensureRadius(radius);

        assertTrue(kernel.sampleCount() <= GaussianKernel.MAX_SAMPLES,
                "sampleCount " + kernel.sampleCount() + " exceeded MAX_SAMPLES " + GaussianKernel.MAX_SAMPLES
                        + " for radius " + radius);
        assertTrue(kernel.sampleCount() >= 0, "sampleCount must be non-negative for radius " + radius);
    }

    /**
     * Property 2: Kernel sample count never exceeds the cap.
     *
     * <p>For 100+ generated radii, {@code sampleCount() <= MAX_SAMPLES} always holds. Uses a seeded
     * {@link Random} loop to avoid adding a new dependency.
     */
    @Test
    void property_sample_count_never_exceeds_cap() {
        Random random = new Random(0xB10B); // seeded for deterministic iterations
        int iterations = 1000;

        for (int i = 0; i < iterations; i++) {
            // Cover small, mid, and very large radii across iterations.
            float radius = random.nextFloat() * 5000f;
            GaussianKernel kernel = new GaussianKernel();
            kernel.ensureRadius(radius);

            int count = kernel.sampleCount();
            assertTrue(count <= GaussianKernel.MAX_SAMPLES,
                    "sampleCount " + count + " exceeded MAX_SAMPLES " + GaussianKernel.MAX_SAMPLES
                            + " for radius " + radius);
            assertTrue(count >= 0, "sampleCount must be non-negative for radius " + radius);
        }
    }

    /**
     * The cap is actually reached: a very large radius whose +/-3 sigma reach needs more than
     * MAX_SAMPLES taps saturates sampleCount at exactly MAX_SAMPLES.
     */
    @Test
    void large_radius_saturates_sample_count_at_cap() {
        GaussianKernel kernel = new GaussianKernel();
        kernel.ensureRadius(10000f);

        assertEquals(GaussianKernel.MAX_SAMPLES, kernel.sampleCount(),
                "a very large radius should saturate sampleCount at MAX_SAMPLES");
    }
}
