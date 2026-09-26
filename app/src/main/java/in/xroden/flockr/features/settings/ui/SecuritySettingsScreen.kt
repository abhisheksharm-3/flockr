/** Turning the app lock on or off. */
package `in`.xroden.flockr.features.settings.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.settings.presentation.AppLockEvent
import `in`.xroden.flockr.features.settings.presentation.SettingsViewModel
import `in`.xroden.flockr.ui.components.AnimatedGlyph
import `in`.xroden.flockr.ui.components.GlyphMotion
import `in`.xroden.flockr.ui.components.HeroActions
import `in`.xroden.flockr.ui.components.HeroButton
import `in`.xroden.flockr.ui.components.HeroCaption
import `in`.xroden.flockr.ui.components.HeroColumn
import `in`.xroden.flockr.ui.components.HeroHeader
import `in`.xroden.flockr.ui.components.SectionTitle
import `in`.xroden.flockr.ui.theme.IconSize
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics

/** The cobalt says whether the lock is on and offers to turn it on; the switch below is the one control. */
@Composable
fun SecuritySettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val haptics = rememberHaptics()
    val activity = LocalActivity.current as? FragmentActivity
    val lockEnabled by viewModel.appLockEnabled.collectAsStateWithLifecycle()
    val canUseAppLock = remember { viewModel.canUseAppLock() }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.appLockEvents.collect { event ->
            when (event) {
                AppLockEvent.Enabled -> haptics.success()
                is AppLockEvent.Failed -> {
                    haptics.error()
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        HeroColumn(
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            hero = {
                HeroHeader(title = "Security") {
                    Row(
                        modifier = Modifier.padding(top = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        AnimatedGlyph(
                            if (lockEnabled) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                            trigger = lockEnabled,
                            motion = GlyphMotion.POP,
                            contentDescription = null,
                            modifier = Modifier.size(IconSize.lg),
                        )
                        Text(if (lockEnabled) "App lock is on" else "App lock is off", style = MaterialTheme.typography.headlineSmallEmphasized)
                    }
                    HeroCaption(
                        when {
                            lockEnabled -> "Only you can open Flockr, even when someone else picks up your phone."
                            canUseAppLock -> "Anyone holding your unlocked phone can open Flockr."
                            else -> "Set up a fingerprint, face or screen lock on this phone to lock Flockr."
                        },
                    )
                    if (!lockEnabled && canUseAppLock) {
                        HeroActions { HeroButton("Turn on app lock", onClick = { viewModel.enableAppLock(activity) }) }
                    }
                }
            },
        ) {
            SectionTitle("App lock")
            SwitchListRow(
                icon = Icons.Rounded.Fingerprint,
                title = "Lock Flockr",
                subtitle = if (canUseAppLock || lockEnabled) {
                    "Ask for your fingerprint, face or screen lock when you come back to the app"
                } else {
                    "Needs a fingerprint, face or screen lock on this phone"
                },
                checked = lockEnabled,
                onCheckedChange = { on -> if (on) viewModel.enableAppLock(activity) else viewModel.setAppLockEnabled(false) },
                enabled = canUseAppLock || lockEnabled,
            )
        }
    }
}
