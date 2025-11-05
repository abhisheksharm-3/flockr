package `in`.xroden.flockr.utils

import android.util.Log

/**
 * Centralized logging utility for Flockr app
 * Provides structured logging with automatic tagging and performance monitoring
 */
object FlockrLogger {

    private const val APP_TAG = "Flockr"
    private var isDebugMode = true

    /**
     * Debug log
     */
    fun d(tag: String, message: String) {
        if (isDebugMode) {
            Log.d("$APP_TAG:$tag", message)
        }
    }

    /**
     * Info log
     */
    fun i(tag: String, message: String) {
        Log.i("$APP_TAG:$tag", message)
    }

    /**
     * Warning log
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w("$APP_TAG:$tag", message, throwable)
        } else {
            Log.w("$APP_TAG:$tag", message)
        }
    }

    /**
     * Error log with optional exception
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e("$APP_TAG:$tag", "$message - ${throwable.message}", throwable)
        } else {
            Log.e("$APP_TAG:$tag", message)
        }
    }

    /**
     * Log repository operation start
     */
    fun repoStart(tag: String, operation: String, params: Map<String, Any?> = emptyMap()) {
        val paramsString = params.entries.joinToString(", ") { "${it.key}=${it.value}" }
        d(tag, "▶ $operation: Starting${if (paramsString.isNotEmpty()) " with $paramsString" else ""}")
    }

    /**
     * Log repository operation success
     */
    fun repoSuccess(tag: String, operation: String, result: Any? = null) {
        val resultString = result?.let { " result=$it" } ?: ""
        d(tag, "✅ $operation: Success$resultString")
    }

    /**
     * Log repository operation error
     */
    fun repoError(tag: String, operation: String, error: Throwable) {
        e(tag, "❌ $operation: Failed", error)
    }

    /**
     * Log ViewModel state change
     */
    fun viewModelState(tag: String, state: String, details: String = "") {
        d(tag, "🔄 State: $state${if (details.isNotEmpty()) " - $details" else ""}")
    }

    /**
     * Log Realtime event
     */
    fun realtimeEvent(tag: String, event: String, details: String = "") {
        d(tag, "📡 Realtime: $event${if (details.isNotEmpty()) " - $details" else ""}")
    }

    /**
     * Log navigation event
     */
    fun navigation(tag: String, destination: String, from: String = "") {
        d(tag, "🧭 Navigation: ${if (from.isNotEmpty()) "$from → " else ""}$destination")
    }

    /**
     * Measure and log operation performance
     */
    inline fun <T> measurePerformance(tag: String, operation: String, block: () -> T): T {
        val startTime = System.currentTimeMillis()
        try {
            return block()
        } finally {
            val duration = System.currentTimeMillis() - startTime
            if (duration > 1000) {
                w(tag, "⏱ $operation took ${duration}ms (>1s)")
            } else {
                d(tag, "⏱ $operation took ${duration}ms")
            }
        }
    }
}

