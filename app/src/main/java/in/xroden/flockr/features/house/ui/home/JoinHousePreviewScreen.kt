/** The house an invite link opens, shown before the user joins it. */
package `in`.xroden.flockr.features.house.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import `in`.xroden.flockr.features.house.model.HousePreview
import `in`.xroden.flockr.features.house.presentation.HomeViewModel
import `in`.xroden.flockr.features.house.presentation.HouseEvent
import `in`.xroden.flockr.features.house.presentation.HousePreviewUiState
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.buttons.FlockrPrimaryButton
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.ComponentHeight
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics

@Composable
fun JoinHousePreviewScreen(
    inviteCode: String,
    onNavigateBack: () -> Unit,
    onHouseJoined: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val haptics = rememberHaptics()
    val previewState by viewModel.previewState.collectAsStateWithLifecycle()
    val isJoining by viewModel.isJoining.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(inviteCode) { viewModel.validateInviteCode(inviteCode) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HouseEvent.Joined -> {
                    haptics.success()
                    onHouseJoined()
                }
                is HouseEvent.Failed -> {
                    haptics.error()
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        topBar = { FlockrTopAppBar(title = "Join a house", onNavigateBack = onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val current = previewState) {
                HousePreviewUiState.Idle, HousePreviewUiState.Loading -> LoadingIndicator(Modifier.align(Alignment.Center))
                is HousePreviewUiState.Error -> ErrorState(current.message, onRetry = { viewModel.validateInviteCode(inviteCode) })
                is HousePreviewUiState.Success -> Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.xl, vertical = Spacing.lg),
                ) {
                    HousePreviewCard(
                        preview = current.preview,
                        isJoining = isJoining,
                        onJoin = { viewModel.joinHouseByInviteCode(inviteCode) },
                    )
                }
            }
        }
    }
}

/**
 * The house behind an invite code and the button that joins it. A full house keeps the button
 * disabled and says why, rather than letting the join fail on the server.
 */
@Composable
internal fun HousePreviewCard(
    preview: HousePreview,
    isJoining: Boolean,
    onJoin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        HouseImage(
            imageUrl = preview.headerImageUrl,
            seed = preview.id,
            modifier = Modifier.fillMaxWidth().height(ComponentHeight.cardMedium),
        )
        Column(Modifier.padding(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(preview.name, style = MaterialTheme.typography.headlineSmallEmphasized)
                Text(
                    "Run by ${preview.ownerName} · ${if (preview.memberCount == 1) "1 member" else "${preview.memberCount} members"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (preview.isFull) {
                Text(
                    "This house is full. Ask ${preview.ownerName} to make room, then try the code again.",
                    style = MaterialTheme.typography.bodyMediumEmphasized,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            FlockrPrimaryButton(
                text = if (isJoining) "Joining…" else "Join house",
                onClick = onJoin,
                enabled = !preview.isFull,
                isLoading = isJoining,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
