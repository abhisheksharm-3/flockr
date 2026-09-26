/** A house's recurring bills: what is due on cobalt, the ones needing payment first, with paying, editing and history. */
package `in`.xroden.flockr.features.expenses.ui.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.data.enums.ExpenseDueStatus
import `in`.xroden.flockr.data.enums.ExpenseFrequency
import `in`.xroden.flockr.features.expenses.data.recurringPaymentShares
import `in`.xroden.flockr.features.expenses.model.RecurringExpense
import `in`.xroden.flockr.features.expenses.presentation.BillsUiState
import `in`.xroden.flockr.features.expenses.presentation.BillsViewModel
import `in`.xroden.flockr.features.expenses.ui.categoryIcon
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.model.nameOf
import `in`.xroden.flockr.features.house.model.today
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.SkeletonHeroScreen
import `in`.xroden.flockr.ui.components.BadgeTone
import `in`.xroden.flockr.ui.components.HeroActions
import `in`.xroden.flockr.ui.components.HeroBadge
import `in`.xroden.flockr.ui.components.HeroButton
import `in`.xroden.flockr.ui.components.HeroCaption
import `in`.xroden.flockr.ui.components.HeroCountUp
import `in`.xroden.flockr.ui.components.HeroHeader
import `in`.xroden.flockr.ui.components.HeroLabel
import `in`.xroden.flockr.ui.components.HeroStatusBarScrim
import `in`.xroden.flockr.ui.components.IconBadge
import `in`.xroden.flockr.ui.components.ListRow
import `in`.xroden.flockr.ui.components.MemberAvatar
import `in`.xroden.flockr.ui.components.SectionTitle
import `in`.xroden.flockr.ui.components.TrailingAmount
import `in`.xroden.flockr.ui.components.buttons.FlockrExtendedFab
import `in`.xroden.flockr.ui.components.dialogs.ConfirmDialog
import `in`.xroden.flockr.ui.components.forms.FormSubmitBar
import `in`.xroden.flockr.ui.components.forms.Sentence
import `in`.xroden.flockr.ui.components.forms.SentenceToken
import `in`.xroden.flockr.ui.components.forms.SentenceWords
import `in`.xroden.flockr.ui.components.inputs.AmountField
import `in`.xroden.flockr.ui.components.inputs.FlockrDatePickerDialog
import `in`.xroden.flockr.ui.components.isHeroScrolledAway
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.dueLabel
import `in`.xroden.flockr.utils.formatMoney
import `in`.xroden.flockr.utils.formatWithHouseConfig
import `in`.xroden.flockr.utils.minorUnitDigits
import `in`.xroden.flockr.utils.parseMoney
import `in`.xroden.flockr.utils.relativeDayLabel
import `in`.xroden.flockr.utils.rememberHaptics
import `in`.xroden.flockr.utils.toAmountInput
import java.math.BigDecimal
import kotlinx.coroutines.launch
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
    val listState = rememberLazyListState()
    var openBill by remember { mutableStateOf<RecurringExpense?>(null) }
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
        floatingActionButton = { FlockrExtendedFab(text = "Add bill", icon = Icons.Rounded.Add, onClick = onAddBill) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            when (val current = state) {
                BillsUiState.Loading -> SkeletonHeroScreen()
                is BillsUiState.Error -> Box(Modifier.fillMaxSize().padding(padding)) {
                    ErrorState(current.message, onRetry = { viewModel.load(houseId) })
                }
                is BillsUiState.Ready -> {
                    BillList(state = current, config = config, listState = listState, onPay = { payingBill = it }, onOpen = { openBill = it })
                    HeroStatusBarScrim(isHeroGone = listState.isHeroScrolledAway)
                }
            }
        }
    }

    val ready = state as? BillsUiState.Ready
    openBill?.let { bill ->
        if (ready != null) {
            BillSheet(
                bill = bill,
                state = ready,
                config = config,
                onPay = { payingBill = bill },
                onHistory = { onBillHistory(bill.id) },
                onEdit = { onEditBill(bill.id) },
                onDelete = { deletingBill = bill },
                onDismiss = { openBill = null },
            )
        }
    }
    payingBill?.let { bill ->
        if (ready != null) {
            PayBillSheet(
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
    listState: LazyListState,
    onPay: (RecurringExpense) -> Unit,
    onOpen: (RecurringExpense) -> Unit,
) {
    val (due, later) = state.bills.partition { it.dueStatus in NEEDS_PAYING }
    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = Spacing.xxxxl * 2)) {
        item(key = "hero") { DueHero(state, due, onPay) }
        listOf(
            Triple("due", "Needs paying", due),
            Triple("later", "Later", later),
        ).forEach { (key, title, bills) ->
            if (bills.isEmpty()) return@forEach
            item(key = "title_$key") { SectionTitle(title) }
            items(bills, key = { it.id }) { bill ->
                BillRow(
                    bill = bill,
                    state = state,
                    config = config,
                    onPay = { onPay(bill) },
                    onOpen = { onOpen(bill) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

/**
 * The cobalt headline: what the bills needing payment add up to at their usual amounts, the most
 * pressing one in words, and paying it in one tap. With nothing due it says what comes next.
 */
@Composable
private fun DueHero(state: BillsUiState.Ready, due: List<RecurringExpense>, onPay: (RecurringExpense) -> Unit) {
    HeroHeader(title = "Bills") {
        val first = due.minByOrNull { it.daysUntilDue }
        when {
            state.bills.isEmpty() -> {
                HeroLabel("No bills yet")
                HeroCaption("Add rent, internet or anything that repeats. Flockr reminds everyone and splits each payment.")
            }
            first == null -> {
                HeroLabel("Nothing due right now")
                HeroBadge("All paid")
                state.bills.minByOrNull { it.daysUntilDue }?.let { HeroCaption("Next up is ${it.name}, ${dueLabel(it.daysUntilDue).lowercase()}.") }
            }
            else -> {
                HeroLabel(if (due.size == 1) "Due soon" else "${due.size} bills due soon")
                HeroCountUp(due.fold(BigDecimal.ZERO) { sum, bill -> sum + bill.amount }, state.currencyCode)
                HeroCaption("${first.name} is ${dueLabel(first.daysUntilDue).lowercase()}.")
                HeroActions { HeroButton("Pay ${first.name}", onClick = { onPay(first) }, enabled = state.payingBillId == null) }
            }
        }
    }
}

/**
 * A bill as its name over two quiet lines, the amount and how often, then when it falls due. One
 * waiting on the house carries its Pay button, so paying is one tap from the list.
 */
@Composable
private fun BillRow(
    bill: RecurringExpense,
    state: BillsUiState.Ready,
    config: HouseConfig?,
    onPay: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    ListRow(
        headline = bill.name,
        supporting = "${bill.amount.formatMoney(state.currencyCode)}, ${frequencyLabel(bill)}\n${dueLine(bill, config)}",
        leading = { IconBadge(categoryIcon(bill.category), billTone(bill.dueStatus)) },
        trailing = if (bill.dueStatus in NEEDS_PAYING) {
            {
                FilledTonalButton(onClick = { haptics.tap(); onPay() }, enabled = state.payingBillId == null, shapes = ButtonDefaults.shapes()) {
                    Text("Pay")
                }
            }
        } else {
            null
        },
        onClick = onOpen,
        modifier = modifier,
    )
}

/** Everything about one bill, and what can be done with it. Actions close the sheet before they run. */
@Composable
private fun BillSheet(
    bill: RecurringExpense,
    state: BillsUiState.Ready,
    config: HouseConfig?,
    onPay: () -> Unit,
    onHistory: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val canPayNow = bill.allowPrepayment || bill.dueStatus in NEEDS_PAYING
    val closeThen: (() -> Unit) -> () -> Unit = { action ->
        { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss(); action() } }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(bill.name, style = MaterialTheme.typography.headlineSmallEmphasized)
            Text(
                "${bill.amount.formatMoney(state.currencyCode)}, ${frequencyLabel(bill)}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                dueLine(bill, config),
                style = MaterialTheme.typography.bodyMediumEmphasized,
                color = if (bill.dueStatus == ExpenseDueStatus.OVERDUE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(splitLabel(bill, state), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(reminderLabel(bill), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(Spacing.md))
        ListRow(
            headline = if (bill.dueStatus in NEEDS_PAYING) "Record a payment" else "Pay early",
            supporting = if (canPayNow) null else "You can pay it from its reminder day on",
            leading = { IconBadge(Icons.Rounded.Payments, if (canPayNow) BadgeTone.SUN else BadgeTone.SLATE) },
            onClick = if (canPayNow && state.payingBillId == null) closeThen(onPay) else null,
            headlineColor = if (canPayNow) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ListRow(headline = "Payment history", leading = { IconBadge(Icons.Rounded.History, BadgeTone.SLATE) }, onClick = closeThen(onHistory))
        ListRow(headline = "Edit bill", leading = { IconBadge(Icons.Rounded.Edit, BadgeTone.SLATE) }, onClick = closeThen(onEdit))
        ListRow(
            headline = "Delete bill",
            leading = { IconBadge(Icons.Rounded.Delete, BadgeTone.ROSE) },
            onClick = closeThen(onDelete),
            headlineColor = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(Spacing.lg))
    }
}

private fun billTone(status: ExpenseDueStatus): BadgeTone = when (status) {
    ExpenseDueStatus.OVERDUE -> BadgeTone.ROSE
    ExpenseDueStatus.DUE_TODAY, ExpenseDueStatus.UPCOMING -> BadgeTone.SUN
    ExpenseDueStatus.SCHEDULED -> BadgeTone.SLATE
}

private fun frequencyLabel(bill: RecurringExpense): String =
    if (bill.frequency == ExpenseFrequency.CUSTOM) "every ${bill.customFrequencyDays} days" else bill.frequency.label.lowercase()

/** "Due tomorrow" on its own when it is that close; otherwise the relative wording with the date after it. */
private fun dueLine(bill: RecurringExpense, config: HouseConfig?): String {
    val label = dueLabel(bill.daysUntilDue)
    return if (bill.daysUntilDue in 0..1) label else "$label · ${bill.nextDueDate.formatWithHouseConfig(config)}"
}

private fun splitLabel(bill: RecurringExpense, state: BillsUiState.Ready): String {
    val method = bill.splitMethod ?: return "Not split: whoever pays bears it"
    val names = bill.shares.joinToString { state.members.nameOf(it.userId, state.viewerId) }
    return "Split ${method.phrase} between $names"
}

private fun reminderLabel(bill: RecurringExpense): String = when {
    !bill.reminderEnabled -> "No reminder"
    bill.reminderDaysBefore == 0 -> "Reminds the house on the day"
    bill.reminderDaysBefore == 1 -> "Reminds the house a day before"
    else -> "Reminds the house ${bill.reminderDaysBefore} days before"
}

/**
 * Paying a bill, as a sheet: the amount this time, which can differ for metered bills, then "Paid by
 * you on **today**", and exactly what each person will owe.
 */
@Composable
private fun PayBillSheet(
    bill: RecurringExpense,
    state: BillsUiState.Ready,
    config: HouseConfig?,
    onConfirm: (BigDecimal, LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText by rememberSaveable { mutableStateOf(bill.amount.toAmountInput(state.currencyCode)) }
    var date by remember { mutableStateOf(config.today()) }
    var isPickingDate by remember { mutableStateOf(false) }
    val amount = parseMoney(amountText, state.currencyCode)?.takeIf { it.signum() > 0 }
    val shares = amount?.let { recurringPaymentShares(bill, it, state.viewerId, minorUnitDigits(state.currencyCode)) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            Text("Pay ${bill.name}", style = MaterialTheme.typography.titleLargeEmphasized, modifier = Modifier.padding(horizontal = Spacing.lg))
            AmountField(
                value = amountText,
                onValueChange = { amountText = it },
                currencyCode = state.currencyCode,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
            )
            Sentence {
                SentenceWords("Paid by you on")
                SentenceToken(date.relativeDayLabel(config), onClick = { isPickingDate = true }, icon = Icons.Rounded.CalendarMonth)
            }
            val owing = shares?.filter { it.owedShare.signum() > 0 }.orEmpty()
            if (owing.isNotEmpty()) {
                Column {
                    SectionTitle("Each person's share")
                    owing.forEach { share ->
                        val member = state.members[share.userId]
                        val name = state.members.nameOf(share.userId, state.viewerId)
                        ListRow(
                            headline = name,
                            leading = { MemberAvatar(name = member?.displayName ?: name, avatarUrl = member?.avatarUrl) },
                            trailing = { TrailingAmount(share.owedShare.formatMoney(state.currencyCode)) },
                        )
                    }
                }
            }
            if (amount != null && shares == null) {
                Text(
                    "This amount can't be split the way the bill is.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }
            FormSubmitBar(
                text = "Record payment",
                onClick = { amount?.let { onConfirm(it, date) } },
                enabled = shares != null,
                isLoading = false,
            )
        }
    }
    if (isPickingDate) {
        FlockrDatePickerDialog(
            initialDate = date,
            firstDayOfWeek = config?.firstDayOfWeek,
            onDateSelected = { date = it; isPickingDate = false },
            onDismiss = { isPickingDate = false },
        )
    }
}
