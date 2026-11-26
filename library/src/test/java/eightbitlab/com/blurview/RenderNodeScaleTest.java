package eightbitlab.com.blurview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.of;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import eightbitlab.com.blurview.RenderNodeBlurController.NodeTransform;

class RenderNodeScaleTest {

    @ParameterizedTest
    @CsvSource({
            "0, 4.0, 0",
            "-10, 4.0, 0",
            "1, 4.0, 1",
            "1080, 4.0, 270",
            "1081, 4.0, 271",
            "2160, 4.0, 540",
            "1080, 1.0, 1080",
            "1080, 0.5, 1080",
            "1000, 8.0, 125"
    })
    void downscaledDimension_roundsUpAndClampsScale(int targetSize, float scaleFactor, int expected) {
        assertEquals(expected, RenderNodeBlurController.downscaledDimension(targetSize, scaleFactor));
    }

    @ParameterizedTest
    @CsvSource({
            "16.0, 4.0, 16.0",
            "25.0, 4.0, 25.0",
            "4.0, 4.0, 4.0",
            "16.0, 1.0, 16.0",
            "16.0, 0.5, 8.0",
            "20.0, 8.0, 20.0"
    })
    void nodeBlurRadius_preservesScreenSpaceRadius(float blurRadius, float scaleFactor, float expected) {
        assertEquals(expected, RenderNodeBlurController.nodeBlurRadius(blurRadius, scaleFactor), 1e-4f);
    }

    @Test
    void nodeTransform_matchesUnscaledLayoutWhenScaleFactorIsOne() {
        NodeTransform t = RenderNodeBlurController.computeNodeTransform(
                400f, 160f, 120f, 800f, 1f, 1f, 0f, 1f
        );
        assertEquals(320f, t.pivotX, 1e-4f);
        assertEquals(880f, t.pivotY, 1e-4f);
        assertEquals(-120f, t.translationX, 1e-4f);
        assertEquals(-800f, t.translationY, 1e-4f);
        assertEquals(1f, t.scaleX, 1e-4f);
        assertEquals(1f, t.scaleY, 1e-4f);
        assertEquals(0f, t.rotationZ, 1e-4f);
    }

    @ParameterizedTest
    @MethodSource("transformCases")
    void downscaledNodeTransform_mapsTargetPointsIdenticallyToUnscaled(
            float viewW,
            float viewH,
            float layoutLeft,
            float layoutTop,
            float viewScaleX,
            float viewScaleY,
            float rotationDeg,
            float scaleFactor
    ) {
        NodeTransform unscaled = RenderNodeBlurController.computeNodeTransform(
                viewW, viewH, layoutLeft, layoutTop, viewScaleX, viewScaleY, rotationDeg, 1f
        );
        NodeTransform downscaled = RenderNodeBlurController.computeNodeTransform(
                viewW, viewH, layoutLeft, layoutTop, viewScaleX, viewScaleY, rotationDeg, scaleFactor
        );

        float invScale = 1f / Math.max(1f, scaleFactor);
        float[][] samplePoints = {
                {layoutLeft, layoutTop},
                {layoutLeft + viewW / 2f, layoutTop + viewH / 2f},
                {layoutLeft + viewW, layoutTop + viewH},
                {0f, 0f},
                {1080f, 2160f}
        };

        for (float[] point : samplePoints) {
            float[] expected = applyRenderNodeMatrix(point[0], point[1], unscaled);
            float[] actual = applyRenderNodeMatrix(point[0] * invScale, point[1] * invScale, downscaled);
            assertEquals(expected[0], actual[0], 1e-3f);
            assertEquals(expected[1], actual[1], 1e-3f);
        }
    }

    /**
     * Applies Android RenderNode's property transform:
     * T(translation) * T(pivot) * R(rotationZ) * S(scale) * T(-pivot).
     */
    private static float[] applyRenderNodeMatrix(float x, float y, NodeTransform t) {
        float sx = (x - t.pivotX) * t.scaleX;
        float sy = (y - t.pivotY) * t.scaleY;
        double rad = Math.toRadians(t.rotationZ);
        float cos = (float) Math.cos(rad);
        float sin = (float) Math.sin(rad);
        float rx = sx * cos - sy * sin;
        float ry = sx * sin + sy * cos;
        return new float[]{rx + t.pivotX + t.translationX, ry + t.pivotY + t.translationY};
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> transformCases() {
        return Stream.of(
                of(400f, 160f, 120f, 800f, 1f, 1f, 0f, 4f),
                of(400f, 160f, 120f, 800f, 1f, 1f, 15f, 4f),
                of(400f, 160f, 120f, 800f, 0.6f, 0.6f, 0f, 4f),
                of(400f, 160f, 120f, 800f, 1.3f, 1.3f, 45f, 4f),
                of(400f, 160f, -80f, 800f, 0.8f, 0.8f, 30f, 4f),
                of(400f, 160f, 120f, 800f, 1.6f, 0.7f, 30f, 4f),
                of(400f, 160f, 120f, 800f, 2f, 1f, 90f, 8f),
                of(400f, 160f, 120f, 800f, 1.6f, 0.7f, 30f, 0.5f)
        );
    }
}
