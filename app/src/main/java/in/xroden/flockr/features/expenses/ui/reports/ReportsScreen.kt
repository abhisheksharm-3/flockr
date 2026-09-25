/** A month of house spending at a glance: the total and its parts, by category, by person, and usage by item. */
package `in`.xroden.flockr.features.expenses.ui.reports

import `in`.xroden.flockr.features.expenses.model.SpendByMember
import `in`.xroden.flockr.ui.components.balanceColor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.expenses.presentation.ReportsUiState
import `in`.xroden.flockr.features.expenses.presentation.ReportsViewModel
import `in`.xroden.flockr.features.house.model.currency
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.cards.SectionCard
import `in`.xroden.flockr.ui.components.charts.ChartEntry
import `in`.xroden.flockr.ui.components.charts.SimplePieChart
import `in`.xroden.flockr.ui.components.inputs.MonthSelector
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.formatMoney
import java.math.BigDecimal

@Composable
fun ReportsScreen(houseId: String, onNavigateBack: () -> Unit, viewModel: ReportsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val month by viewModel.month.collectAsStateWithLifecycle()
    val config by rememberHouseConfig(houseId)

    LaunchedEffect(houseId) { viewModel.load(houseId) }

    Scaffold(topBar = { FlockrTopAppBar(title = "Reports", onNavigateBack = onNavigateBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            month?.let {
                MonthSelector(
                    selectedMonth = it,
                    onMonthChange = viewModel::onMonthChange,
                    timezone = config?.timezone,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                )
            }
            Box(Modifier.fillMaxSize()) {
                when (val current = state) {
                    ReportsUiState.Loading -> LoadingIndicator(Modifier.align(Alignment.Center))
                    is ReportsUiState.Error -> ErrorState(current.message, onRetry = { viewModel.load(houseId) })
                    is ReportsUiState.Ready -> ReportsContent(current, config.currency())
                }
            }
        }
    }
}

@Composable
private fun ReportsContent(state: ReportsUiState.Ready, currencyCode: String) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        SectionCard(title = state.summary.totalSpend.formatMoney(currencyCode), subtitle = "Spent this month, not counting payments between housemates") {
            SummaryLine("One-off expenses", state.summary.oneTimeSpend, currencyCode)
            SummaryLine("Bills", state.summary.recurringSpend, currencyCode)
            SummaryLine("Usage", state.summary.perDiemSpend, currencyCode)
        }
        if (state.byCategory.isNotEmpty()) {
            SectionCard(title = "By category") {
                SimplePieChart(data = state.byCategory.map { ChartEntry(it.category, it.category, it.total) }, currencyCode = currencyCode)
            }
        }
        if (state.byMember.isNotEmpty()) {
            SectionCard(title = "Who paid", subtitle = "What each person paid for the house, and their own share of it") {
                state.byMember.sortedByDescending { it.paid }.forEach { member -> MemberSpendLine(member, currencyCode) }
            }
        }
        if (state.usage.isNotEmpty()) {
            SectionCard(title = "Usage by item") {
                state.usage.forEach { item ->
                    SummaryLine("${item.totalQuantity.stripTrailingZeros().toPlainString()} ${item.unit} ${item.itemName}", item.totalCost, currencyCode)
                }
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, amount: BigDecimal, currencyCode: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(amount.formatMoney(currencyCode), style = MaterialTheme.typography.bodyLargeEmphasized)
    }
}

/** One person's month: what they paid, their share, and whether that leaves them ahead or behind. */
@Composable
private fun MemberSpendLine(member: SpendByMember, currencyCode: String) {
    val difference = member.paid - member.consumed
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(member.fullName, style = MaterialTheme.typography.bodyLargeEmphasized)
            Text(
                "paid ${member.paid.formatMoney(currencyCode)} · share ${member.consumed.formatMoney(currencyCode)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            when (difference.signum()) {
                1 -> "+${difference.formatMoney(currencyCode)}"
                -1 -> "−${difference.abs().formatMoney(currencyCode)}"
                else -> "even"
            },
            style = MaterialTheme.typography.bodyLargeEmphasized,
            color = balanceColor(difference),
        )
    }
}
