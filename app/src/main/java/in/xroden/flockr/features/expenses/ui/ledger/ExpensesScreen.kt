/** The expenses hub: where you stand, the payments that would settle you up, and the house's activity by month. */
package `in`.xroden.flockr.features.expenses.ui.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Scale
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.expenses.model.SettleUpPayment
import `in`.xroden.flockr.features.expenses.presentation.ExpensesUiState
import `in`.xroden.flockr.features.expenses.presentation.ExpensesViewModel
import `in`.xroden.flockr.features.house.model.currency
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.MemberAvatar
import `in`.xroden.flockr.ui.components.buttons.FabAction
import `in`.xroden.flockr.ui.components.buttons.FlockrFabMenu
import `in`.xroden.flockr.ui.components.states.EmptyState
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.formatMoney
import `in`.xroden.flockr.utils.monthYearLabel
import `in`.xroden.flockr.utils.rememberHaptics
import java.math.BigDecimal

/** Where the hub leads. Each is a navigation, so none carries a haptic of its own. */
data class ExpensesNavigation(
    val back: () -> Unit,
    val addExpense: () -> Unit,
    val openExpense: (expenseId: String) -> Unit,
    val settleUp: (fromUserId: String?, toUserId: String?, amount: BigDecimal?) -> Unit,
    val balances: () -> Unit,
    val bills: () -> Unit,
    val perDiem: () -> Unit,
    val reports: () -> Unit,
)

@Composable
fun ExpensesScreen(houseId: String, navigation: ExpensesNavigation, viewModel: ExpensesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val config by rememberHouseConfig(houseId)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(houseId) { viewModel.load(houseId) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { FlockrTopAppBar(title = "Expenses", onNavigateBack = navigation.back, scrollBehavior = scrollBehavior) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val current = state) {
                ExpensesUiState.Loading -> LoadingIndicator(Modifier.align(Alignment.Center))
                is ExpensesUiState.Error -> ErrorState(message = current.message, onRetry = { viewModel.load(houseId) })
                is ExpensesUiState.Ready -> ExpensesContent(current, config.currency(), navigation)
            }
            FlockrFabMenu(
                actions = listOf(
                    FabAction("Add expense", Icons.Rounded.ReceiptLong, navigation.addExpense),
                    FabAction("Record a payment", Icons.Rounded.Handshake) { navigation.settleUp(null, null, null) },
                ),
                contentDescription = "Add",
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}

@Composable
private fun ExpensesContent(state: ExpensesUiState.Ready, currencyCode: String, navigation: ExpensesNavigation) {
    val byMonth = state.expenses.groupBy { it.date.year to it.date.month }
    LazyColumn(contentPadding = PaddingValues(bottom = Spacing.xxxxl * 2)) {
        item(key = "summary") {
            StandingCard(
                net = state.standing.netOf(state.viewerId),
                currencyCode = currencyCode,
                onViewBalances = navigation.balances,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            )
        }
        items(state.standing.paymentsOf(state.viewerId), key = { "pay_${it.fromUserId}_${it.toUserId}" }) { payment ->
            SuggestedPayment(payment, state, currencyCode, onSettle = { navigation.settleUp(payment.fromUserId, payment.toUserId, payment.amount) })
        }
        item(key = "shortcuts") { Shortcuts(navigation) }
        if (state.expenses.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Rounded.ReceiptLong,
                    title = "No expenses yet",
                    subtitle = "Add what you spend for the house and Flockr works out who owes whom.",
                    actionText = "Add an expense",
                    onActionClick = navigation.addExpense,
                )
            }
        }
        byMonth.forEach { (month, expenses) ->
            item(key = "month_${month.first}_${month.second}") {
                Text(
                    expenses.first().date.monthYearLabel(),
                    style = MaterialTheme.typography.titleSmallEmphasized,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.xl, bottom = Spacing.xs),
                )
            }
            items(expenses, key = { it.id }) { expense ->
                ExpenseRow(
                    expense = expense,
                    members = state.members,
                    viewerId = state.viewerId,
                    currencyCode = currencyCode,
                    onClick = { navigation.openExpense(expense.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

/** The viewer's overall balance in the house, in words and colour. */
@Composable
private fun StandingCard(net: BigDecimal, currencyCode: String, onViewBalances: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            val (label, amount) = when (net.signum()) {
                1 -> "You are owed" to net
                -1 -> "You owe" to net.negate()
                else -> "You're all settled up" to null
            }
            Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            amount?.let {
                Text(it.formatMoney(currencyCode), style = MaterialTheme.typography.displaySmallEmphasized, color = balanceColor(net))
            }
            TextButton(onClick = onViewBalances, contentPadding = PaddingValues(0.dp)) {
                Text("See everyone's balance")
            }
        }
    }
}

/** A payment from the settle-up plan that involves the viewer, with a button to record it. */
@Composable
private fun SuggestedPayment(payment: SettleUpPayment, state: ExpensesUiState.Ready, currencyCode: String, onSettle: () -> Unit) {
    val haptics = rememberHaptics()
    val isViewerPaying = payment.fromUserId == state.viewerId
    val other = state.members[if (isViewerPaying) payment.toUserId else payment.fromUserId]
    val otherName = other?.displayName ?: "A former housemate"
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        MemberAvatar(name = otherName, avatarUrl = other?.avatarUrl)
        Column(Modifier.weight(1f)) {
            Text(
                if (isViewerPaying) "You owe $otherName" else "$otherName owes you",
                style = MaterialTheme.typography.bodyLargeEmphasized,
            )
            Text(payment.amount.formatMoney(currencyCode), style = MaterialTheme.typography.bodyMedium, color = if (isViewerPaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }
        FilledTonalButton(onClick = { haptics.tap(); onSettle() }) {
            Text(if (isViewerPaying) "Settle up" else "Record")
        }
    }
}

@Composable
private fun Shortcuts(navigation: ExpensesNavigation) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item { ShortcutChip("Balances", Icons.Rounded.Scale, navigation.balances) }
        item { ShortcutChip("Bills", Icons.Rounded.EventRepeat, navigation.bills) }
        item { ShortcutChip("Usage", Icons.Rounded.People, navigation.perDiem) }
        item { ShortcutChip("Reports", Icons.Rounded.BarChart, navigation.reports) }
    }
}

@Composable
private fun ShortcutChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(label) }, leadingIcon = { Icon(icon, contentDescription = null) })
}
