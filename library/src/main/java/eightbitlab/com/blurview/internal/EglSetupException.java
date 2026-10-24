package eightbitlab.com.blurview.internal;

/**
 * Signals that EGL bring-up in {@link EglCore} failed (display init, config selection, or context
 * creation). Thrown from the {@code EglCore} constructor so the OpenGL blur pipeline can treat the
 * pipeline as unavailable and fall back to the unblurred snapshot instead of using an invalid
 * display, config, or context.
 */
class EglSetupException extends RuntimeException {

    EglSetupException(String message) {
        super(message);
    }
}
