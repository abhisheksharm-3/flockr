/** Logging a use of a per-diem item, with what it will cost. */
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.expenses.presentation.PerDiemEntryFormViewModel
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.buttons.FlockrPrimaryButton
import `in`.xroden.flockr.ui.components.inputs.ChoiceField
import `in`.xroden.flockr.ui.components.inputs.DatePickerField
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.formatMoney
import `in`.xroden.flockr.utils.rememberHaptics

@Composable
fun PerDiemEntryFormScreen(
    houseId: String,
    configId: String?,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    onAddItem: () -> Unit,
    viewModel: PerDiemEntryFormViewModel = hiltViewModel(),
) {
    val haptics = rememberHaptics()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val config by rememberHouseConfig(houseId)
    val snackbarHostState = remember { SnackbarHostState() }
    val isEnabled = !form.isSaving

    LaunchedEffect(houseId, configId) { viewModel.initialize(houseId, configId) }
    LaunchedEffect(Unit) {
        viewModel.saved.collect {
            haptics.success()
            onSaved()
        }
    }
    LaunchedEffect(form.error) {
        val message = form.error ?: return@LaunchedEffect
        haptics.error()
        snackbarHostState.showSnackbar(message)
        viewModel.dismissError()
    }

    Scaffold(
        topBar = { FlockrTopAppBar(title = "Log usage", onNavigateBack = onNavigateBack) },
        bottomBar = {
            FlockrPrimaryButton(
                text = form.cost?.let { "Log ${it.formatMoney(form.currencyCode)}" } ?: "Log",
                onClick = viewModel::save,
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
            if (form.items.isEmpty()) {
                Text("Add an item first, with its price per unit.", style = MaterialTheme.typography.bodyLarge)
                FlockrPrimaryButton(text = "Add an item", onClick = onAddItem)
                return@Column
            }
            ChoiceField(
                label = "Item",
                selected = form.item,
                options = form.items,
                onSelect = { item -> item?.let { chosen -> viewModel.update { it.copy(configId = chosen.id) } } },
                optionLabel = { item -> item?.let { "${it.itemName} · ${it.rate.formatMoney(form.currencyCode)} per ${it.unit}" }.orEmpty() },
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
            FlockrTextField(
                value = form.quantity,
                onValueChange = { quantity -> viewModel.update { it.copy(quantity = quantity) } },
                label = "Quantity",
                suffix = form.item?.unit,
                keyboardType = KeyboardType.Decimal,
                isError = form.quantity.isNotEmpty() && form.parsedQuantity == null,
                supportingText = "Up to three decimal places",
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
            form.date?.let { date ->
                DatePickerField(label = "Date", date = date, houseConfig = config, onDateChange = { day -> viewModel.update { it.copy(date = day) } }, enabled = isEnabled)
            }
            FlockrTextField(
                value = form.notes,
                onValueChange = { notes -> viewModel.update { it.copy(notes = notes) } },
                label = "Notes",
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
