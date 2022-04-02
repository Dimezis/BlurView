package eightbitlab.com.blurview;

import androidx.annotation.NonNull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import eightbitlab.com.blurview.SizeScaler.Size;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.of;

class SizeScalerTest {
    private static final float scalingFactor = 8f;

    private final SizeScaler scaler = new SizeScaler(scalingFactor);

    @ParameterizedTest
    @MethodSource("scalingResults")
    void scales_and_returns_proper_size_and_scale_factor(int x, int y, Size expected) {
        Size result = scaler.scale(x, y);
        assertEquals(expected, result);
    }

    // In case if rounding mode for downscaleSize() will be changed
    @ParameterizedTest
    @CsvSource({"0,0,true", "1,1,false", "8,8,false", "0,100,true"})
    void isZeroSized(int x, int y, boolean isZeroSized) {
        assertEquals(isZeroSized, scaler.isZeroSized(x, y));
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> scalingResults() {
        return Stream.of(
                of(64, 64, size(64, 64, 1f)),
                // min size is 64
                of(7, 7, size(64, 64, 0.109375f)),
                of(128, 128, size(64, 64, 2f)),
                of(1024, 1024, size(128, 128, 8f)),
                // if Y is not divisible by 64 but X is, don't align Y
                of(1024, 256, size(128, 32, 8)),
                of(1000, 256, size(128, 33, 7.8125f)),
                // scale Y by the same factor as X
                of(900, 256, size(128, 37, 7.03125f)),
                of(900, 200, size(128, 29, 7.03125f)),
                of(907, 203, size(128, 29, 7.0859375f)),
                of(1080, 104, size(192, 19, 5.625f)),
                of(1080, 192, size(192, 35, 5.625f)),
                of(1080, 1149, size(192, 205, 5.625f))
        );
    }

    @NonNull
    private static Size size(int x, int y, float scaleFactor) {
        return new Size(x, y, scaleFactor);
    }
}