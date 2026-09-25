/** Turning the app lock on or off. */
package `in`.xroden.flockr.features.settings.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.settings.presentation.AppLockEvent
import `in`.xroden.flockr.features.settings.presentation.SettingsViewModel
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.forms.FormSectionCard
import `in`.xroden.flockr.ui.components.forms.ToggleRow
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics

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

    Scaffold(
        topBar = { FlockrTopAppBar(title = "Security", onNavigateBack = onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            FormSectionCard(icon = Icons.Rounded.Lock, title = "App lock") {
                ToggleRow(
                    title = "Lock Flockr",
                    subtitle = if (canUseAppLock || lockEnabled) {
                        "Ask for your fingerprint, face or screen lock when you come back to the app."
                    } else {
                        "Set up a fingerprint, face or screen lock on this phone to use this."
                    },
                    checked = lockEnabled,
                    onCheckedChange = { on -> if (on) viewModel.enableAppLock(activity) else viewModel.setAppLockEnabled(false) },
                    enabled = canUseAppLock || lockEnabled,
                )
            }
        }
    }
}
