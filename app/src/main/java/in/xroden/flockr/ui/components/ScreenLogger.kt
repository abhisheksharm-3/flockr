package `in`.xroden.flockr.ui.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect

/**
 * Lifecycle logging wrapper for composable screens.
 * Automatically logs when screen is composed, recomposed, and disposed.
 */
@Composable
fun ScreenLogger(
    screenName: String,
    params: Map<String, Any?> = emptyMap(),
    content: @Composable () -> Unit
) {
    val paramsStr = if (params.isNotEmpty()) {
        params.entries.joinToString(", ") { "${it.key}=${it.value}" }
    } else ""

    LaunchedEffect(Unit) {
        Log.i("Screen:$screenName", "📱 Screen Composed${if (paramsStr.isNotEmpty()) " with $paramsStr" else ""}")
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.i("Screen:$screenName", "🗑️ Screen Disposed")
        }
    }

    content()
}

/**
 * Log user action on a screen
 */
fun logUserAction(screenName: String, action: String, details: String = "") {
    Log.d("Screen:$screenName", "👆 User action: $action${if (details.isNotEmpty()) " - $details" else ""}")
}

/**
 * Log screen state change
 */
fun logScreenState(screenName: String, state: String, details: String = "") {
    Log.d("Screen:$screenName", "🔄 State: $state${if (details.isNotEmpty()) " - $details" else ""}")
}

/**
 * Log screen error
 */
fun logScreenError(screenName: String, error: String, exception: Throwable? = null) {
    if (exception != null) {
        Log.e("Screen:$screenName", "❌ Error: $error", exception)
    } else {
        Log.e("Screen:$screenName", "❌ Error: $error")
    }
}

