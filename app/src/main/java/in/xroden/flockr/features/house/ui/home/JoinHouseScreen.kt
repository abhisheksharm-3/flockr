/** Joining a house by typing the invite code a housemate shared, with the house shown before joining. */
package `in`.xroden.flockr.features.house.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.core.validation.INVITE_CODE_LENGTH
import `in`.xroden.flockr.features.house.model.HousePreview
import `in`.xroden.flockr.features.house.presentation.HomeViewModel
import `in`.xroden.flockr.features.house.presentation.HouseEvent
import `in`.xroden.flockr.features.house.presentation.HousePreviewUiState
import `in`.xroden.flockr.ui.components.HeroColumn
import `in`.xroden.flockr.ui.components.ListRow
import `in`.xroden.flockr.ui.components.SectionTitle
import `in`.xroden.flockr.ui.components.forms.FormHero
import `in`.xroden.flockr.ui.components.forms.FormSubmitBar
import `in`.xroden.flockr.ui.components.forms.HeroNote
import `in`.xroden.flockr.ui.components.forms.HeroTextInput
import `in`.xroden.flockr.ui.theme.ComponentHeight
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics

/** The code typed large on cobalt; once it's complete the house it opens shows below, and the bar joins it. */
@Composable
fun JoinHouseScreen(
    onHouseJoined: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val haptics = rememberHaptics()
    val previewState by viewModel.previewState.collectAsStateWithLifecycle()
    val isJoining by viewModel.isJoining.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var code by rememberSaveable { mutableStateOf("") }
    val preview = (previewState as? HousePreviewUiState.Success)?.preview
    val error = (previewState as? HousePreviewUiState.Error)?.message

    LaunchedEffect(code) {
        if (code.length == INVITE_CODE_LENGTH) viewModel.validateInviteCode(code) else viewModel.resetPreviewState()
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HouseEvent.Joined -> {
                    haptics.success()
                    onHouseJoined(event.houseId)
                }
                is HouseEvent.Failed -> {
                    haptics.error()
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            FormSubmitBar(
                text = when {
                    isJoining -> "Joining…"
                    preview != null -> "Join ${preview.name}"
                    else -> "Join house"
                },
                onClick = { viewModel.joinHouseByInviteCode(code) },
                enabled = preview != null && !preview.isFull,
                isLoading = isJoining,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        HeroColumn(
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
            hero = {
                FormHero("Join a house") {
                    HeroTextInput(
                        value = code,
                        onValueChange = { typed -> code = typed.filter(Char::isLetterOrDigit).uppercase().take(INVITE_CODE_LENGTH) },
                        placeholder = "Invite code",
                        enabled = !isJoining,
                        autoFocus = true,
                        style = MaterialTheme.typography.displaySmallEmphasized,
                    )
                    HeroNote(
                        when {
                            error != null -> error
                            previewState is HousePreviewUiState.Loading -> "Looking for the house…"
                            preview != null -> "Found it"
                            else -> "${code.length} of $INVITE_CODE_LENGTH letters and numbers, from the invite a housemate sent"
                        },
                        isError = error != null,
                    )
                }
            },
        ) {
            preview?.let { FoundHouse(it) }
        }
    }
}

/**
 * The house behind the code, straight on the page. A full house says why the join button stays
 * disabled, rather than letting the join fail on the server.
 */
@Composable
private fun FoundHouse(preview: HousePreview) {
    Column {
        SectionTitle("The code opens")
        ListRow(
            headline = preview.name,
            supporting = "Run by ${preview.ownerName} · ${if (preview.memberCount == 1) "1 member" else "${preview.memberCount} members"}",
            leading = {
                HouseImage(
                    imageUrl = preview.headerImageUrl,
                    seed = preview.id,
                    modifier = Modifier.size(ComponentHeight.avatarLarge).clip(CircleShape),
                )
            },
        )
        if (preview.isFull) {
            Text(
                "This house is full. Ask ${preview.ownerName} to make room, then try the code again.",
                style = MaterialTheme.typography.bodyMediumEmphasized,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = Spacing.lg),
            )
        }
        Text(
            "Once you're in, you'll see its balances, bills, chores and lists, and your housemates will see you in Members.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
        )
    }
}
