/** The house an invite link opens, shown before the user joins it. */
package `in`.xroden.flockr.features.house.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import `in`.xroden.flockr.ui.components.HeroActions
import `in`.xroden.flockr.ui.components.HeroButton
import `in`.xroden.flockr.ui.components.HeroCaption
import `in`.xroden.flockr.ui.components.HeroHeader
import `in`.xroden.flockr.ui.components.HeroLabel
import `in`.xroden.flockr.ui.components.HeroSecondaryButton
import `in`.xroden.flockr.ui.components.states.ErrorState
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

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        val current = previewState
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = padding.calculateBottomPadding())) {
            if (current is HousePreviewUiState.Success) {
                InvitationHero(
                    preview = current.preview,
                    isJoining = isJoining,
                    onNavigateBack = onNavigateBack,
                    onJoin = { viewModel.joinHouseByInviteCode(inviteCode) },
                )
            } else {
                HeroHeader(title = "Join a house") {
                    HeroCaption(if (current is HousePreviewUiState.Error) "That invite didn't open." else "Opening the invite…")
                }
            }
            when (current) {
                HousePreviewUiState.Idle, HousePreviewUiState.Loading -> Box(Modifier.fillMaxWidth().padding(Spacing.xxxl), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
                is HousePreviewUiState.Error -> ErrorState(current.message, onRetry = { viewModel.validateInviteCode(inviteCode) })
                is HousePreviewUiState.Success -> Text(
                    "Once you're in, you'll see the house's balances, bills, chores and lists, and your housemates will see you in Members.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xl),
                )
            }
        }
    }
}

/**
 * The house the invite opens, its photo washed in cobalt behind it, with the button that joins it
 * and one that declines. A full house gets no join button and says why, rather than letting the join
 * fail on the server.
 */
@Composable
private fun InvitationHero(preview: HousePreview, isJoining: Boolean, onNavigateBack: () -> Unit, onJoin: () -> Unit) {
    HeroHeader(title = "Join a house", imageUrl = preview.headerImageUrl) {
        HeroLabel("You're invited to")
        Text(preview.name, style = MaterialTheme.typography.displaySmallEmphasized)
        HeroCaption("Run by ${preview.ownerName} · ${if (preview.memberCount == 1) "1 member" else "${preview.memberCount} members"}")
        if (preview.isFull) {
            HeroCaption("This house is full. Ask ${preview.ownerName} to make room, then try the invite again.")
            HeroActions { HeroSecondaryButton(text = "Not now", onClick = onNavigateBack) }
        } else {
            HeroActions {
                HeroButton(text = if (isJoining) "Joining…" else "Join ${preview.name}", onClick = onJoin, enabled = !isJoining)
                HeroSecondaryButton(text = "Not now", onClick = onNavigateBack)
            }
        }
    }
}
