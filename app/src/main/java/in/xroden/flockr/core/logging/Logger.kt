package `in`.xroden.flockr.core.logging

import `in`.xroden.flockr.BuildConfig

/**
 * Centralized logging utility for the application.
 * Automatically disabled in release builds for security and performance.
 * Use this instead of android.util.Log for production-ready logging.
 */
object Logger {

    private const val TAG_PREFIX = "Flockr"
    private val isDebugBuild = BuildConfig.DEBUG

    /**
     * Log debug information.
     * Only logs in debug builds.
     */
    fun d(tag: String, message: String) {
        if (isDebugBuild) {
            android.util.Log.d("$TAG_PREFIX:$tag", message)
        }
    }

    /**
     * Log informational messages.
     * Only logs in debug builds.
     */
    fun i(tag: String, message: String) {
        if (isDebugBuild) {
            android.util.Log.i("$TAG_PREFIX:$tag", message)
        }
    }

    /**
     * Log warnings.
     * Logs in all builds as warnings may indicate issues.
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            android.util.Log.w("$TAG_PREFIX:$tag", message, throwable)
        } else {
            android.util.Log.w("$TAG_PREFIX:$tag", message)
        }
    }

    /**
     * Log errors.
     * Logs in all builds as errors need to be tracked.
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            android.util.Log.e("$TAG_PREFIX:$tag", message, throwable)
        } else {
            android.util.Log.e("$TAG_PREFIX:$tag", message)
        }
    }

    /**
     * Log verbose information.
     * Only logs in debug builds.
     */
    fun v(tag: String, message: String) {
        if (isDebugBuild) {
            android.util.Log.v("$TAG_PREFIX:$tag", message)
        }
    }
}

/**
 * Extension function for easy logging from any class.
 */
inline fun <reified T> T.logDebug(message: String) {
    Logger.d(T::class.java.simpleName, message)
}

inline fun <reified T> T.logInfo(message: String) {
    Logger.i(T::class.java.simpleName, message)
}

inline fun <reified T> T.logWarning(message: String, throwable: Throwable? = null) {
    Logger.w(T::class.java.simpleName, message, throwable)
}

inline fun <reified T> T.logError(message: String, throwable: Throwable? = null) {
    Logger.e(T::class.java.simpleName, message, throwable)
}
