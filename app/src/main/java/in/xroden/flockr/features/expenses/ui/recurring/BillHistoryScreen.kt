/** Every payment recorded against a bill, newest first and grouped by year, each opening as the expense it became. */
package `in`.xroden.flockr.features.expenses.ui.recurring

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.expenses.presentation.BillHistoryUiState
import `in`.xroden.flockr.features.expenses.presentation.BillHistoryViewModel
import `in`.xroden.flockr.features.expenses.ui.ledger.ExpenseRow
import `in`.xroden.flockr.features.house.model.currency
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.SkeletonRows
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.SectionTitle
import `in`.xroden.flockr.ui.components.states.EmptyState
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.formatMoney
import java.math.BigDecimal

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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(houseId, billId) { viewModel.load(houseId, billId) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            FlockrTopAppBar(
                title = "Payment history",
                subtitle = (state as? BillHistoryUiState.Ready)?.billName,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val current = state) {
                BillHistoryUiState.Loading -> SkeletonRows()
                is BillHistoryUiState.Error -> ErrorState(current.message, onRetry = { viewModel.load(houseId, billId) })
                is BillHistoryUiState.Ready -> if (current.payments.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.History,
                        title = "No payments yet",
                        subtitle = "When someone pays this bill from Bills, the payment shows up here.",
                    )
                } else {
                    val currencyCode = config.currency()
                    val byYear = current.payments.groupBy { it.date.year }
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Spacing.xxl)) {
                        byYear.forEach { (year, payments) ->
                            item(key = "year_$year") {
                                val total = payments.fold(BigDecimal.ZERO) { sum, payment -> sum + payment.amount }
                                val count = if (payments.size == 1) "1 payment" else "${payments.size} payments"
                                SectionTitle("$year", subtitle = "$count · ${total.formatMoney(currencyCode)} paid")
                            }
                            items(payments, key = { it.id }) { payment ->
                                ExpenseRow(
                                    expense = payment,
                                    members = current.members,
                                    viewerId = current.viewerId,
                                    currencyCode = currencyCode,
                                    onClick = { onOpenExpense(payment.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
