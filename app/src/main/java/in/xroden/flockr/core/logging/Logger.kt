package `in`.xroden.flockr.core.logging

import `in`.xroden.flockr.BuildConfig

/** Centralized logging utility. Disabled in release builds. */
object Logger {

    private const val TAG_PREFIX = "Flockr"
    private val isDebugBuild = BuildConfig.DEBUG

    fun d(tag: String, message: String) {
        if (isDebugBuild) android.util.Log.d("$TAG_PREFIX:$tag", message)
    }

    fun i(tag: String, message: String) {
        if (isDebugBuild) android.util.Log.i("$TAG_PREFIX:$tag", message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            android.util.Log.w("$TAG_PREFIX:$tag", message, throwable)
        } else {
            android.util.Log.w("$TAG_PREFIX:$tag", message)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            android.util.Log.e("$TAG_PREFIX:$tag", message, throwable)
        } else {
            android.util.Log.e("$TAG_PREFIX:$tag", message)
        }
    }

    fun v(tag: String, message: String) {
        if (isDebugBuild) android.util.Log.v("$TAG_PREFIX:$tag", message)
    }
}

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
