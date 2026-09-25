/** Adding or editing a per-diem item and its price, and retiring it. */
package `in`.xroden.flockr.features.expenses.ui.perdiem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.expenses.model.PerDiemCategories
import `in`.xroden.flockr.features.expenses.presentation.PerDiemItemFormViewModel
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.buttons.FlockrPrimaryButton
import `in`.xroden.flockr.ui.components.dialogs.ConfirmDialog
import `in`.xroden.flockr.ui.components.inputs.AmountField
import `in`.xroden.flockr.ui.components.inputs.ChoiceField
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics

@Composable
fun PerDiemItemFormScreen(
    houseId: String,
    configId: String?,
    onNavigateBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: PerDiemItemFormViewModel = hiltViewModel(),
) {
    val haptics = rememberHaptics()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var isConfirmingDelete by rememberSaveable { mutableStateOf(false) }
    val isEnabled = !form.isSaving

    LaunchedEffect(houseId, configId) { viewModel.initialize(houseId, configId) }
    LaunchedEffect(Unit) {
        viewModel.done.collect {
            haptics.success()
            onDone()
        }
    }
    LaunchedEffect(form.error) {
        val message = form.error ?: return@LaunchedEffect
        haptics.error()
        snackbarHostState.showSnackbar(message)
        viewModel.dismissError()
    }

    Scaffold(
        topBar = { FlockrTopAppBar(title = if (form.isEditing) "Edit item" else "Add item", onNavigateBack = onNavigateBack) },
        bottomBar = {
            FlockrPrimaryButton(
                text = if (form.isEditing) "Save changes" else "Add item",
                onClick = { viewModel.save(houseId) },
                enabled = form.canSave,
                isLoading = form.isSaving,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (!form.isLoaded) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { LoadingIndicator() }
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            FlockrTextField(
                value = form.itemName,
                onValueChange = { name -> viewModel.update { it.copy(itemName = name) } },
                label = "Item",
                placeholder = "Milk, water can, newspaper…",
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
            AmountField(
                value = form.rate,
                onValueChange = { rate -> viewModel.update { it.copy(rate = rate) } },
                currencyCode = form.currencyCode,
                label = "Price per unit",
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
            FlockrTextField(
                value = form.unit,
                onValueChange = { unit -> viewModel.update { it.copy(unit = unit) } },
                label = "Unit",
                placeholder = "litre, can, packet…",
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
            ChoiceField(
                label = "Category",
                selected = form.category,
                options = PerDiemCategories.DEFAULT,
                onSelect = { category -> viewModel.update { it.copy(category = category) } },
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
            if (form.isEditing) {
                Text(
                    "A new price applies to usage logged from now on. What's already logged keeps its price.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = viewModel::archive, enabled = isEnabled, modifier = Modifier.fillMaxWidth()) {
                    Text("Archive: stop logging it, keep its history")
                }
                if (form.isAdmin) {
                    TextButton(onClick = { isConfirmingDelete = true }, enabled = isEnabled, modifier = Modifier.fillMaxWidth()) {
                        Text("Delete it and all its usage", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (isConfirmingDelete) {
        ConfirmDialog(
            title = "Delete ${form.itemName} and its usage?",
            message = "Every logged use of it goes too, in every month. Months already billed keep their bills.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                isConfirmingDelete = false
                viewModel.deleteWithUsage()
            },
            onDismiss = { isConfirmingDelete = false },
        )
    }
}
