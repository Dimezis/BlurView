package eightbitlab.com.blurview.internal;

/**
 * GLSL ES 3.00 sources for the OpenGL blur pipeline.
 */
final class BlurShaders {

    private BlurShaders() {
    }

    /**
     * Passthrough vertex shader: emits the fullscreen quad position and its UV.
     */
    static final String VERTEX = "#version 300 es\n" +
            "layout(location = 0) in vec2 aPosition;\n" +
            "layout(location = 1) in vec2 aUv;\n" +
            "out vec2 vUv;\n" +
            "void main() {\n" +
            "    vUv = aUv;\n" +
            "    gl_Position = vec4(aPosition, 0.0, 1.0);\n" +
            "}\n";

    /**
     * One axis of a separable Gaussian via linear (bilinear) sampling. Offsets/weights are precomputed
     * by {@link GaussianKernel}; each tap samples between two texels so GL_LINEAR folds the +/- pair
     * into one fetch. The kernel is symmetric, so the {@code uOffsets}/{@code uWeights} arrays hold the
     * right half only and the t=0 tap has unit weight (the unnormalized Gaussian at 0). Out-of-axis
     * taps are dropped and the result renormalized by the in-bounds weight. {@code uFlipY} flips the
     * vertical axis on the final pass (GL bottom-left vs Bitmap top-left origin).
     */
    static final String BLUR_FRAGMENT = "#version 300 es\n" +
            "precision highp float;\n" +
            "uniform sampler2D uTexture;\n" +
            "uniform vec2 uTexelSize;\n" +
            "uniform vec2 uDirection;\n" +
            "uniform int uSampleCount;\n" +
            "uniform float uOffsets[24];\n" +
            "uniform float uWeights[24];\n" +
            "uniform int uFlipY;\n" +
            "in vec2 vUv;\n" +
            "out vec4 frag;\n" +
            "void main() {\n" +
            "    vec2 baseUv = (uFlipY == 1) ? vec2(vUv.x, 1.0 - vUv.y) : vUv;\n" +
            "    vec4 sum = texture(uTexture, baseUv);\n" +
            "    float weightSum = 1.0;\n" +
            "    for (int i = 0; i < uSampleCount; i++) {\n" +
            "        vec2 delta = uDirection * uTexelSize * uOffsets[i];\n" +
            "        float weight = uWeights[i];\n" +
            "        vec2 uvP = baseUv + delta;\n" +
            "        vec2 uvN = baseUv - delta;\n" +
            "        float inbP = step(0.0, dot(uvP, uDirection)) * step(dot(uvP, uDirection), 1.0);\n" +
            "        float inbN = step(0.0, dot(uvN, uDirection)) * step(dot(uvN, uDirection), 1.0);\n" +
            "        sum += texture(uTexture, uvP) * (weight * inbP);\n" +
            "        sum += texture(uTexture, uvN) * (weight * inbN);\n" +
            "        weightSum += weight * (inbP + inbN);\n" +
            "    }\n" +
            "    frag = sum / weightSum;\n" +
            "}\n";

    /**
     * External OES variant of {@link #BLUR_FRAGMENT} for the first (horizontal) pass when the
     * snapshot comes from a {@link android.graphics.SurfaceTexture} rather than a plain 2D texture.
     * Uses {@code samplerExternalOES} and applies {@code uTransform} (from
     * {@link android.graphics.SurfaceTexture#getTransformMatrix}) to the UV before sampling.
     * No {@code uFlipY}: this is always an intermediate pass; the final vertical pass handles
     * the Y-flip as normal.
     */
    static final String BLUR_FRAGMENT_EXTERNAL = "#version 300 es\n" +
            "#extension GL_OES_EGL_image_external_essl3 : require\n" +
            "precision highp float;\n" +
            "uniform samplerExternalOES uTexture;\n" +
            "uniform mat4 uTransform;\n" +
            "uniform vec2 uTexelSize;\n" +
            "uniform vec2 uDirection;\n" +
            "uniform int uSampleCount;\n" +
            "uniform float uOffsets[24];\n" +
            "uniform float uWeights[24];\n" +
            "in vec2 vUv;\n" +
            "out vec4 frag;\n" +
            "void main() {\n" +
            "    vec2 baseUv = (uTransform * vec4(vUv, 0.0, 1.0)).xy;\n" +
            "    vec4 sum = texture(uTexture, baseUv);\n" +
            "    float weightSum = 1.0;\n" +
            "    for (int i = 0; i < uSampleCount; i++) {\n" +
            "        vec2 delta = uDirection * uTexelSize * uOffsets[i];\n" +
            "        float weight = uWeights[i];\n" +
            "        vec2 uvP = baseUv + delta;\n" +
            "        vec2 uvN = baseUv - delta;\n" +
            "        float inbP = step(0.0, dot(uvP, uDirection)) * step(dot(uvP, uDirection), 1.0);\n" +
            "        float inbN = step(0.0, dot(uvN, uDirection)) * step(dot(uvN, uDirection), 1.0);\n" +
            "        sum += texture(uTexture, uvP) * (weight * inbP);\n" +
            "        sum += texture(uTexture, uvN) * (weight * inbN);\n" +
            "        weightSum += weight * (inbP + inbN);\n" +
            "    }\n" +
            "    frag = sum / weightSum;\n" +
            "}\n";
}
