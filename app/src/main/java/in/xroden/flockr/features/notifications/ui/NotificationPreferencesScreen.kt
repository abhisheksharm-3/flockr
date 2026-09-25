/** Choosing which notifications each house sends you. */
package `in`.xroden.flockr.features.notifications.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.LoadingIndicator
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.notifications.model.NotificationGroup
import `in`.xroden.flockr.features.notifications.presentation.NotificationPreferencesUiState
import `in`.xroden.flockr.features.notifications.presentation.NotificationPreferencesViewModel
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.cards.SectionCard
import `in`.xroden.flockr.ui.components.forms.ToggleRow
import `in`.xroden.flockr.ui.components.states.EmptyState
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.Spacing

@Composable
fun NotificationPreferencesScreen(onNavigateBack: () -> Unit, viewModel: NotificationPreferencesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val error = (state as? NotificationPreferencesUiState.Ready)?.error

    LaunchedEffect(error) {
        error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error)
        viewModel.dismissError()
    }

    Scaffold(
        topBar = { FlockrTopAppBar(title = "Notifications", subtitle = "What each house tells you about", onNavigateBack = onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val current = state) {
                NotificationPreferencesUiState.Loading -> LoadingIndicator(Modifier.align(Alignment.Center))
                is NotificationPreferencesUiState.Error -> ErrorState(current.message, onRetry = viewModel::load)
                is NotificationPreferencesUiState.Ready -> if (current.houses.isEmpty()) {
                    EmptyState(icon = Icons.Rounded.NotificationsOff, title = "No houses yet", subtitle = "Join or create a house to choose what it tells you about.")
                } else {
                    LazyColumn(contentPadding = PaddingValues(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                        items(current.houses, key = { it.house.id }) { prefs ->
                            SectionCard(title = prefs.house.name) {
                                NotificationGroup.entries.forEach { group ->
                                    val kinds = prefs.enabled.keys.filter { it.group == group }
                                    if (kinds.isEmpty()) return@forEach
                                    Text(group.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                    kinds.forEach { type ->
                                        ToggleRow(
                                            title = type.label,
                                            checked = prefs.enabled.getValue(type),
                                            onCheckedChange = { viewModel.set(prefs.house.id, type, it) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
