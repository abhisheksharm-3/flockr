package `in`.xroden.flockr.features.expenses.ui.reports

import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.clickable
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
import `in`.xroden.flockr.ui.components.inputs.MonthSelector
import `in`.xroden.flockr.ui.components.charts.SimpleBarChart
import `in`.xroden.flockr.ui.components.charts.SimplePieChart

import `in`.xroden.flockr.features.expenses.presentation.MonthlySummaryViewModel
import `in`.xroden.flockr.features.expenses.presentation.PerDiemViewModel

import `in`.xroden.flockr.utils.getCurrencySymbol
import `in`.xroden.flockr.utils.getTodayInHouseTimezone
import kotlinx.datetime.*

import `in`.xroden.flockr.features.expenses.model.PerDiemBillItemized
import `in`.xroden.flockr.features.expenses.presentation.MonthlySummaryUiState
import `in`.xroden.flockr.features.expenses.presentation.PerDiemBillUiState
import `in`.xroden.flockr.features.house.model.HouseConfig
import java.math.BigDecimal
import java.util.Locale
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportsScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToCategory: (String) -> Unit,
    onNavigateToUser: (String) -> Unit,
    onNavigateToOneTimeExpenses: () -> Unit = {},
    onNavigateToRecurringExpenses: () -> Unit = {},
    onNavigateToPerDiemExpenses: () -> Unit = {},
    viewModel: MonthlySummaryViewModel = hiltViewModel(),
    perDiemViewModel: PerDiemViewModel = hiltViewModel()
) {
    val summaryState by viewModel.summaryState.collectAsState()
    val perDiemBillState by perDiemViewModel.billState.collectAsState()
    val houseConfig by viewModel.houseConfig.collectAsState()

    // Initial month based on house timezone, updated when houseConfig loads
    var selectedMonth by remember {
        mutableStateOf(
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.let {
                LocalDate(it.year, it.month, 1)
            }
        )
    }

    // Update selected month when house config loads to use correct timezone
    LaunchedEffect(houseConfig) {
        houseConfig?.let {
            val houseToday = it.getTodayInHouseTimezone()
            selectedMonth = LocalDate(houseToday.year, houseToday.month, 1)
        }
    }

    LaunchedEffect(houseId, selectedMonth) {
        val monthStr = "${selectedMonth.year}-${selectedMonth.monthNumber.toString().padStart(2, '0')}-01"
        viewModel.loadMonthlySummary(houseId, monthStr)
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
            MonthlyReportsTopBar(onNavigateBack = onNavigateBack)
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
                MonthSelectorSection(
                    selectedMonth = selectedMonth,
                    onMonthChange = { selectedMonth = it },
                    timezone = houseConfig?.timezone
                )
            }

            // Summary Card
            item {
                MonthlyOverviewCard(
                    summaryState = summaryState,
                    houseConfig = houseConfig,
                    onNavigateToOneTimeExpenses = onNavigateToOneTimeExpenses,
                    onNavigateToRecurringExpenses = onNavigateToRecurringExpenses,
                    onNavigateToPerDiemExpenses = onNavigateToPerDiemExpenses
                )
            }

            // Spending by Member
            item {
                SpendingByMemberSection(
                    summaryState = summaryState,
                    perDiemBillState = perDiemBillState,
                    houseConfig = houseConfig,
                    onNavigateToUser = onNavigateToUser
                )
            }

            // Spending by Category
            item {
                SpendingByCategorySection(
                    summaryState = summaryState,
                    perDiemBillState = perDiemBillState,
                    chartColors = chartColors,
                    houseConfig = houseConfig,
                    onNavigateToCategory = onNavigateToCategory
                )
            }

            // Per Diem Itemized
            item {
                PerDiemItemizedSection(
                    summaryState = summaryState,
                    perDiemBillState = perDiemBillState,
                    houseConfig = houseConfig
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthlyReportsTopBar(onNavigateBack: () -> Unit) {
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
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
private fun MonthSelectorSection(
    selectedMonth: LocalDate,
    onMonthChange: (LocalDate) -> Unit,
    timezone: String?
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        MonthSelector(
            selectedMonth = selectedMonth,
            onMonthChange = onMonthChange,
            timezone = timezone
        )
    }
}

@Composable
private fun MonthlyOverviewCard(
    summaryState: MonthlySummaryUiState,
    houseConfig: HouseConfig?,
    onNavigateToOneTimeExpenses: () -> Unit,
    onNavigateToRecurringExpenses: () -> Unit,
    onNavigateToPerDiemExpenses: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
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
                } else BigDecimal.ZERO

                Text(
                    text = "${getCurrencySymbol(houseConfig?.currencyCode ?: "$")}%.2f".format(Locale.getDefault(), totalExpenses),
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
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onNavigateToOneTimeExpenses() }
                        .padding(8.dp)
                ) {
                    Text(
                        text = "One-Time",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${getCurrencySymbol(houseConfig?.currencyCode ?: "$")}%.2f".format(Locale.getDefault(), summary?.oneTimeExpenses ?: BigDecimal.ZERO),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onNavigateToRecurringExpenses() }
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Recurring",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${getCurrencySymbol(houseConfig?.currencyCode ?: "$")}%.2f".format(Locale.getDefault(), summary?.recurringExpenses ?: BigDecimal.ZERO),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onNavigateToPerDiemExpenses() }
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Per Diem",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${getCurrencySymbol(houseConfig?.currencyCode ?: "$")}%.2f".format(Locale.getDefault(), summary?.perDiemExpenses ?: BigDecimal.ZERO),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun SpendingByMemberSection(
    summaryState: MonthlySummaryUiState,
    perDiemBillState: PerDiemBillUiState,
    houseConfig: HouseConfig?,
    onNavigateToUser: (String) -> Unit
) {
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
                    (it.fullName ?: "Unknown") to (it.totalSpent.toDouble())
                }.toMutableMap()

                // Add per diem as "House" spending
                val perDiemTotal = perDiemItemized.fold(BigDecimal.ZERO) { acc, item ->
                    acc.add(item.totalAmount)
                }

                if (perDiemTotal > BigDecimal.ZERO) {
                    memberData["House (Per Diem)"] = perDiemTotal.toDouble()
                }

                SimpleBarChart(
                    data = memberData,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    currencySymbol = getCurrencySymbol(houseConfig?.currencyCode ?: "$"),
                    onItemClick = { name ->
                        val user = spendByMember.find { it.fullName == name }
                        user?.userId?.let { onNavigateToUser(it) }
                    }
                )
            }
        }
    }
}

@Composable
private fun SpendingByCategorySection(
    summaryState: MonthlySummaryUiState,
    perDiemBillState: PerDiemBillUiState,
    chartColors: List<Color>,
    houseConfig: HouseConfig?,
    onNavigateToCategory: (String) -> Unit
) {
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
                    (it.category) to (it.totalAmount.toDouble())
                }

                SimplePieChart(
                    data = categoryData,
                    colors = chartColors,
                    modifier = Modifier.fillMaxWidth(),
                    currencySymbol = getCurrencySymbol(houseConfig?.currencyCode ?: "$"),
                    onItemClick = { category ->
                        onNavigateToCategory(category)
                    }
                )
            }
        }
    }
}

@Composable
private fun PerDiemItemizedSection(
    summaryState: MonthlySummaryUiState,
    perDiemBillState: PerDiemBillUiState,
    houseConfig: HouseConfig?
) {
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
                    PerDiemItemizedCard(item = item, houseConfig = houseConfig)
                }

                // Total per diem
                val summary = if (summaryState is MonthlySummaryUiState.Success) {
                    (summaryState as MonthlySummaryUiState.Success).summary
                } else null

                if (summary?.perDiemExpenses != null && summary.perDiemExpenses > BigDecimal.ZERO) {
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
                            text = "${getCurrencySymbol(houseConfig?.currencyCode ?: "$")}${String.format(Locale.getDefault(), "%.2f", summary.perDiemExpenses)}",
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

@Composable
private fun PerDiemItemizedCard(
    item: PerDiemBillItemized,
    houseConfig: HouseConfig?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.large,
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
                    text = "${getCurrencySymbol(houseConfig?.currencyCode ?: "$")}${String.format(Locale.getDefault(), "%.2f", item.totalAmount)}",
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
                        text = "Quantity: ${String.format(Locale.getDefault(), "%.1f", item.totalQuantity)} ${item.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "@${getCurrencySymbol(houseConfig?.currencyCode ?: "$")}${String.format(Locale.getDefault(), "%.2f", item.rate)}/${item.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
