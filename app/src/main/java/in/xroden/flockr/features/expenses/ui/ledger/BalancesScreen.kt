/** Everyone's balance, the fewest payments that settle the house, and what any two people have shared. */
package `in`.xroden.flockr.features.expenses.ui.ledger

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.expenses.model.ExpenseKind
import `in`.xroden.flockr.features.expenses.model.MemberBalance
import `in`.xroden.flockr.features.expenses.model.SettleUpPayment
import `in`.xroden.flockr.features.expenses.model.SharedHistoryEntry
import `in`.xroden.flockr.features.expenses.presentation.BalancesUiState
import `in`.xroden.flockr.features.expenses.presentation.BalancesViewModel
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.model.currency
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.MemberAvatar
import `in`.xroden.flockr.ui.components.cards.SectionCard
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.formatMoney
import `in`.xroden.flockr.utils.formatWithHouseConfig
import `in`.xroden.flockr.utils.rememberHaptics
import java.math.BigDecimal

@Composable
fun BalancesScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onSettleUp: (fromUserId: String, toUserId: String, amount: BigDecimal) -> Unit,
    onOpenExpense: (expenseId: String) -> Unit,
    viewModel: BalancesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val config by rememberHouseConfig(houseId)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(houseId) { viewModel.load(houseId) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { FlockrTopAppBar(title = "Balances", onNavigateBack = onNavigateBack, scrollBehavior = scrollBehavior) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val current = state) {
                BalancesUiState.Loading -> LoadingIndicator(Modifier.align(Alignment.Center))
                is BalancesUiState.Error -> ErrorState(current.message, onRetry = { viewModel.load(houseId) })
                is BalancesUiState.Ready -> BalancesContent(
                    state = current,
                    config = config,
                    onSettleUp = onSettleUp,
                    onOpenExpense = onOpenExpense,
                    onExpandMember = { viewModel.loadHistory(houseId, it) },
                )
            }
        }
    }
}

@Composable
private fun BalancesContent(
    state: BalancesUiState.Ready,
    config: HouseConfig?,
    onSettleUp: (String, String, BigDecimal) -> Unit,
    onOpenExpense: (String) -> Unit,
    onExpandMember: (String) -> Unit,
) {
    val currencyCode = config.currency()
    var expandedUserId by rememberSaveable { mutableStateOf<String?>(null) }
    LazyColumn(contentPadding = PaddingValues(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        item(key = "plan") {
            SectionCard(
                title = "Settle up",
                subtitle = "The fewest payments that square everyone, whatever order the expenses came in",
            ) {
                if (state.standing.plan.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Icon(Icons.Rounded.Celebration, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Everyone is settled up.", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                state.standing.plan.forEach { payment -> PlanLine(payment, state, currencyCode, onSettleUp) }
            }
        }
        item(key = "balances_title") {
            Text("Everyone's balance", style = MaterialTheme.typography.titleMediumEmphasized)
        }
        items(state.standing.balances, key = { it.userId }) { balance ->
            BalanceLine(
                balance = balance,
                state = state,
                config = config,
                isExpanded = expandedUserId == balance.userId,
                onToggle = {
                    expandedUserId = if (expandedUserId == balance.userId) null else balance.userId
                    onExpandMember(balance.userId)
                },
                onOpenExpense = onOpenExpense,
            )
        }
    }
}

/** "Karan pays Riya ₹200", with a button when the viewer is one of the two. */
@Composable
private fun PlanLine(payment: SettleUpPayment, state: BalancesUiState.Ready, currencyCode: String, onSettleUp: (String, String, BigDecimal) -> Unit) {
    val haptics = rememberHaptics()
    val from = state.members.nameOf(payment.fromUserId, state.viewerId)
    val to = state.members.nameOf(payment.toUserId, state.viewerId).let { if (payment.toUserId == state.viewerId) "you" else it }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text("$from ${if (payment.fromUserId == state.viewerId) "pay" else "pays"} $to", style = MaterialTheme.typography.bodyLargeEmphasized)
            Text(payment.amount.formatMoney(currencyCode), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.viewerId == payment.fromUserId || state.viewerId == payment.toUserId) {
            FilledTonalButton(onClick = { haptics.tap(); onSettleUp(payment.fromUserId, payment.toUserId, payment.amount) }) {
                Text(if (state.viewerId == payment.fromUserId) "Settle up" else "Record")
            }
        }
    }
}

/** A member's net balance; opening it shows what they and the viewer have shared. */
@Composable
private fun BalanceLine(
    balance: MemberBalance,
    state: BalancesUiState.Ready,
    config: HouseConfig?,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onOpenExpense: (String) -> Unit,
) {
    val currencyCode = config.currency()
    val member = state.members[balance.userId]
    val name = state.members.nameOf(balance.userId, state.viewerId)
    val canExpand = balance.userId != state.viewerId
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(enabled = canExpand, onClick = onToggle).padding(vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            MemberAvatar(name = member?.displayName ?: balance.fullName, avatarUrl = member?.avatarUrl)
            Column(Modifier.weight(1f)) {
                Text(if (balance.isActive) name else "$name (left)", style = MaterialTheme.typography.bodyLargeEmphasized)
                Text(
                    "paid ${balance.paid.formatMoney(currencyCode)} · share ${balance.owed.formatMoney(currencyCode)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    when (balance.net.signum()) {
                        1 -> "gets back"
                        -1 -> "owes"
                        else -> "settled up"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = balanceColor(balance.net),
                )
                if (balance.net.signum() != 0) {
                    Text(balance.net.abs().formatMoney(currencyCode), style = MaterialTheme.typography.bodyMediumEmphasized, color = balanceColor(balance.net))
                }
            }
        }
        AnimatedVisibility(visible = isExpanded) {
            SharedHistory(entries = state.history[balance.userId], otherName = name, config = config, onOpenExpense = onOpenExpense)
        }
    }
}

@Composable
private fun SharedHistory(entries: List<SharedHistoryEntry>?, otherName: String, config: HouseConfig?, onOpenExpense: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(start = Spacing.xxxxl), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        when {
            entries == null -> LoadingIndicator()
            entries.isEmpty() -> Text("You and $otherName haven't shared anything yet.", style = MaterialTheme.typography.bodySmall)
            else -> entries.forEach { entry -> HistoryLine(entry, config, onOpenExpense) }
        }
    }
}

@Composable
private fun HistoryLine(entry: SharedHistoryEntry, config: HouseConfig?, onOpenExpense: (String) -> Unit) {
    val currencyCode = config.currency()
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onOpenExpense(entry.expenseId) }.padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(if (entry.kind == ExpenseKind.SETTLEMENT) "Payment" else entry.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${entry.date.formatWithHouseConfig(config)} · ${entry.amount.formatMoney(currencyCode)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                when {
                    entry.kind == ExpenseKind.SETTLEMENT && entry.betweenUs.signum() > 0 -> "you paid"
                    entry.kind == ExpenseKind.SETTLEMENT -> "you received"
                    entry.betweenUs.signum() > 0 -> "you lent"
                    entry.betweenUs.signum() < 0 -> "you borrowed"
                    else -> "no change"
                },
                style = MaterialTheme.typography.labelSmall,
                color = balanceColor(entry.betweenUs),
            )
            Text(entry.betweenUs.abs().formatMoney(currencyCode), style = MaterialTheme.typography.bodyMediumEmphasized, color = balanceColor(entry.betweenUs))
        }
    }
}
