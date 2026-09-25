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
