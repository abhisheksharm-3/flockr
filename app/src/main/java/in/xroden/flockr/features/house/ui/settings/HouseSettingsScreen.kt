/** A house's settings: its name typed large on cobalt, how money and dates read as a sentence, the address and picture, then activity and the way out. */
package `in`.xroden.flockr.features.house.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.house.presentation.HouseSettingsUiState
import `in`.xroden.flockr.features.house.presentation.HouseSettingsViewModel
import `in`.xroden.flockr.features.house.ui.HouseLocaleSentence
import `in`.xroden.flockr.features.house.ui.HouseLocationRow
import `in`.xroden.flockr.features.house.ui.home.HouseImage
import `in`.xroden.flockr.ui.components.SkeletonFormScreen
import `in`.xroden.flockr.ui.components.BadgeTone
import `in`.xroden.flockr.ui.components.HeroStatusBarScrim
import `in`.xroden.flockr.ui.components.IconBadge
import `in`.xroden.flockr.ui.components.ListRow
import `in`.xroden.flockr.ui.components.Section
import `in`.xroden.flockr.ui.components.dialogs.ConfirmDialog
import `in`.xroden.flockr.ui.components.forms.FormHero
import `in`.xroden.flockr.ui.components.forms.FormSubmitBar
import `in`.xroden.flockr.ui.components.forms.HeroNote
import `in`.xroden.flockr.ui.components.forms.HeroTextInput
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.ui.components.isHeroScrolledAway
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.ComponentHeight
import `in`.xroden.flockr.ui.theme.IconSize
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics

private enum class ExitConfirm { LEAVE, DELETE }

@Composable
fun HouseSettingsScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAuditLog: () -> Unit = {},
    onDeleteHouse: () -> Unit = {},
    viewModel: HouseSettingsViewModel = hiltViewModel()
) {
    val haptics = rememberHaptics()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var confirm by rememberSaveable { mutableStateOf<ExitConfirm?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::uploadHeaderImage)
    }

    LaunchedEffect(houseId) { viewModel.load(houseId) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event.isError) haptics.error() else haptics.success()
            snackbarHostState.showSnackbar(event.message)
        }
    }
    LaunchedEffect(Unit) {
        viewModel.exited.collect {
            haptics.success()
            onDeleteHouse()
        }
    }

    val ready = state as? HouseSettingsUiState.Ready
    Scaffold(
        bottomBar = {
            if (ready?.canEdit == true) {
                FormSubmitBar(
                    text = if (ready.hasChanges) "Save changes" else "All changes saved",
                    onClick = viewModel::save,
                    enabled = ready.canSave,
                    isLoading = ready.isSaving,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (val current = state) {
            HouseSettingsUiState.Loading -> SkeletonFormScreen()
            is HouseSettingsUiState.Error -> ErrorState(current.message, modifier = Modifier.padding(padding), onRetry = { viewModel.load(houseId) })
            is HouseSettingsUiState.Ready -> Box(Modifier.fillMaxSize()) {
                SettingsForm(
                    form = current,
                    listState = listState,
                    contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + Spacing.xxl),
                    onUpdate = viewModel::update,
                    onPickImage = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onOpenActivity = onNavigateToAuditLog,
                    onLeave = { confirm = ExitConfirm.LEAVE },
                    onDelete = { confirm = ExitConfirm.DELETE },
                )
                HeroStatusBarScrim(isHeroGone = listState.isHeroScrolledAway)
            }
        }
    }

    val houseName = ready?.house?.name.orEmpty()
    when (confirm) {
        ExitConfirm.LEAVE -> ConfirmDialog(
            title = "Leave $houseName?",
            message = "You'll lose access until someone invites you again. Expenses you were part of stay on the ledger.",
            confirmText = "Leave",
            isDestructive = true,
            onConfirm = {
                confirm = null
                viewModel.leaveHouse()
            },
            onDismiss = { confirm = null },
        )
        ExitConfirm.DELETE -> ConfirmDialog(
            title = "Delete $houseName?",
            message = "This deletes the house and everything in it for everyone: expenses, balances, bills, chores, lists, chat and documents. It can't be undone.",
            confirmText = "Delete for everyone",
            isDestructive = true,
            onConfirm = {
                confirm = null
                viewModel.deleteHouse()
            },
            onDismiss = { confirm = null },
        )
        null -> Unit
    }
}

/**
 * The name on cobalt, then the sentence and the free-text fields, then the rows that lead elsewhere
 * or out of the house. Leaving and deleting sit last and apart, so they are never tapped on the way
 * to something else.
 */
@Composable
private fun SettingsForm(
    form: HouseSettingsUiState.Ready,
    listState: LazyListState,
    contentPadding: PaddingValues,
    onUpdate: ((HouseSettingsUiState.Ready) -> HouseSettingsUiState.Ready) -> Unit,
    onPickImage: () -> Unit,
    onOpenActivity: () -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit,
) {
    val isEnabled = form.canEdit && !form.isSaving
    val nameError = form.nameError.takeIf { form.canEdit }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Spacing.xl),
    ) {
        item {
            FormHero("House settings") {
                HeroTextInput(
                    value = form.name,
                    onValueChange = { name -> onUpdate { it.copy(name = name) } },
                    placeholder = "Name your house",
                    enabled = isEnabled,
                    style = MaterialTheme.typography.headlineLargeEmphasized,
                )
                HeroNote(
                    when {
                        !form.canEdit -> "Only the owner and admins can change these settings"
                        nameError != null -> nameError
                        else -> "Everyone in the house sees this name"
                    },
                    isError = nameError != null,
                )
            }
        }
        item {
            HouseLocaleSentence(
                currencyCode = form.currencyCode,
                onCurrencyChange = { code -> onUpdate { it.copy(currencyCode = code) } },
                dateFormat = form.dateFormat,
                onDateFormatChange = { pattern -> onUpdate { it.copy(dateFormat = pattern) } },
                firstDayOfWeek = form.firstDayOfWeek,
                onFirstDayOfWeekChange = { day -> onUpdate { it.copy(firstDayOfWeek = day) } },
                timezone = form.timezone,
                onTimezoneChange = { zone -> onUpdate { it.copy(timezone = zone) } },
                enabled = isEnabled,
                isCurrencyLocked = form.isCurrencyLocked,
            )
        }
        item {
            FlockrTextField(
                value = form.address,
                onValueChange = { address -> onUpdate { it.copy(address = address) } },
                label = "Address",
                placeholder = "Optional",
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
            )
        }
        item {
            HouseLocationRow(form.latitude, form.longitude, form.address, enabled = isEnabled) { place ->
                onUpdate { it.copy(latitude = place.latitude, longitude = place.longitude, address = it.address.ifBlank { place.address.orEmpty() }) }
            }
        }
        item {
            PictureRow(
                houseId = form.house.id,
                imageUrl = form.house.headerImageUrl,
                canEdit = form.canEdit,
                isUploading = form.isUploadingImage,
                onPickImage = onPickImage,
            )
        }
        item {
            Section(title = "History") {
                ListRow(
                    headline = "Activity",
                    supporting = "Everything that's changed in the house, and who changed it",
                    leading = { IconBadge(Icons.Rounded.History, BadgeTone.SLATE) },
                    trailing = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = onOpenActivity,
                )
            }
        }
        item {
            Section(title = if (form.isOwner) "Delete" else "Leave") {
                if (form.isOwner) {
                    ExitRow(
                        text = "Delete house",
                        icon = Icons.Rounded.DeleteForever,
                        note = "Deletes the house and everything in it for everyone",
                        enabled = !form.isSaving,
                        onClick = onDelete,
                    )
                } else {
                    ExitRow(
                        text = "Leave house",
                        icon = Icons.AutoMirrored.Rounded.Logout,
                        note = "You'll need a new invite to come back",
                        enabled = !form.isSaving,
                        onClick = onLeave,
                    )
                }
            }
        }
    }
}

/** The house picture in a circle beside the button that sets it; the picture shows on the home list and the house screen. */
@Composable
private fun PictureRow(houseId: String, imageUrl: String?, canEdit: Boolean, isUploading: Boolean, onPickImage: () -> Unit) {
    val haptics = rememberHaptics()
    ListRow(
        headline = "Picture",
        supporting = "On your home screen and at the top of the house",
        leading = { HouseImage(imageUrl = imageUrl, seed = houseId, modifier = Modifier.size(ComponentHeight.avatarLarge).clip(CircleShape)) },
        trailing = if (canEdit) {
            {
                FilledTonalButton(onClick = { haptics.tap(); onPickImage() }, enabled = !isUploading, shapes = ButtonDefaults.shapes()) {
                    if (isUploading) {
                        LoadingIndicator(modifier = Modifier.size(IconSize.sm), color = LocalContentColor.current)
                        Text("Uploading…", modifier = Modifier.padding(start = Spacing.sm))
                    } else {
                        Text(if (imageUrl != null) "Change" else "Add")
                    }
                }
            }
        } else {
            null
        },
    )
}

/** A way out of the house, in the error colour so it never reads as an ordinary setting. */
@Composable
private fun ExitRow(text: String, icon: ImageVector, note: String, enabled: Boolean, onClick: () -> Unit) {
    val haptics = rememberHaptics()
    ListRow(
        headline = text,
        supporting = note,
        headlineColor = MaterialTheme.colorScheme.error,
        leading = { IconBadge(icon, BadgeTone.ROSE) },
        onClick = if (enabled) ({ haptics.tap(); onClick() }) else null,
    )
}
