package `in`.xroden.flockr.utils

import android.annotation.SuppressLint
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * The user's "Haptic Feedback" preference, provided once at the root of the app from
 * [in.xroden.flockr.features.settings.presentation.SettingsViewModel].
 *
 * This is a [State] rather than a plain Boolean on purpose: [Haptics] reads it at click time,
 * outside composition, so flipping the setting recomposes nothing.
 */
val LocalHapticsEnabled = staticCompositionLocalOf<State<Boolean>> { mutableStateOf(true) }

/** What a haptic means. Each maps to one feel on the vibrator. */
enum class HapticEffect { TAP, TOGGLE_ON, TOGGLE_OFF, SELECT, SUCCESS, ERROR, LONG_PRESS, GESTURE_THRESHOLD, GESTURE_END }

/**
 * Haptic feedback for the app, named by what the interaction *means* rather than by how strong
 * the buzz is. Every call is gated on the user's preference, read at call time, and handed to
 * [play], which the app wires to the vibrator and tests wire to a recorder.
 */
@Stable
class Haptics(
    private val play: (HapticEffect) -> Unit,
    private val enabled: State<Boolean>,
) {
    private fun perform(effect: HapticEffect) {
        if (enabled.value) play(effect)
    }

    /** A primary call to action or FAB was tapped. */
    fun tap() = perform(HapticEffect.TAP)

    /** A switch, checkbox, or check-off row moved into the on position. */
    fun toggleOn() = perform(HapticEffect.TOGGLE_ON)

    /** A switch, checkbox, or check-off row moved into the off position. */
    fun toggleOff() = perform(HapticEffect.TOGGLE_OFF)

    /** Convenience for a toggle whose new state is already known. */
    fun toggle(on: Boolean) = if (on) toggleOn() else toggleOff()

    /** The user moved between discrete choices: a chip, segment, pill, page, or picker step. */
    fun select() = perform(HapticEffect.SELECT)

    /** An operation the user initiated completed successfully. */
    fun success() = perform(HapticEffect.SUCCESS)

    /** An operation failed, or a destructive action was confirmed. */
    fun error() = perform(HapticEffect.ERROR)

    /** A long press opened a context menu or entered a selection mode. */
    fun longPress() = perform(HapticEffect.LONG_PRESS)

    /** A drag or swipe crossed the threshold at which releasing would commit the action. */
    fun gestureThreshold() = perform(HapticEffect.GESTURE_THRESHOLD)

    /** A drag or swipe gesture settled where it was headed. */
    fun gestureEnd() = perform(HapticEffect.GESTURE_END)
}

/** One composed primitive: its [id] from [VibrationEffect.Composition], [scale] in 0..1, and a [delayMs] before it. */
private data class Beat(val id: Int, val scale: Float, val delayMs: Int = 0)

/**
 * Plays each effect as the vibrator's composition primitives, which feel the same on every phone
 * that has them, falling back to the predefined click and tick effects on one that doesn't. The
 * view feedback constants this replaced, such as context click and segment tick, are mapped by each
 * manufacturer and play nothing at all on many phones. Touch usage keeps every effect under the
 * system's touch-vibration strength setting. Lint can't follow a primitive id through [Beat], so it
 * flags the ids passed to `addPrimitive` as unchecked; every id here is a `Composition` constant.
 */
@SuppressLint("WrongConstant")
private fun vibratorPlayer(vibrator: Vibrator?): (HapticEffect) -> Unit {
    val touch = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
    fun play(fallback: Int, vararg beats: Beat) {
        val target = vibrator?.takeIf { it.hasVibrator() } ?: return
        val effect = if (target.areAllPrimitivesSupported(*beats.map { it.id }.toIntArray())) {
            VibrationEffect.startComposition().apply { beats.forEach { addPrimitive(it.id, it.scale, it.delayMs) } }.compose()
        } else {
            VibrationEffect.createPredefined(fallback)
        }
        target.vibrate(effect, touch)
    }
    val click = VibrationEffect.Composition.PRIMITIVE_CLICK
    val tick = VibrationEffect.Composition.PRIMITIVE_TICK
    return { effect ->
        when (effect) {
            HapticEffect.TAP -> play(VibrationEffect.EFFECT_CLICK, Beat(click, 0.6f))
            HapticEffect.TOGGLE_ON -> play(VibrationEffect.EFFECT_CLICK, Beat(click, 0.9f))
            HapticEffect.TOGGLE_OFF -> play(VibrationEffect.EFFECT_TICK, Beat(tick, 0.7f))
            HapticEffect.SELECT -> play(VibrationEffect.EFFECT_TICK, Beat(tick, 0.5f))
            HapticEffect.SUCCESS -> play(VibrationEffect.EFFECT_DOUBLE_CLICK, Beat(tick, 0.5f), Beat(click, 0.9f, delayMs = 70))
            HapticEffect.ERROR -> play(VibrationEffect.EFFECT_HEAVY_CLICK, Beat(click, 1f), Beat(click, 1f, delayMs = 90))
            HapticEffect.LONG_PRESS -> play(VibrationEffect.EFFECT_HEAVY_CLICK, Beat(VibrationEffect.Composition.PRIMITIVE_THUD, 0.8f))
            HapticEffect.GESTURE_THRESHOLD -> play(VibrationEffect.EFFECT_TICK, Beat(tick, 0.8f))
            HapticEffect.GESTURE_END -> play(VibrationEffect.EFFECT_CLICK, Beat(click, 0.4f))
        }
    }
}

/** Haptics for the current composition, honouring the user's preference. */
@Composable
fun rememberHaptics(): Haptics {
    val context = LocalContext.current
    val enabled = LocalHapticsEnabled.current
    return remember(context, enabled) {
        Haptics(vibratorPlayer(context.getSystemService(VibratorManager::class.java)?.defaultVibrator), enabled)
    }
}
