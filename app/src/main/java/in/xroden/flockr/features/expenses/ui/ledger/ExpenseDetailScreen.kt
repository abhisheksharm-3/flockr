/** One expense or payment in full: the amount and what it means for you, each person's part, and deleting it. */
package `in`.xroden.flockr.features.expenses.ui.ledger

import coil3.compose.AsyncImage
import androidx.core.net.toUri
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
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
import androidx.compose.ui.graphics.Color
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.expenses.model.Expense
import `in`.xroden.flockr.features.expenses.model.ExpenseKind
import `in`.xroden.flockr.features.expenses.model.ExpenseShare
import `in`.xroden.flockr.features.expenses.presentation.ExpenseDetailUiState
import `in`.xroden.flockr.features.expenses.presentation.ExpenseDetailViewModel
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.model.currency
import `in`.xroden.flockr.features.house.model.nameInSentence
import `in`.xroden.flockr.features.house.model.nameOf
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.SkeletonHeroScreen
import `in`.xroden.flockr.ui.components.BadgeTone
import `in`.xroden.flockr.ui.components.HeroActions
import `in`.xroden.flockr.ui.components.HeroAmount
import `in`.xroden.flockr.ui.components.HeroCaption
import `in`.xroden.flockr.ui.components.HeroHeader
import `in`.xroden.flockr.ui.components.HeroLabel
import `in`.xroden.flockr.ui.components.HeroSecondaryButton
import `in`.xroden.flockr.ui.components.HeroStatusBarScrim
import `in`.xroden.flockr.ui.components.IconBadge
import `in`.xroden.flockr.ui.components.ListRow
import `in`.xroden.flockr.ui.components.MemberAvatar
import `in`.xroden.flockr.ui.components.SectionTitle
import `in`.xroden.flockr.ui.components.dialogs.ConfirmDialog
import `in`.xroden.flockr.ui.components.isHeroScrolledAway
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.formatMoney
import `in`.xroden.flockr.utils.formatWithHouseConfig
import `in`.xroden.flockr.utils.rememberHaptics

private val RECEIPT_PREVIEW_HEIGHT = 320.dp

@Composable
fun ExpenseDetailScreen(
    houseId: String,
    expenseId: String,
    onNavigateBack: () -> Unit,
    onEdit: (expenseId: String) -> Unit,
    viewModel: ExpenseDetailViewModel = hiltViewModel(),
) {
    val haptics = rememberHaptics()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val config by rememberHouseConfig(houseId)
    val snackbarHostState = remember { SnackbarHostState() }
    var isConfirmingDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(houseId, expenseId) { viewModel.load(houseId, expenseId) }
    LaunchedEffect(Unit) {
        viewModel.deleted.collect {
            haptics.success()
            onNavigateBack()
        }
    }
    val ready = state as? ExpenseDetailUiState.Ready
    LaunchedEffect(ready?.deleteError) {
        val message = ready?.deleteError ?: return@LaunchedEffect
        haptics.error()
        snackbarHostState.showSnackbar(message)
        viewModel.dismissDeleteError()
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        when (val current = state) {
            ExpenseDetailUiState.Loading -> SkeletonHeroScreen()
            is ExpenseDetailUiState.Error -> Box(Modifier.fillMaxSize().padding(padding)) {
                ErrorState(current.message, onRetry = { viewModel.load(houseId, expenseId) })
            }
            is ExpenseDetailUiState.Ready -> DetailContent(
                state = current,
                config = config,
                onEdit = { onEdit(current.expense.id) },
                onDelete = { isConfirmingDelete = true },
            )
        }
    }

    if (isConfirmingDelete && ready != null) {
        val isSettlement = ready.expense.kind == ExpenseKind.SETTLEMENT
        ConfirmDialog(
            title = if (isSettlement) "Delete this payment?" else "Delete this expense?",
            message = "Balances will be recalculated for everyone on it. This can't be undone.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                isConfirmingDelete = false
                viewModel.delete()
            },
            onDismiss = { isConfirmingDelete = false },
        )
    }
}

@Composable
private fun DetailContent(
    state: ExpenseDetailUiState.Ready,
    config: HouseConfig?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val expense = state.expense
    val currencyCode = config.currency()
    val isSettlement = expense.kind == ExpenseKind.SETTLEMENT
    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = Spacing.xxl)) {
            item(key = "hero") {
                HeroHeader(
                    title = if (isSettlement) "Payment" else expense.name,
                    subtitle = listOfNotNull(expense.category?.takeIf { it != expense.name }, expense.date.formatWithHouseConfig(config)).joinToString(" · "),
                ) {
                    HeroLabel(whoPaid(expense, state))
                    HeroAmount(expense.amount.formatMoney(currencyCode))
                    HeroCaption(viewerSentence(expense, state.viewerId, currencyCode))
                    if (expense.kind == ExpenseKind.EXPENSE && expense.perDiemMonth == null) {
                        HeroActions { HeroSecondaryButton("Edit expense", onClick = onEdit) }
                    }
                }
            }
            item(key = "shares_title") {
                SectionTitle(if (isSettlement) "Between" else expense.splitMethod?.let { "Split ${it.phrase}" } ?: "Not split")
            }
            items(expense.shares.sortedByDescending { it.paidShare }, key = { it.userId }) { share -> ShareRow(share, state, currencyCode) }
            expense.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                item(key = "notes_title") { SectionTitle("Notes") }
                item(key = "notes") { QuietLine(notes, MaterialTheme.colorScheme.onSurface) }
            }
            state.receiptUrl?.let { url ->
                item(key = "receipt_title") { SectionTitle("Receipt") }
                item(key = "receipt") {
                    val context = LocalContext.current
                    AsyncImage(
                        model = url,
                        contentDescription = "Receipt photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .padding(horizontal = Spacing.lg)
                            .fillMaxWidth()
                            .heightIn(max = RECEIPT_PREVIEW_HEIGHT)
                            .clip(MaterialTheme.shapes.large)
                            .clickable(onClickLabel = "Open the receipt") {
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
                            },
                    )
                }
            }
            item(key = "origin") {
                val origin = when {
                    expense.recurringExpenseId != null -> "Recorded as a payment of a recurring bill."
                    expense.perDiemMonth != null -> "Worked out from the month's usage log. Delete it to change that month's usage."
                    else -> null
                }
                QuietLine(
                    listOfNotNull("Added by ${state.members.nameInSentence(expense.createdBy, state.viewerId)}.", origin).joinToString(" "),
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    Modifier.padding(top = Spacing.lg),
                )
            }
            if (state.canDelete) {
                item(key = "delete") {
                    ListRow(
                        headline = if (isSettlement) "Delete this payment" else "Delete this expense",
                        leading = { IconBadge(Icons.Rounded.Delete, BadgeTone.ROSE) },
                        onClick = if (state.isDeleting) null else onDelete,
                        headlineColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = Spacing.sm),
                    )
                }
            }
            item(key = "inset") { Spacer(Modifier.navigationBarsPadding()) }
        }
        HeroStatusBarScrim(isHeroGone = listState.isHeroScrolledAway)
    }
}

/** "Riya paid" above an expense's amount, or "Karan paid you" above a payment's. */
private fun whoPaid(expense: Expense, state: ExpenseDetailUiState.Ready): String {
    val payer = state.members.nameOf(expense.payerId, state.viewerId)
    if (expense.kind != ExpenseKind.SETTLEMENT) return "$payer paid"
    val receiver = expense.shares.firstOrNull { it.owedShare.signum() > 0 }?.userId
    return "$payer paid ${state.members.nameInSentence(receiver, state.viewerId)}"
}

/** What this expense or payment did to the viewer's balance, in words. */
private fun viewerSentence(expense: Expense, viewerId: String, currencyCode: String): String {
    val share = expense.shareOf(viewerId)
    if (expense.kind == ExpenseKind.SETTLEMENT) {
        return when {
            share == null -> "It doesn't change your balance."
            share.paidShare.signum() > 0 -> "It comes off what you owed."
            else -> "It comes off what you're owed."
        }
    }
    return when {
        share == null -> "You're not part of this one."
        share.net.signum() > 0 && share.owedShare.signum() > 0 ->
            "You lent ${share.net.formatMoney(currencyCode)}. Your share is ${share.owedShare.formatMoney(currencyCode)}."
        share.net.signum() > 0 -> "You lent ${share.net.formatMoney(currencyCode)}."
        share.net.signum() < 0 -> "You owe ${share.net.abs().formatMoney(currencyCode)} for this."
        else -> "You're square on this one."
    }
}

/** One person's part: "Paid ₹90 and owes ₹30", or just what they owe or received. */
@Composable
private fun ShareRow(share: ExpenseShare, state: ExpenseDetailUiState.Ready, currencyCode: String) {
    val member = state.members[share.userId]
    val name = state.members.nameOf(share.userId, state.viewerId)
    val owe = if (share.userId == state.viewerId) "owe" else "owes"
    val description = when {
        state.expense.kind == ExpenseKind.SETTLEMENT && share.paidShare.signum() > 0 -> "Paid ${share.paidShare.formatMoney(currencyCode)}"
        state.expense.kind == ExpenseKind.SETTLEMENT -> "Received ${share.owedShare.formatMoney(currencyCode)}"
        share.paidShare.signum() > 0 && share.owedShare.signum() > 0 ->
            "Paid ${share.paidShare.formatMoney(currencyCode)} and $owe ${share.owedShare.formatMoney(currencyCode)}"
        share.paidShare.signum() > 0 -> "Paid ${share.paidShare.formatMoney(currencyCode)}"
        else -> "${owe.replaceFirstChar { it.uppercase() }} ${share.owedShare.formatMoney(currencyCode)}"
    }
    ListRow(
        headline = name,
        supporting = description,
        leading = { MemberAvatar(name = member?.displayName ?: name, avatarUrl = member?.avatarUrl) },
    )
}

@Composable
private fun QuietLine(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        modifier = modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    )
}
