package `in`.xroden.flockr.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * The user's "Haptic Feedback" preference, provided once at the root of the app from
 * [in.xroden.flockr.features.settings.presentation.SettingsViewModel].
 *
 * This is a [State] rather than a plain Boolean on purpose: [Haptics] reads it at click time,
 * outside composition, so flipping the setting recomposes nothing.
 */
val LocalHapticsEnabled = staticCompositionLocalOf<State<Boolean>> { mutableStateOf(true) }

/**
 * Haptic feedback for the app, named by what the interaction *means* rather than by how strong
 * the buzz is. Every call is gated on the user's preference.
 *
 * The vocabulary is deliberately small — one function per interaction class in the app's haptic
 * policy. Anything not listed here is meant to stay silent: plain navigation, list rows that only
 * navigate, tab switches, and text entry (the IME supplies its own feedback).
 */
@Stable
class Haptics(
    private val feedback: HapticFeedback,
    private val enabled: State<Boolean>,
) {
    private fun perform(type: HapticFeedbackType) {
        if (enabled.value) feedback.performHapticFeedback(type)
    }

    /** A primary call to action or FAB was tapped. */
    fun tap() = perform(HapticFeedbackType.ContextClick)

    /** A switch, checkbox, or check-off row moved into the on position. */
    fun toggleOn() = perform(HapticFeedbackType.ToggleOn)

    /** A switch, checkbox, or check-off row moved into the off position. */
    fun toggleOff() = perform(HapticFeedbackType.ToggleOff)

    /** Convenience for a toggle whose new state is already known. */
    fun toggle(on: Boolean) = if (on) toggleOn() else toggleOff()

    /** The user moved between discrete choices: a filter chip, segment, pill, or picker step. */
    fun select() = perform(HapticFeedbackType.SegmentTick)

    /** An operation the user initiated completed successfully. */
    fun success() = perform(HapticFeedbackType.Confirm)

    /** An operation failed, or a destructive action was confirmed. */
    fun error() = perform(HapticFeedbackType.Reject)

    /** A long press opened a context menu or entered a selection mode. */
    fun longPress() = perform(HapticFeedbackType.LongPress)

    /** A drag or swipe crossed the threshold at which releasing would commit the action. */
    fun gestureThreshold() = perform(HapticFeedbackType.GestureThresholdActivate)

    /** A drag or swipe gesture completed. */
    fun gestureEnd() = perform(HapticFeedbackType.GestureEnd)
}

/**
 * Haptics for the current composition, honouring the user's preference.
 *
 * Backed by Compose's own [LocalHapticFeedback], so it routes through
 * `View.performHapticFeedback` and therefore also respects the system's touch-feedback setting.
 */
@Composable
fun rememberHaptics(): Haptics {
    val feedback = LocalHapticFeedback.current
    val enabled = LocalHapticsEnabled.current
    return remember(feedback, enabled) { Haptics(feedback, enabled) }
}
