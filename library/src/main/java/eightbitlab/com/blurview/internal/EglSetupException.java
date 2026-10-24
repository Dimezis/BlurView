package eightbitlab.com.blurview.internal;

/**
 * Signals that EGL bring-up in {@link EglCore} failed (display init, config selection, or context
 * creation). Thrown from the {@code EglCore} constructor so the OpenGL blur pipeline can treat the
 * pipeline as unavailable and fall back to the unblurred snapshot instead of using an invalid
 * display, config, or context.
 * <p>
 * Public so callers outside this package (e.g. {@code OpenGLBlurAlgorithm}) can catch it by name
 * and distinguish a known setup failure from an unexpected programming error.
 */
public class EglSetupException extends RuntimeException {

    public EglSetupException(String message) {
        super(message);
    }
}
