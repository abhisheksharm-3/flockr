package `in`.xroden.flockr.utils

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The preference gate is the part that was broken before: the setting existed, wrote to
 * DataStore, and nothing read it. These tests fail if that regresses.
 */
class HapticsTest {

    private class RecordingFeedback : HapticFeedback {
        val performed = mutableListOf<HapticFeedbackType>()
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            performed += hapticFeedbackType
        }
    }

    @Test
    fun `fires when the preference is on`() {
        val feedback = RecordingFeedback()
        Haptics(feedback, mutableStateOf(true)).tap()
        assertEquals(listOf(HapticFeedbackType.ContextClick), feedback.performed)
    }

    @Test
    fun `stays silent when the preference is off`() {
        val feedback = RecordingFeedback()
        val haptics = Haptics(feedback, mutableStateOf(false))
        haptics.tap()
        haptics.toggleOn()
        haptics.success()
        haptics.error()
        haptics.select()
        haptics.longPress()
        haptics.gestureThreshold()
        assertTrue("no haptic may fire while disabled", feedback.performed.isEmpty())
    }

    @Test
    fun `preference is read at call time, not at construction`() {
        val feedback = RecordingFeedback()
        val enabled = mutableStateOf(true)
        val haptics = Haptics(feedback, enabled)

        haptics.tap()
        enabled.value = false
        haptics.tap()
        enabled.value = true
        haptics.tap()

        assertEquals(2, feedback.performed.size)
    }

    @Test
    fun `toggle picks the direction-specific effect`() {
        val feedback = RecordingFeedback()
        val haptics = Haptics(feedback, mutableStateOf(true))
        haptics.toggle(on = true)
        haptics.toggle(on = false)
        assertEquals(
            listOf(HapticFeedbackType.ToggleOn, HapticFeedbackType.ToggleOff),
            feedback.performed
        )
    }
}
