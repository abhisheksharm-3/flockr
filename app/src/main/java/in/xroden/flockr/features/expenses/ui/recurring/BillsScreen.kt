/** A house's recurring bills, the ones needing payment first, with paying, editing and history. */
package `in`.xroden.flockr.features.expenses.ui.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.data.enums.ExpenseDueStatus
import `in`.xroden.flockr.data.enums.ExpenseFrequency
import `in`.xroden.flockr.features.expenses.data.recurringPaymentShares
import `in`.xroden.flockr.features.expenses.model.RecurringExpense
import `in`.xroden.flockr.features.expenses.presentation.BillsUiState
import `in`.xroden.flockr.features.expenses.presentation.BillsViewModel
import `in`.xroden.flockr.features.expenses.ui.categoryIcon
import `in`.xroden.flockr.features.house.model.nameOf
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.model.today
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.buttons.FlockrExtendedFab
import `in`.xroden.flockr.ui.components.dialogs.ConfirmDialog
import `in`.xroden.flockr.ui.components.inputs.AmountField
import `in`.xroden.flockr.ui.components.inputs.DatePickerField
import `in`.xroden.flockr.ui.components.states.EmptyState
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.ComponentHeight
import `in`.xroden.flockr.ui.theme.IconSize
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.formatMoney
import `in`.xroden.flockr.utils.formatWithHouseConfig
import `in`.xroden.flockr.utils.minorUnitDigits
import `in`.xroden.flockr.utils.parseMoney
import `in`.xroden.flockr.utils.rememberHaptics
import `in`.xroden.flockr.utils.toAmountInput
import java.math.BigDecimal
import kotlin.math.absoluteValue
import kotlinx.datetime.LocalDate

private val NEEDS_PAYING = setOf(ExpenseDueStatus.OVERDUE, ExpenseDueStatus.DUE_TODAY, ExpenseDueStatus.UPCOMING)

@Composable
fun BillsScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onAddBill: () -> Unit,
    onEditBill: (billId: String) -> Unit,
    onBillHistory: (billId: String) -> Unit,
    viewModel: BillsViewModel = hiltViewModel(),
) {
    val haptics = rememberHaptics()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val config by rememberHouseConfig(houseId)
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var payingBill by remember { mutableStateOf<RecurringExpense?>(null) }
    var deletingBill by remember { mutableStateOf<RecurringExpense?>(null) }

    LaunchedEffect(houseId) { viewModel.load(houseId) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event.isError) haptics.error() else haptics.success()
            snackbarHostState.showSnackbar(event.message)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { FlockrTopAppBar(title = "Bills", onNavigateBack = onNavigateBack, scrollBehavior = scrollBehavior) },
        floatingActionButton = { FlockrExtendedFab(text = "Add bill", icon = Icons.Rounded.Add, onClick = onAddBill) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val current = state) {
                BillsUiState.Loading -> LoadingIndicator(Modifier.align(Alignment.Center))
                is BillsUiState.Error -> ErrorState(current.message, onRetry = { viewModel.load(houseId) })
                is BillsUiState.Ready -> if (current.bills.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.EventRepeat,
                        title = "No bills yet",
                        subtitle = "Add rent, internet or anything that repeats. Flockr reminds everyone and splits each payment.",
                        actionText = "Add a bill",
                        onActionClick = onAddBill,
                    )
                } else {
                    BillList(
                        state = current,
                        config = config,
                        onPay = { payingBill = it },
                        onEdit = { onEditBill(it.id) },
                        onHistory = { onBillHistory(it.id) },
                        onDelete = { deletingBill = it },
                    )
                }
            }
        }
    }

    val ready = state as? BillsUiState.Ready
    payingBill?.let { bill ->
        if (ready != null) {
            PayBillDialog(
                bill = bill,
                state = ready,
                config = config,
                onConfirm = { amount, date ->
                    payingBill = null
                    viewModel.pay(bill, amount, date)
                },
                onDismiss = { payingBill = null },
            )
        }
    }
    deletingBill?.let { bill ->
        ConfirmDialog(
            title = "Delete ${bill.name}?",
            message = "It stops repeating and reminding. Payments already recorded stay in the ledger.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                deletingBill = null
                viewModel.delete(bill)
            },
            onDismiss = { deletingBill = null },
        )
    }
}

@Composable
private fun BillList(
    state: BillsUiState.Ready,
    config: HouseConfig?,
    onPay: (RecurringExpense) -> Unit,
    onEdit: (RecurringExpense) -> Unit,
    onHistory: (RecurringExpense) -> Unit,
    onDelete: (RecurringExpense) -> Unit,
) {
    val (due, later) = state.bills.partition { it.dueStatus in NEEDS_PAYING }
    LazyColumn(
        contentPadding = PaddingValues(start = Spacing.lg, end = Spacing.lg, top = Spacing.sm, bottom = Spacing.xxxxl * 2),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        listOf("Needs paying" to due, "Later" to later).forEach { (title, bills) ->
            if (bills.isEmpty()) return@forEach
            item(key = title) { Text(title, style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(bills, key = { it.id }) { bill ->
                BillCard(
                    bill = bill,
                    state = state,
                    config = config,
                    onPay = { onPay(bill) },
                    onEdit = { onEdit(bill) },
                    onHistory = { onHistory(bill) },
                    onDelete = { onDelete(bill) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun BillCard(
    bill: RecurringExpense,
    state: BillsUiState.Ready,
    config: HouseConfig?,
    onPay: () -> Unit,
    onEdit: () -> Unit,
    onHistory: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val isOverdue = bill.dueStatus == ExpenseDueStatus.OVERDUE
    val canPayNow = bill.allowPrepayment || bill.dueStatus in NEEDS_PAYING
    var isMenuOpen by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.largeIncreased,
        colors = CardDefaults.cardColors(
            containerColor = if (isOverdue) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Box(Modifier.size(ComponentHeight.avatar), contentAlignment = Alignment.Center) {
                        Icon(categoryIcon(bill.category), contentDescription = null, modifier = Modifier.size(IconSize.md))
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(bill.name, style = MaterialTheme.typography.titleMediumEmphasized)
                    Text(
                        "${bill.amount.formatMoney(state.currencyCode)} · ${frequencyLabel(bill)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { isMenuOpen = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "More for ${bill.name}") }
                    DropdownMenu(expanded = isMenuOpen, onDismissRequest = { isMenuOpen = false }) {
                        DropdownMenuItem(text = { Text("Payment history") }, onClick = { isMenuOpen = false; onHistory() })
                        DropdownMenuItem(text = { Text("Edit") }, onClick = { isMenuOpen = false; onEdit() })
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { isMenuOpen = false; onDelete() })
                    }
                }
            }
            Text(
                dueLabel(bill, config),
                style = MaterialTheme.typography.bodyMediumEmphasized,
                color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            Text(splitLabel(bill, state), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                FilledTonalButton(onClick = { haptics.tap(); onPay() }, enabled = canPayNow && state.payingBillId == null) {
                    Text(if (canPayNow) "Mark paid" else "Not due yet")
                }
            }
        }
    }
}

private fun frequencyLabel(bill: RecurringExpense): String =
    if (bill.frequency == ExpenseFrequency.CUSTOM) "every ${bill.customFrequencyDays} days" else bill.frequency.label.lowercase()

private fun dueLabel(bill: RecurringExpense, config: HouseConfig?): String {
    val date = bill.nextDueDate.formatWithHouseConfig(config)
    return when {
        bill.daysUntilDue < 0 -> "Overdue by ${bill.daysUntilDue.absoluteValue} day${if (bill.daysUntilDue == -1) "" else "s"} · was due $date"
        bill.daysUntilDue == 0 -> "Due today"
        bill.daysUntilDue == 1 -> "Due tomorrow"
        else -> "Due in ${bill.daysUntilDue} days · $date"
    }
}

private fun splitLabel(bill: RecurringExpense, state: BillsUiState.Ready): String {
    val method = bill.splitMethod ?: return "Not split: whoever pays bears it"
    val names = bill.shares.joinToString { state.members.nameOf(it.userId, state.viewerId) }
    return "Split ${method.label.lowercase()} between $names"
}

/** The amount this time, which can differ for metered bills, the date, and what each person will owe. */
@Composable
private fun PayBillDialog(
    bill: RecurringExpense,
    state: BillsUiState.Ready,
    config: HouseConfig?,
    onConfirm: (BigDecimal, LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val haptics = rememberHaptics()
    var amountText by rememberSaveable { mutableStateOf(bill.amount.toAmountInput(state.currencyCode)) }
    var date by remember { mutableStateOf(config.today()) }
    val amount = parseMoney(amountText, state.currencyCode)?.takeIf { it.signum() > 0 }
    val shares = amount?.let { recurringPaymentShares(bill, it, state.viewerId, minorUnitDigits(state.currencyCode)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pay ${bill.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                AmountField(value = amountText, onValueChange = { amountText = it }, currencyCode = state.currencyCode, modifier = Modifier.fillMaxWidth())
                DatePickerField(label = "Paid on", date = date, houseConfig = config, onDateChange = { date = it })
                shares?.filter { it.owedShare.signum() > 0 }?.forEach { share ->
                    Text(
                        "${state.members.nameOf(share.userId, state.viewerId)}: ${share.owedShare.formatMoney(state.currencyCode)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (amount != null && shares == null) {
                    Text("This amount can't be split the way the bill is.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { haptics.tap(); amount?.let { onConfirm(it, date) } }, enabled = shares != null) { Text("Record payment") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
