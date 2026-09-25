/** A month of per-diem usage: what it came to per person, billing it, the items, and the usage log. */
package `in`.xroden.flockr.features.expenses.ui.perdiem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.expenses.model.PerDiemConfig
import `in`.xroden.flockr.features.expenses.model.PerDiemEntryWithDetails
import `in`.xroden.flockr.features.expenses.presentation.PerDiemUiState
import `in`.xroden.flockr.features.expenses.presentation.PerDiemViewModel
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.model.currency
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.buttons.FlockrExtendedFab
import `in`.xroden.flockr.ui.components.cards.SectionCard
import `in`.xroden.flockr.ui.components.dialogs.ConfirmDialog
import `in`.xroden.flockr.ui.components.inputs.DatePickerField
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.ui.components.inputs.MonthSelector
import `in`.xroden.flockr.ui.components.states.EmptyState
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.formatMoney
import `in`.xroden.flockr.utils.formatWithHouseConfig
import `in`.xroden.flockr.utils.monthYearLabel
import `in`.xroden.flockr.utils.parseDecimal
import `in`.xroden.flockr.utils.rememberHaptics
import java.math.BigDecimal
import kotlinx.datetime.LocalDate

/** Where the usage screen leads. */
data class PerDiemNavigation(
    val back: () -> Unit,
    val logUsage: (configId: String?) -> Unit,
    val addItem: () -> Unit,
    val editItem: (configId: String) -> Unit,
    val openExpense: (expenseId: String) -> Unit,
)

@Composable
fun PerDiemScreen(houseId: String, navigation: PerDiemNavigation, viewModel: PerDiemViewModel = hiltViewModel()) {
    val haptics = rememberHaptics()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val month by viewModel.month.collectAsStateWithLifecycle()
    val config by rememberHouseConfig(houseId)
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var editingEntry by remember { mutableStateOf<PerDiemEntryWithDetails?>(null) }
    var isConfirmingBill by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(houseId) { viewModel.load(houseId) }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { notice ->
            if (notice.isError) haptics.error() else haptics.success()
            snackbarHostState.showSnackbar(notice.message)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { FlockrTopAppBar(title = "Usage", onNavigateBack = navigation.back, scrollBehavior = scrollBehavior) },
        floatingActionButton = { FlockrExtendedFab(text = "Log usage", icon = Icons.Rounded.Add, onClick = { navigation.logUsage(null) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            month?.let { selected ->
                MonthSelector(
                    selectedMonth = selected,
                    onMonthChange = viewModel::onMonthChange,
                    timezone = config?.timezone,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                )
            }
            Box(Modifier.fillMaxSize()) {
                when (val current = state) {
                    PerDiemUiState.Loading -> LoadingIndicator(Modifier.align(Alignment.Center))
                    is PerDiemUiState.Error -> ErrorState(current.message, onRetry = { viewModel.load(houseId) })
                    is PerDiemUiState.Ready -> PerDiemContent(
                        state = current,
                        config = config,
                        navigation = navigation,
                        onBill = { isConfirmingBill = true },
                        onEditEntry = { editingEntry = it },
                    )
                }
            }
        }
    }

    val ready = state as? PerDiemUiState.Ready
    if (isConfirmingBill && ready != null) {
        ConfirmDialog(
            title = "Bill ${month?.monthYearLabel().orEmpty()}?",
            message = "Record that you paid ${ready.data.total.formatMoney(config.currency())} for this month's usage. " +
                "Each person will owe you what they used, and the month's log will be locked.",
            confirmText = "Bill it",
            onConfirm = {
                isConfirmingBill = false
                viewModel.billMonth()
            },
            onDismiss = { isConfirmingBill = false },
        )
    }
    editingEntry?.let { entry ->
        EditEntryDialog(
            entry = entry,
            config = config,
            onSave = { quantity, date, notes ->
                editingEntry = null
                viewModel.updateEntry(entry, quantity, date, notes)
            },
            onDelete = {
                editingEntry = null
                viewModel.deleteEntry(entry)
            },
            onDismiss = { editingEntry = null },
        )
    }
}

@Composable
private fun PerDiemContent(
    state: PerDiemUiState.Ready,
    config: HouseConfig?,
    navigation: PerDiemNavigation,
    onBill: () -> Unit,
    onEditEntry: (PerDiemEntryWithDetails) -> Unit,
) {
    val currencyCode = config.currency()
    val data = state.data
    LazyColumn(
        contentPadding = PaddingValues(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.xxxxl * 2),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        item(key = "total") {
            SectionCard(title = data.total.formatMoney(currencyCode), subtitle = "Used this month") {
                data.byMember.forEach { member ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (member.userId == state.viewerId) "You" else member.fullName, style = MaterialTheme.typography.bodyLarge)
                        Text(member.totalCost.formatMoney(currencyCode), style = MaterialTheme.typography.bodyLargeEmphasized)
                    }
                }
                val bill = data.usageBill
                when {
                    bill != null -> OutlinedButton(onClick = { navigation.openExpense(bill.id) }) { Text("Billed · view the expense") }
                    data.total.signum() > 0 -> Button(onClick = onBill, enabled = !state.isBilling) { Text("Bill this month") }
                }
            }
        }
        item(key = "items") {
            SectionCard(
                title = "Items",
                action = { TextButton(onClick = navigation.addItem) { Text("Add item") } },
            ) {
                if (data.items.isEmpty()) {
                    Text("Add what the house buys by use, like milk or water cans, with its price per unit.", style = MaterialTheme.typography.bodyMedium)
                }
                data.items.forEach { item -> ItemLine(item, currencyCode, onLog = { navigation.logUsage(item.id) }, onEdit = { navigation.editItem(item.id) }) }
            }
        }
        if (data.entries.isEmpty() && data.items.isNotEmpty()) {
            item(key = "empty") {
                EmptyState(icon = Icons.Rounded.WaterDrop, title = "No usage this month", subtitle = "Log what you use and it adds up here.")
            }
        }
        if (data.entries.isNotEmpty()) {
            item(key = "log_title") { Text("Log", style = MaterialTheme.typography.titleMediumEmphasized) }
        }
        items(data.entries, key = { it.id }) { entry ->
            EntryLine(entry, state.viewerId, config, isLocked = data.usageBill != null, onClick = { onEditEntry(entry) })
        }
    }
}

@Composable
private fun ItemLine(item: PerDiemConfig, currencyCode: String, onLog: () -> Unit, onEdit: () -> Unit) {
    val haptics = rememberHaptics()
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.itemName, style = MaterialTheme.typography.bodyLargeEmphasized)
            Text("${item.rate.formatMoney(currencyCode)} per ${item.unit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FilledTonalButton(onClick = { haptics.tap(); onLog() }) { Text("Log") }
    }
}

@Composable
private fun EntryLine(entry: PerDiemEntryWithDetails, viewerId: String, config: HouseConfig?, isLocked: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !isLocked, onClick = onClick).padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("${entry.quantity.stripTrailingZeros().toPlainString()} ${entry.unit} ${entry.itemName}", style = MaterialTheme.typography.bodyLarge)
            Text(
                "${if (entry.addedBy == viewerId) "You" else entry.addedByName} · ${entry.date.formatWithHouseConfig(config)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(entry.totalCost.formatMoney(config.currency()), style = MaterialTheme.typography.bodyMediumEmphasized)
    }
}

/** Changing or deleting a logged use. The price stays what it was when the use was logged. */
@Composable
private fun EditEntryDialog(
    entry: PerDiemEntryWithDetails,
    config: HouseConfig?,
    onSave: (BigDecimal, LocalDate, String?) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var quantityText by rememberSaveable { mutableStateOf(entry.quantity.stripTrailingZeros().toPlainString()) }
    var date by remember { mutableStateOf(entry.date) }
    var notes by rememberSaveable { mutableStateOf(entry.notes.orEmpty()) }
    val quantity = parseDecimal(quantityText)?.takeIf { it.signum() > 0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.itemName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                FlockrTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = "Quantity",
                    suffix = entry.unit,
                    keyboardType = KeyboardType.Decimal,
                    isError = quantityText.isNotEmpty() && quantity == null,
                    modifier = Modifier.fillMaxWidth(),
                )
                DatePickerField(label = "Date", date = date, houseConfig = config, onDateChange = { date = it })
                FlockrTextField(value = notes, onValueChange = { notes = it }, label = "Notes", modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { quantity?.let { onSave(it, date, notes.ifBlank { null }) } }, enabled = quantity != null) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
