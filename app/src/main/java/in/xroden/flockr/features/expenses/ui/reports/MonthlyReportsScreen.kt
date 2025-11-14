package `in`.xroden.flockr.features.expenses.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.ui.components.cards.SectionCard
import `in`.xroden.flockr.ui.components.charts.SimpleBarChart
import `in`.xroden.flockr.ui.components.charts.SimplePieChart
import `in`.xroden.flockr.features.expenses.domain.ExpenseViewModel
import `in`.xroden.flockr.utils.Constants
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportsScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    val perDiemItemized by viewModel.perDiemBillItemized.collectAsState()
    val spendByMember by viewModel.spendByMember.collectAsState()
    val spendByCategory by viewModel.spendByCategory.collectAsState()
    val houseConfig by viewModel.houseConfig.collectAsState()
    val monthlySummary by viewModel.monthlySummary.collectAsState()

    // Load per diem configs to get categories
    val perDiemViewModel: `in`.xroden.flockr.ui.viewmodel.PerDiemViewModel = hiltViewModel()
    val perDiemConfigs by perDiemViewModel.configs.collectAsState()

    LaunchedEffect(houseId, selectedMonth) {
        val monthStr = selectedMonth.format(DateTimeFormatter.ofPattern(Constants.DateFormats.YEAR_MONTH))
        viewModel.loadMonthlySummary(houseId, monthStr)
        viewModel.loadPerDiemBillItemized(houseId, monthStr)
        viewModel.loadSpendByCategory(houseId, monthStr)
        viewModel.loadSpendByMember(houseId, monthStr)
        viewModel.loadHouseConfig(houseId)
        perDiemViewModel.loadConfigs(houseId)
    }

    // Chart colors from constants
    val chartColors = Constants.ChartColors.PIE_CHART_COLORS.map { Color(it) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Monthly Report",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
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
            contentPadding = PaddingValues(Constants.UI.STANDARD_PADDING_DP.dp),
            verticalArrangement = Arrangement.spacedBy(Constants.UI.CARD_SPACING_DP.dp)
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
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

            // Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                            Text(
                                text = "${houseConfig?.currencySymbol ?: "$"}%.2f".format(monthlySummary?.totalExpenses ?: 0.0),
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
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "One-Time",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "${houseConfig?.currencySymbol ?: "$"}%.2f".format(monthlySummary?.oneTimeExpenses ?: 0.0),
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
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "${houseConfig?.currencySymbol ?: "$"}%.2f".format(monthlySummary?.recurringExpenses ?: 0.0),
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
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "${houseConfig?.currencySymbol ?: "$"}%.2f".format(monthlySummary?.perDiemExpenses ?: 0.0),
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
                                (it.fullName ?: "Unknown") to (it.totalSpent ?: 0.0)
                            }.toMutableMap()

                            // Add per diem as "House" spending
                            val perDiemTotal = perDiemItemized.sumOf { it.totalAmount ?: 0.0 }
                            if (perDiemTotal > 0) {
                                memberData["House (Per Diem)"] = perDiemTotal
                            }

                            SimpleBarChart(
                                data = memberData,
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary,
                                currencySymbol = houseConfig?.currencySymbol ?: "$"
                            )
                        }
                    }
                }
            }

            // Spending by Category
            item {
                SectionCard(title = "Spending by Category") {
                    if (spendByCategory.isEmpty() && perDiemItemized.isEmpty()) {
                        Text(
                            text = "No expenses recorded this month",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Build category data - merge regular expenses and per diem
                            val categoryData = spendByCategory.associate {
                                (it.category ?: "Uncategorized") to (it.totalAmount ?: 0.0)
                            }.toMutableMap()

                            // Create map of item names to categories from configs
                            val itemNameToCategory = perDiemConfigs.associate { config ->
                                config.itemName to config.category
                            }

                            // Group per diem by category using config lookup and add to categoryData
                            perDiemItemized.groupBy { item ->
                                itemNameToCategory[item.itemName] ?: "Per Diem"
                            }.forEach { (category, items) ->
                                val total = items.sumOf { it.totalAmount ?: 0.0 }
                                if (total > 0) {
                                    categoryData[category] = (categoryData[category] ?: 0.0) + total
                                }
                            }

                            SimplePieChart(
                                data = categoryData,
                                colors = chartColors,
                                modifier = Modifier.fillMaxWidth(),
                                currencySymbol = houseConfig?.currencySymbol ?: "$"
                            )
                        }
                    }
                }
            }

            // Per Diem Itemized
            item {
                SectionCard(title = "Per Diem Usage by Item") {
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
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp)
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
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "${houseConfig?.currencySymbol ?: "$"}${String.format("%.2f", item.totalAmount)}",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
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
                                                text = "@${houseConfig?.currencySymbol ?: "$"}${String.format("%.2f", item.rate)}/${item.unit}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Total per diem
                            if (monthlySummary?.perDiemExpenses != null && monthlySummary!!.perDiemExpenses > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
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
                                        text = "${houseConfig?.currencySymbol ?: "$"}${String.format("%.2f", monthlySummary!!.perDiemExpenses)}",
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
