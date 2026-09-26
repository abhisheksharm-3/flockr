/** Everyone's balance, the fewest payments that settle the house, and what any two people have shared. */
package `in`.xroden.flockr.features.expenses.ui.ledger

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import `in`.xroden.flockr.features.house.model.nameInSentence
import `in`.xroden.flockr.features.house.model.nameOf
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.SkeletonRow
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
import `in`.xroden.flockr.ui.components.balanceColor
import `in`.xroden.flockr.ui.components.balanceHeadline
import `in`.xroden.flockr.ui.components.isHeroScrolledAway
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
    val listState = rememberLazyListState()

    LaunchedEffect(houseId) { viewModel.load(houseId) }

    Scaffold { padding ->
        Box(Modifier.fillMaxSize()) {
            when (val current = state) {
                BalancesUiState.Loading -> SkeletonHeroScreen()
                is BalancesUiState.Error -> Box(Modifier.fillMaxSize().padding(padding)) {
                    ErrorState(current.message, onRetry = { viewModel.load(houseId) })
                }
                is BalancesUiState.Ready -> {
                    BalancesContent(
                        state = current,
                        config = config,
                        listState = listState,
                        onSettleUp = onSettleUp,
                        onOpenExpense = onOpenExpense,
                        onExpandMember = { viewModel.loadHistory(houseId, it) },
                    )
                    HeroStatusBarScrim(isHeroGone = listState.isHeroScrolledAway)
                }
            }
        }
    }
}

@Composable
private fun BalancesContent(
    state: BalancesUiState.Ready,
    config: HouseConfig?,
    listState: LazyListState,
    onSettleUp: (String, String, BigDecimal) -> Unit,
    onOpenExpense: (String) -> Unit,
    onExpandMember: (String) -> Unit,
) {
    val haptics = rememberHaptics()
    val currencyCode = config.currency()
    var expandedUserId by rememberSaveable { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = Spacing.xxl)) {
        item(key = "hero") { BalanceHero(state, currencyCode, onSettleUp) }
        item(key = "plan_title") {
            SectionTitle("Settle up", subtitle = "The fewest payments that square everyone, whatever order the expenses came in")
        }
        if (state.standing.plan.isEmpty()) {
            item(key = "plan_empty") {
                ListRow(
                    headline = "Everyone is settled up",
                    supporting = "When someone owes, the payment that squares it shows up here.",
                    leading = { IconBadge(Icons.Rounded.Celebration, BadgeTone.JADE) },
                )
            }
        }
        items(state.standing.plan, key = { "plan_${it.fromUserId}_${it.toUserId}" }) { payment ->
            PlanRow(payment, state, currencyCode, onSettleUp)
        }
        item(key = "balances_title") { SectionTitle("Everyone's balance", subtitle = "Tap a housemate to see what you've shared") }
        items(state.standing.balances, key = { it.userId }) { balance ->
            BalanceRow(
                balance = balance,
                state = state,
                config = config,
                isExpanded = expandedUserId == balance.userId,
                onToggle = {
                    haptics.toggle(expandedUserId != balance.userId)
                    expandedUserId = if (expandedUserId == balance.userId) null else balance.userId
                    onExpandMember(balance.userId)
                },
                onOpenExpense = onOpenExpense,
            )
        }
        item(key = "inset") { Spacer(Modifier.navigationBarsPadding()) }
    }
}

/** Where the viewer stands in the house, and the one payment that would square them first. */
@Composable
private fun BalanceHero(state: BalancesUiState.Ready, currencyCode: String, onSettleUp: (String, String, BigDecimal) -> Unit) {
    val net = state.standing.netOf(state.viewerId)
    val mine = state.standing.paymentsOf(state.viewerId)
    HeroHeader(title = "Balances") {
        HeroLabel(balanceHeadline(net))
        if (net.signum() == 0) HeroBadge("All square") else HeroCountUp(net.abs(), currencyCode)
        HeroCaption(
            when {
                state.standing.plan.isEmpty() -> "Nobody owes anybody. Nice."
                mine.isEmpty() -> "You're square. The rest of the house has ${if (state.standing.plan.size == 1) "one payment" else "${state.standing.plan.size} payments"} to sort out."
                mine.size == 1 -> "One payment settles you up."
                else -> "${mine.size} payments settle you up."
            },
        )
        mine.firstOrNull()?.let { payment ->
            HeroActions {
                HeroButton(
                    text = if (payment.fromUserId == state.viewerId) "Settle up" else "Record payment",
                    onClick = { onSettleUp(payment.fromUserId, payment.toUserId, payment.amount) },
                )
            }
        }
    }
}

/** "Karan pays Riya ₹200", with a button when the viewer is one of the two. */
@Composable
private fun PlanRow(payment: SettleUpPayment, state: BalancesUiState.Ready, currencyCode: String, onSettleUp: (String, String, BigDecimal) -> Unit) {
    val haptics = rememberHaptics()
    val payer = state.members[payment.fromUserId]
    val from = state.members.nameOf(payment.fromUserId, state.viewerId)
    val to = state.members.nameInSentence(payment.toUserId, state.viewerId)
    val isViewerPaying = state.viewerId == payment.fromUserId
    val involvesViewer = isViewerPaying || state.viewerId == payment.toUserId
    ListRow(
        headline = "$from ${if (isViewerPaying) "pay" else "pays"} $to",
        supporting = payment.amount.formatMoney(currencyCode),
        leading = { MemberAvatar(name = payer?.displayName ?: from, avatarUrl = payer?.avatarUrl) },
        trailing = if (involvesViewer) {
            {
                FilledTonalButton(onClick = { haptics.tap(); onSettleUp(payment.fromUserId, payment.toUserId, payment.amount) }, shapes = ButtonDefaults.shapes()) {
                    Text(if (isViewerPaying) "Settle up" else "Record payment")
                }
            }
        } else {
            null
        },
    )
}

/** A member's net balance; opening it shows what they and the viewer have shared. */
@Composable
private fun BalanceRow(
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
    Column(Modifier.fillMaxWidth()) {
        ListRow(
            headline = if (balance.isActive) name else "$name (left)",
            supporting = "Paid ${balance.paid.formatMoney(currencyCode)} · share ${balance.owed.formatMoney(currencyCode)}",
            leading = { MemberAvatar(name = member?.displayName ?: balance.fullName, avatarUrl = member?.avatarUrl) },
            trailing = {
                if (balance.net.signum() == 0) {
                    TrailingAmount("Settled up")
                } else {
                    TrailingAmount(
                        amount = balance.net.abs().formatMoney(currencyCode),
                        detail = if (balance.net.signum() > 0) "gets back" else "owes",
                        detailColor = balanceColor(balance.net),
                    )
                }
            },
            onClick = if (balance.userId != state.viewerId) onToggle else null,
        )
        AnimatedVisibility(visible = isExpanded) {
            SharedHistory(entries = state.history[balance.userId], otherName = name, config = config, onOpenExpense = onOpenExpense)
        }
    }
}

@Composable
private fun SharedHistory(entries: List<SharedHistoryEntry>?, otherName: String, config: HouseConfig?, onOpenExpense: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(start = Spacing.xxxxl, bottom = Spacing.sm)) {
        when {
            entries == null -> Column { repeat(2) { SkeletonRow() } }
            entries.isEmpty() -> Text(
                "You and $otherName haven't shared anything yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            )
            else -> entries.forEach { entry -> HistoryRow(entry, config, onOpenExpense) }
        }
    }
}

/** One thing the pair shared, with which way it moved the balance between them. */
@Composable
private fun HistoryRow(entry: SharedHistoryEntry, config: HouseConfig?, onOpenExpense: (String) -> Unit) {
    val currencyCode = config.currency()
    ListRow(
        headline = if (entry.kind == ExpenseKind.SETTLEMENT) "Payment" else entry.name,
        supporting = "${entry.date.formatWithHouseConfig(config)} · ${entry.amount.formatMoney(currencyCode)}",
        trailing = {
            TrailingAmount(
                amount = entry.betweenUs.abs().formatMoney(currencyCode),
                detail = when {
                    entry.kind == ExpenseKind.SETTLEMENT && entry.betweenUs.signum() > 0 -> "you paid"
                    entry.kind == ExpenseKind.SETTLEMENT -> "you received"
                    entry.betweenUs.signum() > 0 -> "you lent"
                    entry.betweenUs.signum() < 0 -> "you borrowed"
                    else -> "no change"
                },
                detailColor = balanceColor(entry.betweenUs),
            )
        },
        onClick = { onOpenExpense(entry.expenseId) },
    )
}
