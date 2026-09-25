/** A house's settings: name, address and picture, money and dates, its activity, and leaving or deleting it. */
package `in`.xroden.flockr.features.house.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import `in`.xroden.flockr.features.house.presentation.HouseSettingsUiState
import `in`.xroden.flockr.features.house.presentation.HouseSettingsViewModel
import `in`.xroden.flockr.features.house.ui.HouseLocaleFields
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.buttons.FlockrPrimaryButton
import `in`.xroden.flockr.ui.components.dialogs.ConfirmDialog
import `in`.xroden.flockr.ui.components.forms.FormSectionCard
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
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
        topBar = { FlockrTopAppBar(title = "House settings", onNavigateBack = onNavigateBack) },
        bottomBar = {
            if (ready?.canEdit == true) {
                FlockrPrimaryButton(
                    text = "Save changes",
                    onClick = viewModel::save,
                    enabled = ready.canSave,
                    isLoading = ready.isSaving,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Spacing.xl, vertical = Spacing.lg),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val current = state) {
                HouseSettingsUiState.Loading -> LoadingIndicator(Modifier.align(Alignment.Center))
                is HouseSettingsUiState.Error -> ErrorState(current.message, onRetry = { viewModel.load(houseId) })
                is HouseSettingsUiState.Ready -> SettingsForm(
                    form = current,
                    onUpdate = viewModel::update,
                    onPickImage = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onOpenActivity = onNavigateToAuditLog,
                    onLeave = { confirm = ExitConfirm.LEAVE },
                    onDelete = { confirm = ExitConfirm.DELETE },
                )
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

@Composable
private fun SettingsForm(
    form: HouseSettingsUiState.Ready,
    onUpdate: ((HouseSettingsUiState.Ready) -> HouseSettingsUiState.Ready) -> Unit,
    onPickImage: () -> Unit,
    onOpenActivity: () -> Unit,
    onLeave: () -> Unit,
    onDelete: () -> Unit,
) {
    val isEnabled = form.canEdit && !form.isSaving
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.xl, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        if (!form.canEdit) {
            Text(
                "Only the owner and admins can change these settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FormSectionCard(icon = Icons.Rounded.Home, title = "House") {
            FlockrTextField(
                value = form.name,
                onValueChange = { name -> onUpdate { it.copy(name = name) } },
                label = "Name",
                placeholder = "Maple Street flat",
                isError = form.canEdit && form.nameError != null,
                supportingText = form.nameError.takeIf { form.canEdit },
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
            FlockrTextField(
                value = form.address,
                onValueChange = { address -> onUpdate { it.copy(address = address) } },
                label = "Address",
                placeholder = "Optional",
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        HeaderImageSection(
            imageUrl = form.house.headerImageUrl,
            canEdit = form.canEdit,
            isUploading = form.isUploadingImage,
            onPickImage = onPickImage,
        )
        LocalizationSection(
            currency = form.currencyCode,
            onCurrencyChange = { code -> onUpdate { it.copy(currencyCode = code) } },
            dateFormat = form.dateFormat,
            onDateFormatChange = { pattern -> onUpdate { it.copy(dateFormat = pattern) } },
            firstDayOfWeek = form.firstDayOfWeek,
            onFirstDayChange = { day -> onUpdate { it.copy(firstDayOfWeek = day) } },
            timezone = form.timezone,
            onTimezoneChange = { zone -> onUpdate { it.copy(timezone = zone) } },
            isCurrencyLocked = form.isCurrencyLocked,
            enabled = isEnabled,
        )
        ActivityLink(onClick = onOpenActivity)
        if (form.isOwner) {
            ExitButton(
                text = "Delete house",
                icon = Icons.Rounded.DeleteForever,
                note = "Deletes the house and everything in it for everyone.",
                enabled = !form.isSaving,
                onClick = onDelete,
            )
        } else {
            ExitButton(
                text = "Leave house",
                icon = Icons.AutoMirrored.Rounded.Logout,
                note = "You'll need a new invite to come back.",
                enabled = !form.isSaving,
                onClick = onLeave,
            )
        }
    }
}

@Composable
private fun HeaderImageSection(imageUrl: String?, canEdit: Boolean, isUploading: Boolean, onPickImage: () -> Unit) {
    val haptics = rememberHaptics()
    FormSectionCard(icon = Icons.Rounded.Image, title = "Picture", iconTint = MaterialTheme.colorScheme.tertiary) {
        val imageModifier = Modifier.fillMaxWidth().height(ComponentHeight.cardSmall).clip(MaterialTheme.shapes.large)
        if (imageUrl != null) {
            AsyncImage(model = imageUrl, contentDescription = "House picture", contentScale = ContentScale.Crop, modifier = imageModifier)
        } else {
            Surface(color = MaterialTheme.colorScheme.tertiaryContainer, modifier = imageModifier) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Home, contentDescription = null, modifier = Modifier.size(IconSize.xl))
                }
            }
        }
        if (canEdit) {
            FilledTonalButton(onClick = { haptics.tap(); onPickImage() }, enabled = !isUploading, modifier = Modifier.fillMaxWidth()) {
                if (isUploading) {
                    LoadingIndicator(modifier = Modifier.size(IconSize.sm), color = LocalContentColor.current)
                    Text("Uploading…", modifier = Modifier.padding(start = Spacing.sm))
                } else {
                    Text(if (imageUrl != null) "Change picture" else "Add a picture")
                }
            }
        }
    }
}

@Composable
private fun LocalizationSection(
    currency: String,
    onCurrencyChange: (String) -> Unit,
    dateFormat: String,
    onDateFormatChange: (String) -> Unit,
    firstDayOfWeek: Int,
    onFirstDayChange: (Int) -> Unit,
    timezone: String,
    onTimezoneChange: (String) -> Unit,
    isCurrencyLocked: Boolean,
    enabled: Boolean,
) {
    FormSectionCard(title = "Money and dates", icon = Icons.Rounded.Language, iconTint = MaterialTheme.colorScheme.secondary) {
        HouseLocaleFields(
            currencyCode = currency,
            onCurrencyChange = onCurrencyChange,
            dateFormat = dateFormat,
            onDateFormatChange = onDateFormatChange,
            firstDayOfWeek = firstDayOfWeek,
            onFirstDayOfWeekChange = onFirstDayChange,
            timezone = timezone,
            onTimezoneChange = onTimezoneChange,
            enabled = enabled,
            isCurrencyLocked = isCurrencyLocked,
        )
    }
}

@Composable
private fun ActivityLink(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.largeIncreased,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.xl),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text("Activity", style = MaterialTheme.typography.titleMediumEmphasized)
                Text(
                    "Everything that's changed in the house, and who changed it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ExitButton(
    text: String,
    icon: ImageVector,
    note: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val haptics = rememberHaptics()
    Column(Modifier.padding(top = Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        OutlinedButton(
            onClick = { haptics.tap(); onClick() },
            enabled = enabled,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(IconSize.sm))
            Text(text, modifier = Modifier.padding(start = Spacing.sm))
        }
        Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
