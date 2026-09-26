package `in`.xroden.flockr.utils

import androidx.compose.runtime.mutableStateOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The preference gate is the part that was broken before: the setting existed, wrote to
 * DataStore, and nothing read it. These tests fail if that regresses, or if an interaction starts
 * playing the wrong feel.
 */
class HapticsTest {

    private val played = mutableListOf<HapticEffect>()

    @Test
    fun `fires when the preference is on`() {
        Haptics(played::add, mutableStateOf(true)).tap()
        assertEquals(listOf(HapticEffect.TAP), played)
    }

    @Test
    fun `stays silent when the preference is off`() {
        val haptics = Haptics(played::add, mutableStateOf(false))
        haptics.tap()
        haptics.toggleOn()
        haptics.success()
        haptics.error()
        haptics.select()
        haptics.longPress()
        haptics.gestureThreshold()
        haptics.gestureEnd()
        assertTrue("no haptic may fire while disabled", played.isEmpty())
    }

    @Test
    fun `preference is read at call time, not at construction`() {
        val enabled = mutableStateOf(true)
        val haptics = Haptics(played::add, enabled)

        haptics.tap()
        enabled.value = false
        haptics.tap()
        enabled.value = true
        haptics.tap()

        assertEquals(2, played.size)
    }

    @Test
    fun `toggle picks the direction-specific effect`() {
        val haptics = Haptics(played::add, mutableStateOf(true))
        haptics.toggle(on = true)
        haptics.toggle(on = false)
        assertEquals(listOf(HapticEffect.TOGGLE_ON, HapticEffect.TOGGLE_OFF), played)
    }
}
