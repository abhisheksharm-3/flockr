/** One expense or payment in full: the amount, who paid, what each person owes, and deleting it. */
package `in`.xroden.flockr.features.expenses.ui.ledger

import `in`.xroden.flockr.features.house.model.nameInSentence
import `in`.xroden.flockr.features.house.model.nameOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.expenses.model.ExpenseKind
import `in`.xroden.flockr.features.expenses.model.ExpenseShare
import `in`.xroden.flockr.features.expenses.presentation.ExpenseDetailUiState
import `in`.xroden.flockr.features.expenses.presentation.ExpenseDetailViewModel
import `in`.xroden.flockr.features.expenses.ui.categoryIcon
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.model.currency
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.MemberAvatar
import `in`.xroden.flockr.ui.components.cards.SectionCard
import `in`.xroden.flockr.ui.components.dialogs.ConfirmDialog
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.IconSize
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.formatMoney
import `in`.xroden.flockr.utils.formatWithHouseConfig
import `in`.xroden.flockr.utils.rememberHaptics

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

    Scaffold(
        topBar = {
            FlockrTopAppBar(
                title = if (ready?.expense?.kind == ExpenseKind.SETTLEMENT) "Payment" else "Expense",
                onNavigateBack = onNavigateBack,
                actions = {
                    if (ready != null && ready.expense.kind == ExpenseKind.EXPENSE && ready.expense.perDiemMonth == null) {
                        IconButton(onClick = { onEdit(ready.expense.id) }) { Icon(Icons.Rounded.Edit, contentDescription = "Edit") }
                    }
                    if (ready?.canDelete == true) {
                        IconButton(onClick = { isConfirmingDelete = true }, enabled = !ready.isDeleting) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val current = state) {
                ExpenseDetailUiState.Loading -> LoadingIndicator(Modifier.align(Alignment.Center))
                is ExpenseDetailUiState.Error -> ErrorState(current.message, onRetry = { viewModel.load(houseId, expenseId) })
                is ExpenseDetailUiState.Ready -> DetailContent(current, config)
            }
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
private fun DetailContent(state: ExpenseDetailUiState.Ready, config: HouseConfig?) {
    val expense = state.expense
    val currencyCode = config.currency()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) {
                Box(Modifier.size(IconSize.xxl), contentAlignment = Alignment.Center) {
                    Icon(categoryIcon(expense.category), contentDescription = null, modifier = Modifier.size(IconSize.lg))
                }
            }
            Column {
                Text(expense.name, style = MaterialTheme.typography.headlineSmallEmphasized)
                Text(expense.amount.formatMoney(currencyCode), style = MaterialTheme.typography.displaySmallEmphasized)
            }
        }
        Text(
            buildString {
                append(expense.date.formatWithHouseConfig(config))
                append(" · added by ")
                append(state.members.nameInSentence(expense.createdBy, state.viewerId))
                expense.category?.let { append(" · $it") }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SectionCard(title = if (expense.kind == ExpenseKind.SETTLEMENT) "Between" else "Split ${expense.splitMethod?.label?.lowercase() ?: "— one person bears it"}") {
            expense.shares.sortedByDescending { it.paidShare }.forEach { share -> ShareLine(share, state, currencyCode) }
        }
        expense.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            SectionCard(title = "Notes") { Text(notes, style = MaterialTheme.typography.bodyLarge) }
        }
        val origin = when {
            expense.recurringExpenseId != null -> "Recorded as a payment of a recurring bill."
            expense.perDiemMonth != null -> "Worked out from the month's usage log. Delete it to change that month's usage."
            else -> null
        }
        origin?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

/** One person's part: "Riya paid ₹90 and owes ₹30", or just what they owe or received. */
@Composable
private fun ShareLine(share: ExpenseShare, state: ExpenseDetailUiState.Ready, currencyCode: String) {
    val member = state.members[share.userId]
    val name = state.members.nameOf(share.userId, state.viewerId)
    val isSettlement = state.expense.kind == ExpenseKind.SETTLEMENT
    val description = when {
        isSettlement && share.paidShare.signum() > 0 -> "paid ${share.paidShare.formatMoney(currencyCode)}"
        isSettlement -> "received ${share.owedShare.formatMoney(currencyCode)}"
        share.paidShare.signum() > 0 && share.owedShare.signum() > 0 ->
            "paid ${share.paidShare.formatMoney(currencyCode)} and owe${if (share.userId == state.viewerId) "" else "s"} ${share.owedShare.formatMoney(currencyCode)}"
        share.paidShare.signum() > 0 -> "paid ${share.paidShare.formatMoney(currencyCode)}"
        else -> "owe${if (share.userId == state.viewerId) "" else "s"} ${share.owedShare.formatMoney(currencyCode)}"
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxWidth()) {
        MemberAvatar(name = member?.displayName ?: name, avatarUrl = member?.avatarUrl)
        Text("$name $description", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}
