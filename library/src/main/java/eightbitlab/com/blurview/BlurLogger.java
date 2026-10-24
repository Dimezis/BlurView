package eightbitlab.com.blurview;

import androidx.annotation.Nullable;

/**
 * Hook for routing BlurView's internal error reports to the host app's logging or crash-reporting
 * pipeline (Crashlytics, Sentry, Timber, etc.) for observability in production.
 * <p>
 * BlurView never crashes the host view on a blur failure - it falls back to drawing the unblurred
 * snapshot and reports the failure through this logger instead. By default these reports go to
 * {@link android.util.Log}, where they are invisible in the field. Install a logger via
 * {@link BlurView#setLogger(BlurLogger)} to capture them in prod.
 */
public interface BlurLogger {

    /**
     * Reports a non-fatal BlurView error. The library has already recovered (typically by drawing
     * the unblurred content), so this is for observability, not for the caller to act on.
     *
     * @param tag       a short category tag, e.g. {@code "BlurView"}
     * @param message   a human-readable description of what failed
     * @param throwable the cause, if any
     */
    void onError(@Nullable String tag, @Nullable String message, @Nullable Throwable throwable);
}
