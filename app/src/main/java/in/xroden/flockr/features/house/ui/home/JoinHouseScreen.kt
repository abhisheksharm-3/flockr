/** Joining a house by typing the invite code a housemate shared, with the house shown before joining. */
package `in`.xroden.flockr.features.house.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.core.validation.INVITE_CODE_LENGTH
import `in`.xroden.flockr.features.house.presentation.HomeViewModel
import `in`.xroden.flockr.features.house.presentation.HouseEvent
import `in`.xroden.flockr.features.house.presentation.HousePreviewUiState
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.ui.theme.IconSize
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics

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
        topBar = { FlockrTopAppBar(title = "Join a house", onNavigateBack = onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(
                "Type the code a housemate shared with you. It's $INVITE_CODE_LENGTH letters and numbers, and you'll see the house before you join.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val error = (previewState as? HousePreviewUiState.Error)?.message
            FlockrTextField(
                value = code,
                onValueChange = { typed -> code = typed.filter(Char::isLetterOrDigit).uppercase().take(INVITE_CODE_LENGTH) },
                label = "Invite code",
                enabled = !isJoining,
                isError = error != null,
                supportingText = error ?: "${code.length} of $INVITE_CODE_LENGTH",
                trailingIcon = if (previewState is HousePreviewUiState.Loading) {
                    { LoadingIndicator(Modifier.size(IconSize.md)) }
                } else {
                    null
                },
                keyboardType = KeyboardType.Ascii,
                capitalization = KeyboardCapitalization.Characters,
                modifier = Modifier.fillMaxWidth(),
            )
            (previewState as? HousePreviewUiState.Success)?.let { success ->
                HousePreviewCard(
                    preview = success.preview,
                    isJoining = isJoining,
                    onJoin = { viewModel.joinHouseByInviteCode(code) },
                )
            }
        }
    }
}
