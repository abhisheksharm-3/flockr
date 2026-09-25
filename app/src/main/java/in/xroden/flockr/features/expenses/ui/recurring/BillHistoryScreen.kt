/** Every payment recorded against a bill, newest first, each opening as the expense it became. */
package `in`.xroden.flockr.features.expenses.ui.recurring

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.expenses.presentation.BillHistoryUiState
import `in`.xroden.flockr.features.expenses.presentation.BillHistoryViewModel
import `in`.xroden.flockr.features.expenses.ui.ledger.ExpenseRow
import `in`.xroden.flockr.features.house.model.currency
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.states.EmptyState
import `in`.xroden.flockr.ui.components.states.ErrorState

@Composable
fun BillHistoryScreen(
    houseId: String,
    billId: String,
    onNavigateBack: () -> Unit,
    onOpenExpense: (expenseId: String) -> Unit,
    viewModel: BillHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val config by rememberHouseConfig(houseId)

    LaunchedEffect(houseId, billId) { viewModel.load(houseId, billId) }

    Scaffold(
        topBar = {
            FlockrTopAppBar(
                title = "Payment history",
                subtitle = (state as? BillHistoryUiState.Ready)?.billName,
                onNavigateBack = onNavigateBack,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val current = state) {
                BillHistoryUiState.Loading -> LoadingIndicator(Modifier.align(Alignment.Center))
                is BillHistoryUiState.Error -> ErrorState(current.message, onRetry = { viewModel.load(houseId, billId) })
                is BillHistoryUiState.Ready -> if (current.payments.isEmpty()) {
                    EmptyState(icon = Icons.Rounded.History, title = "No payments yet", subtitle = "Payments you mark on this bill appear here.")
                } else {
                    LazyColumn {
                        items(current.payments, key = { it.id }) { payment ->
                            ExpenseRow(
                                expense = payment,
                                members = current.members,
                                viewerId = current.viewerId,
                                currencyCode = config.currency(),
                                onClick = { onOpenExpense(payment.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}
