package `in`.xroden.flockr.features.expenses.ui.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.ui.components.cards.SectionCard
import `in`.xroden.flockr.ui.components.charts.SimpleBarChart
import `in`.xroden.flockr.ui.components.charts.SimplePieChart
import `in`.xroden.flockr.features.expenses.domain.ExpenseViewModel
import `in`.xroden.flockr.features.expenses.domain.PerDiemViewModel
import `in`.xroden.flockr.ui.theme.*
import `in`.xroden.flockr.ui.theme.DateFormats
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.ui.util.getCurrencySymbol
import java.time.YearMonth
import java.time.format.DateTimeFormatter

import `in`.xroden.flockr.features.expenses.domain.MonthlySummaryUiState
import `in`.xroden.flockr.features.expenses.domain.PerDiemBillUiState
import `in`.xroden.flockr.features.expenses.domain.PerDiemConfigUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportsScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: ExpenseViewModel = hiltViewModel(),
    perDiemViewModel: PerDiemViewModel = hiltViewModel()
) {
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    val summaryState by viewModel.summaryState.collectAsState()
    val perDiemBillState by perDiemViewModel.billState.collectAsState()
    val perDiemConfigState by perDiemViewModel.configState.collectAsState()
    val houseConfig by viewModel.houseConfig.collectAsState()

    LaunchedEffect(houseId, selectedMonth) {
        val monthStr = selectedMonth.format(DateTimeFormatter.ofPattern(DateFormats.YEAR_MONTH)) + "-01"
        viewModel.loadMonthlySummary(houseId, monthStr)
        viewModel.loadMonthlySummary(houseId, monthStr)
        // viewModel.loadPerDiemBillItemized(houseId, monthStr) // Removed from ExpenseViewModel
        // viewModel.loadSpendByCategory(houseId, monthStr) // Loaded via loadMonthlySummary
        // viewModel.loadSpendByMember(houseId, monthStr) // Loaded via loadMonthlySummary
        viewModel.loadHouseConfig(houseId)
        perDiemViewModel.loadConfigs(houseId)
        perDiemViewModel.loadPerDiemReports(houseId, monthStr)
    }

    // Chart colors from theme
    val chartColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.error,
        Color(0xFF10B981), // Emerald
        Color(0xFFF59E0B), // Amber
        Color(0xFF8B5CF6), // Violet
        Color(0xFFEC4899)  // Pink
    )

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Monthly Report",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Month Selector
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Financial Summary",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { selectedMonth = selectedMonth.minusMonths(1) }) {
                                Icon(Icons.Default.ChevronLeft, "Previous Month")
                            }

                            Text(
                                text = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            IconButton(
                                onClick = { selectedMonth = selectedMonth.plusMonths(1) },
                                enabled = selectedMonth < YearMonth.now()
                            ) {
                                Icon(Icons.Default.ChevronRight, "Next Month")
                            }
                        }
                    }
                }
            }

            // Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Monthly Overview",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        // Total Expenses - Prominent
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Total Expenses",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            val totalExpenses = if (summaryState is MonthlySummaryUiState.Success) {
                                (summaryState as MonthlySummaryUiState.Success).summary.totalExpenses
                            } else java.math.BigDecimal.ZERO

                            Text(
                                text = "${getCurrencySymbol(houseConfig?.currencyCode ?: "$")}%.2f".format(totalExpenses),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                        // Breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val summary = if (summaryState is MonthlySummaryUiState.Success) {
                                (summaryState as MonthlySummaryUiState.Success).summary
                            } else null

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "One-Time",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "${getCurrencySymbol(houseConfig?.currencyCode ?: "$")}%.2f".format(summary?.oneTimeExpenses ?: java.math.BigDecimal.ZERO),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Recurring",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "${getCurrencySymbol(houseConfig?.currencyCode ?: "$")}%.2f".format(summary?.recurringExpenses ?: java.math.BigDecimal.ZERO),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Per Diem",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "${getCurrencySymbol(houseConfig?.currencyCode ?: "$")}%.2f".format(summary?.perDiemExpenses ?: java.math.BigDecimal.ZERO),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Spending by Member
            item {
                SectionCard(title = "Spending by Member") {
                    val spendByMember = if (summaryState is MonthlySummaryUiState.Success) {
                        (summaryState as MonthlySummaryUiState.Success).spendByMember
                    } else emptyList()

                    val perDiemItemized = if (perDiemBillState is PerDiemBillUiState.Success) {
                        (perDiemBillState as PerDiemBillUiState.Success).itemized
                    } else emptyList()

                    if (spendByMember.isEmpty() && perDiemItemized.isEmpty()) {
                        Text(
                            text = "No expenses recorded this month",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Build member data map - individual spending
                            val memberData = spendByMember.associate {
                                (it.fullName ?: "Unknown") to (it.totalSpent?.toDouble() ?: 0.0)
                            }.toMutableMap()

                            // Add per diem as "House" spending
                            val perDiemTotal = perDiemItemized.fold(java.math.BigDecimal.ZERO) { acc, item ->
                                acc.add(item.totalAmount ?: java.math.BigDecimal.ZERO)
                            }
                            
                            if (perDiemTotal > java.math.BigDecimal.ZERO) {
                                memberData["House (Per Diem)"] = perDiemTotal.toDouble()
                            }

                            SimpleBarChart(
                                data = memberData,
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                currencySymbol = getCurrencySymbol(houseConfig?.currencyCode ?: "$")
                            )
                        }
                    }
                }
            }

            // Spending by Category
            item {
                SectionCard(title = "Spending by Category") {
                    val spendByCategory = if (summaryState is MonthlySummaryUiState.Success) {
                        (summaryState as MonthlySummaryUiState.Success).spendByCategory
                    } else emptyList()

                    val perDiemItemized = if (perDiemBillState is PerDiemBillUiState.Success) {
                        (perDiemBillState as PerDiemBillUiState.Success).itemized
                    } else emptyList()

                    if (spendByCategory.isEmpty() && perDiemItemized.isEmpty()) {
                        Text(
                            text = "No expenses recorded this month",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Category data is now fully aggregated by the RPC
                            val categoryData = spendByCategory.associate {
                                (it.category ?: "Uncategorized") to (it.totalAmount?.toDouble() ?: 0.0)
                            }

                            SimplePieChart(
                                data = categoryData,
                                colors = chartColors,
                                modifier = Modifier.fillMaxWidth(),
                                currencySymbol = getCurrencySymbol(houseConfig?.currencyCode ?: "$")
                            )
                        }
                    }
                }
            }

            // Per Diem Itemized
            item {
                SectionCard(title = "Per Diem Usage by Item") {
                    val perDiemItemized = if (perDiemBillState is PerDiemBillUiState.Success) {
                        (perDiemBillState as PerDiemBillUiState.Success).itemized
                    } else emptyList()

                    if (perDiemItemized.isEmpty()) {
                        Text(
                            text = "No per diem items used this month",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            perDiemItemized.forEach { item ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    shape = MaterialTheme.shapes.medium,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Item name and amount
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.itemName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${getCurrencySymbol(houseConfig?.currencyCode ?: "$")}${String.format("%.2f", item.totalAmount)}",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        )

                                        // Quantity and rate details
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ShoppingBag,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                                Text(
                                                    text = "Quantity: ${String.format("%.1f", item.totalQuantity)} ${item.unit}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "@${getCurrencySymbol(houseConfig?.currencyCode ?: "$")}${String.format("%.2f", item.rate)}/${item.unit}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Total per diem
                            val summary = if (summaryState is MonthlySummaryUiState.Success) {
                                (summaryState as MonthlySummaryUiState.Success).summary
                            } else null

                            if (summary?.perDiemExpenses != null && summary.perDiemExpenses > java.math.BigDecimal.ZERO) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Total Per Diem",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${getCurrencySymbol(houseConfig?.currencyCode ?: "$")}${String.format("%.2f", summary.perDiemExpenses)}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
