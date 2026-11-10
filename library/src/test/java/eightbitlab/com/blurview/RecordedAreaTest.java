package eightbitlab.com.blurview;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RecordedAreaTest {
    private static final int[] RECORDED = {64, 64, 320, 256};

    private final RecordedArea recorded = new RecordedArea();

    @Test
    void nothing_recorded_covers_no_area() {
        assertFalse(recorded.covers(true, new int[]{64, 64, 128, 128}));
    }

    @Test
    void covers_an_area_inside_the_recording() {
        recorded.set(true, RECORDED);
        assertTrue(recorded.covers(true, RECORDED));
        assertTrue(recorded.covers(true, new int[]{128, 64, 256, 192}));
    }

    @Test
    void does_not_cover_an_area_reaching_past_any_edge() {
        recorded.set(true, RECORDED);
        assertFalse(recorded.covers(true, new int[]{0, 64, 320, 256}));
        assertFalse(recorded.covers(true, new int[]{64, 0, 320, 256}));
        assertFalse(recorded.covers(true, new int[]{64, 64, 384, 256}));
        assertFalse(recorded.covers(true, new int[]{64, 64, 320, 320}));
    }

    @Test
    void a_view_outside_the_target_needs_nothing_recorded() {
        assertTrue(recorded.covers(false, RECORDED));
        recorded.set(true, RECORDED);
        assertTrue(recorded.covers(false, RECORDED));
    }

    @Test
    void recording_nothing_zeroes_the_area() {
        recorded.set(true, RECORDED);
        recorded.set(false, RECORDED);
        assertFalse(recorded.has);
        assertArrayEquals(new int[4], recorded.area);
        assertFalse(recorded.covers(true, RECORDED));
    }

    @Test
    void cleared_recording_covers_no_area() {
        recorded.set(true, RECORDED);
        recorded.clear();
        assertFalse(recorded.covers(true, RECORDED));
        assertArrayEquals(new int[4], recorded.area);
    }
}
