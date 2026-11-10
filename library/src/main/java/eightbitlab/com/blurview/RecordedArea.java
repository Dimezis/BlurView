package eightbitlab.com.blurview;

import java.util.Arrays;

/**
 * The part of the target a blur node holds (see {@link BlurArea}).
 */
final class RecordedArea {

    /** Left, top, right, bottom in target coordinates; all zero while {@link #has} is false. */
    final int[] area = new int[4];
    /** False when nothing is recorded, or the BlurView was outside the target when it was. */
    boolean has;

    /** The node has just been recorded with {@code recorded}, or with nothing if {@code has} is false. */
    void set(boolean has, int[] recorded) {
        this.has = has;
        if (has) {
            System.arraycopy(recorded, 0, area, 0, 4);
        } else {
            Arrays.fill(area, 0);
        }
    }

    /**
     * Whether the recording already holds what the BlurView needs.
     *
     * @param needs  false when the BlurView is outside the target and shows none of it
     * @param needed the area it needs otherwise
     */
    boolean covers(boolean needs, int[] needed) {
        if (!needs) {
            // Whatever is recorded lies inside the target, so none of it shows over the BlurView
            return true;
        }
        return has
                && area[0] <= needed[0] && area[1] <= needed[1]
                && area[2] >= needed[2] && area[3] >= needed[3];
    }

    void clear() {
        set(false, area);
    }
}
