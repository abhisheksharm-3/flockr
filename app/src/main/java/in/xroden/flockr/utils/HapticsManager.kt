package `in`.xroden.flockr.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.xroden.flockr.features.settings.data.ISettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for haptic feedback across the app.
 * Provides different haptic patterns for various interactions.
 */
@Singleton
class HapticsManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: ISettingsRepository
) {
    private val vibrator: Vibrator by lazy {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    }

    // Cached preference so click handlers never block the main thread on a DataStore read.
    @Volatile
    private var hapticsEnabledCache: Boolean = true

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            settingsRepository.hapticsEnabled.collect { hapticsEnabledCache = it }
        }
    }

    /**
     * Light tap - for toggles, list item taps, minor interactions
     */
    suspend fun lightTap() {
        if (!isEnabled()) return
        vibrate(HapticType.LIGHT)
    }

    /**
     * Medium feedback - for button presses
     */
    suspend fun mediumTap() {
        if (!isEnabled()) return
        vibrate(HapticType.MEDIUM)
    }

    /**
     * Heavy feedback - for important actions like delete, submit
     */
    suspend fun heavyTap() {
        if (!isEnabled()) return
        vibrate(HapticType.HEAVY)
    }

    /**
     * Success feedback - for successful operations
     */
    suspend fun success() {
        if (!isEnabled()) return
        vibrate(HapticType.SUCCESS)
    }

    /**
     * Error feedback - for errors or warnings
     */
    suspend fun error() {
        if (!isEnabled()) return
        vibrate(HapticType.ERROR)
    }

    /**
     * Selection feedback - for item selection
     */
    suspend fun selection() {
        if (!isEnabled()) return
        vibrate(HapticType.SELECTION)
    }

    /**
     * Perform haptic on a view using system constants (more efficient)
     */
    fun performHapticFeedback(view: View, type: HapticType = HapticType.LIGHT) {
        if (!isEnabled()) return

        val feedbackConstant = when (type) {
            HapticType.LIGHT -> HapticFeedbackConstants.CLOCK_TICK
            HapticType.MEDIUM -> HapticFeedbackConstants.CONTEXT_CLICK
            HapticType.HEAVY -> HapticFeedbackConstants.LONG_PRESS
            HapticType.SUCCESS -> HapticFeedbackConstants.CONFIRM
            HapticType.ERROR -> HapticFeedbackConstants.REJECT
            HapticType.SELECTION -> HapticFeedbackConstants.KEYBOARD_TAP
        }

        view.performHapticFeedback(feedbackConstant)
    }

    private fun isEnabled(): Boolean = hapticsEnabledCache

    /**
     * Current haptics preference (cached; safe to read from the main thread).
     */
    val isHapticsEnabled: Boolean
        get() = hapticsEnabledCache

    @SuppressLint("MissingPermission")
    private fun vibrate(type: HapticType) {
        if (!vibrator.hasVibrator()) return

        val effect = when (type) {
            HapticType.LIGHT -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            HapticType.MEDIUM -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            HapticType.HEAVY -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            HapticType.SUCCESS -> VibrationEffect.createWaveform(
                longArrayOf(0, 30, 50, 30),
                intArrayOf(0, 100, 0, 100),
                -1
            )
            HapticType.ERROR -> VibrationEffect.createWaveform(
                longArrayOf(0, 50, 30, 50),
                intArrayOf(0, 150, 0, 150),
                -1
            )
            HapticType.SELECTION -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        }
        vibrator.vibrate(effect)
    }

    enum class HapticType {
        LIGHT,
        MEDIUM,
        HEAVY,
        SUCCESS,
        ERROR,
        SELECTION
    }
}

/**
 * Composable helper to get HapticsManager and perform haptics on click.
 * Respects user's haptics preference setting.
 */
@Composable
fun rememberHapticFeedback(): HapticFeedback {
    val view = LocalView.current
    val hapticsManager = LocalHapticsManager.current
    return remember(view, hapticsManager) { HapticFeedback(view, hapticsManager) }
}

/**
 * Composition local for providing HapticsManager to the composable tree.
 * This should be provided at the app level.
 */
val LocalHapticsManager = androidx.compose.runtime.staticCompositionLocalOf<HapticsManager?> { null }

class HapticFeedback(private val view: View, private val hapticsManager: HapticsManager?) {

    private fun isEnabled(): Boolean {
        // If no manager available, allow haptics by default
        return hapticsManager?.isHapticsEnabled ?: true
    }

    fun performClick() {
        if (!isEnabled()) return
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    fun performLightClick() {
        if (!isEnabled()) return
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun performHeavyClick() {
        if (!isEnabled()) return
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    fun performSuccess() {
        if (!isEnabled()) return
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    fun performError() {
        if (!isEnabled()) return
        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
    }

    fun performSelection() {
        if (!isEnabled()) return
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
}

