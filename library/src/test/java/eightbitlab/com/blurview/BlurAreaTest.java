package eightbitlab.com.blurview;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BlurAreaTest {
    private static final int TARGET_WIDTH = 1000;
    private static final int TARGET_HEIGHT = 2000;

    private final int[] out = new int[4];

    @ParameterizedTest
    // sigma = 0.57735 * radius + 0.5, reach = ceil(3 * sigma), plus two downscaled pixels
    // once Skia downscales (sigma above 4)
    // Radius 6 is just under sigma 4, radius 6.1 just over it
    // Radius 500 is past sigma 256, where Skia downscales by 128
    @CsvSource({"0,0", "-1,0", "1,4", "5,11", "6,12", "6.1,17", "10,23", "25,53", "100,207", "500,1124"})
    void reach_is_three_sigmas_plus_resampling(float radius, int expected) {
        assertEquals(expected, BlurArea.reach(radius));
    }

    @Test
    void reach_never_shrinks_as_the_radius_grows() {
        int previous = 0;
        for (float radius = 0f; radius <= 600f; radius += 0.25f) {
            int reach = BlurArea.reach(radius);
            assertTrue(reach >= previous, "reach shrank at radius " + radius);
            previous = reach;
        }
    }

    @Test
    void untransformed_view_is_its_bounds_plus_reach_snapped_to_the_grid() {
        // 90, 190, 410, 260 before snapping
        assertTrue(compute(100, 200, 300, 50, 0f, 1f, 1f, 10));
        assertArrayEquals(new int[]{64, 128, 448, 320}, out);
    }

    @Test
    void area_on_grid_lines_is_not_widened() {
        assertTrue(compute(74, 138, 300, 172, 0f, 1f, 1f, 10));
        assertArrayEquals(new int[]{64, 128, 384, 320}, out);
    }

    @Test
    void area_stops_at_the_target_edges() {
        assertTrue(compute(0, 0, TARGET_WIDTH, 100, 0f, 1f, 1f, 30));
        assertArrayEquals(new int[]{0, 0, TARGET_WIDTH, 192}, out);
    }

    @Test
    void view_rotated_by_90_degrees_about_its_center_covers_its_swapped_bounds() {
        // 200 x 100 view centered at (300, 450): rotated, it spans 250, 350, 350, 550
        assertTrue(compute(200, 400, 200, 100, 90f, 1f, 1f, 0));
        assertArrayEquals(new int[]{192, 320, 384, 576}, out);
    }

    @Test
    void view_rotated_by_45_degrees_covers_its_bounding_box() {
        // 100 x 100 square centered at (150, 150): its diagonal is 100 * sqrt(2) ~ 141.42,
        // so it spans 79.3 to 220.7, rounded outwards to 79 and 221
        assertTrue(compute(100, 100, 100, 100, 45f, 1f, 1f, 0));
        assertArrayEquals(new int[]{64, 64, 256, 256}, out);
    }

    @Test
    void rotation_about_a_corner_pivot_swings_the_view_around_that_corner() {
        // 100 x 100 at (200, 200) turned 90 degrees about its top-left corner spans 100, 200, 200, 300.
        // About its center it would stay at 200, 200, 300, 300, which snaps to 192, 192, 320, 320.
        assertTrue(BlurArea.compute(200, 200, 100, 100, 0f, 0f, 1f, 1f, 90f, 0,
                TARGET_WIDTH, TARGET_HEIGHT, out));
        assertArrayEquals(new int[]{64, 192, 256, 320}, out);
    }

    @Test
    void stretched_and_rotated_view_is_rotated_before_it_is_scaled() {
        // 100 x 100 at the origin, scaled 2 x 1 and turned 90 degrees about its center. The node
        // shows the target at center + Scale x Rotate x (corner - center): x spans -50 to 150 and
        // y 0 to 100. Scaling before rotating would give x 0 to 100 and y -50 to 150 instead.
        assertTrue(compute(0, 0, 100, 100, 90f, 2f, 1f, 0));
        assertArrayEquals(new int[]{0, 0, 192, 128}, out);
    }

    @Test
    void empty_target_has_no_area() {
        assertFalse(BlurArea.compute(0, 0, 100, 100, 50f, 50f, 1f, 1f, 0f, 10, 0, 0, out));
        assertFalse(BlurArea.compute(0, 0, 100, 100, 50f, 50f, 1f, 1f, 0f, 10, TARGET_WIDTH, 0, out));
    }

    @Test
    void view_partly_left_of_the_target_is_clamped_at_zero() {
        // -110, 40, 110, 160 before snapping
        assertTrue(compute(-100, 50, 200, 100, 0f, 1f, 1f, 10));
        assertArrayEquals(new int[]{0, 0, 128, 192}, out);
    }

    @Test
    void scaled_up_view_covers_its_scaled_bounds() {
        // Scaled 2x about its center (200, 150), it spans 100, 50, 300, 250
        assertTrue(compute(150, 100, 100, 100, 0f, 2f, 2f, 0));
        assertArrayEquals(new int[]{64, 0, 320, 256}, out);
    }

    @Test
    void view_outside_the_target_has_no_area() {
        assertFalse(compute(TARGET_WIDTH + 50, 0, 100, 100, 0f, 1f, 1f, 10));
        assertFalse(compute(0, -200, 100, 100, 0f, 1f, 1f, 10));
    }

    @Test
    void reach_alone_can_overlap_the_target() {
        // The view is just past the right edge, but the blur still reads the target's last pixels
        assertTrue(compute(TARGET_WIDTH + 5, 0, 100, 100, 0f, 1f, 1f, 10));
        assertArrayEquals(new int[]{960, 0, TARGET_WIDTH, 128}, out);
    }

    @Test
    void snapping_alone_does_not_make_an_outside_view_overlap() {
        // Its reach stops 5px past the right edge; the grid line below that is inside the target
        assertFalse(compute(TARGET_WIDTH + 15, 0, 100, 100, 0f, 1f, 1f, 10));
    }

    @Test
    void area_moves_only_when_the_view_crosses_a_grid_line() {
        // 70, 140, 170, 240 before snapping, and 80, 150, 180, 250 after the move
        assertTrue(compute(80, 150, 80, 80, 0f, 1f, 1f, 10));
        assertArrayEquals(new int[]{64, 128, 192, 256}, out);
        assertTrue(compute(90, 160, 80, 80, 0f, 1f, 1f, 10));
        assertArrayEquals(new int[]{64, 128, 192, 256}, out);
    }

    // Pivot at the view's center, as by default
    private boolean compute(float left, float top, int width, int height,
                            float rotation, float scaleX, float scaleY, int reach) {
        return BlurArea.compute(left, top, width, height, width / 2f, height / 2f,
                scaleX, scaleY, rotation, reach, TARGET_WIDTH, TARGET_HEIGHT, out);
    }
}
