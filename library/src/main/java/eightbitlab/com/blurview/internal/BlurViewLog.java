package eightbitlab.com.blurview.internal;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import eightbitlab.com.blurview.BlurLogger;

/**
 * Process-wide sink for BlurView's internal error reports. Every library log site routes through
 * {@link #error} so a host app can observe failures in production via a {@link BlurLogger}.
 * <p>
 * Defaults to forwarding to {@link Log}.
 */
public final class BlurViewLog {

    private static BlurLogger logger = Log::e;

    private BlurViewLog() {
    }

    /**
     * Installs the active logger, routing reports to it instead of logcat. To silence reporting,
     * pass a no-op logger.
     */
    public static void setLogger(@NonNull BlurLogger newLogger) {
        logger = newLogger;
    }

    /**
     * Reports a non-fatal error to the active logger
     */
    public static void error(@Nullable String tag, @Nullable String message, @Nullable Throwable throwable) {
        try {
            logger.onError(tag, message, throwable);
        } catch (Exception loggerFailed) {
            Log.e("BlurView", "BlurLogger threw while reporting an error", loggerFailed);
        }
    }

    public static void error(@Nullable String tag, @Nullable String message) {
        error(tag, message, null);
    }
}
