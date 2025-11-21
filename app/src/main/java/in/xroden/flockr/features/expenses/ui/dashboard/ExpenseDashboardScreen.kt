package `in`.xroden.flockr.features.expenses.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.features.expenses.model.OneTimeExpense
import `in`.xroden.flockr.ui.components.cards.DataCard
import `in`.xroden.flockr.ui.components.cards.CompactDataCard
import `in`.xroden.flockr.ui.components.data.BalanceDisplay
import `in`.xroden.flockr.ui.components.data.BalanceSize
import `in`.xroden.flockr.ui.components.data.CompactStatDisplay
import `in`.xroden.flockr.ui.components.lists.ModernListItem
import `in`.xroden.flockr.features.expenses.domain.ExpenseViewModel
import `in`.xroden.flockr.features.expenses.domain.OneTimeExpenseUiState
import `in`.xroden.flockr.features.expenses.domain.BalanceUiState
import `in`.xroden.flockr.features.expenses.domain.MonthlySummaryUiState
import kotlinx.datetime.toLocalDateTime

/**
 * Central Finance Dashboard - Hub for all finance features
 * Inspired by fold.money's data-rich, clean design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDashboardScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToOneTimeExpenses: () -> Unit,
    onNavigateToRecurringExpenses: () -> Unit,
    onNavigateToBalances: () -> Unit,
    onNavigateToPerDiem: () -> Unit,
    onNavigateToQuickPerDiem: () -> Unit,
    onNavigateToReports: () -> Unit,
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    val expenseState by viewModel.expenseState.collectAsState()
    val balanceState by viewModel.balanceState.collectAsState()
    val houseConfig by viewModel.houseConfig.collectAsState()
    val summaryState by viewModel.summaryState.collectAsState()
    val currencySymbol = houseConfig?.getCurrencySymbol() ?: "$"

    LaunchedEffect(houseId) {
        viewModel.loadExpenses(houseId)
        viewModel.loadBalances(houseId)
        viewModel.loadHouseConfig(houseId)
        // Load monthly summary for current month
        val currentMonth = kotlinx.datetime.Clock.System.now()
            .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
            .date.toString().substring(0, 7) + "-01"  // Get YYYY-MM-01 format
        viewModel.loadMonthlySummary(houseId, currentMonth)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Finance",
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
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Section
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Finance Hub",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Manage expenses, split bills, and track spending",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Stats Row
            item {
                // Use monthly summary which includes all expense types (one-time, recurring, per diem)
                val monthlySummary = when (val state = summaryState) {
                    is MonthlySummaryUiState.Success -> state.summary
                    else -> null
                }
                val totalThisMonth = monthlySummary?.totalExpenses?.toDouble() ?: 0.0

                val currentUserId = viewModel.getCurrentUserId()
                val balances = when (val state = balanceState) {
                    is BalanceUiState.Success -> state.balances
                    else -> emptyList()
                }
                val userBalance = balances.find { it.userId == currentUserId }?.balance ?: java.math.BigDecimal.ZERO

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CompactDataCard(
                        label = "This Month",
                        value = "$currencySymbol${"%.2f".format(totalThisMonth)}",
                        modifier = Modifier.weight(1f),
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                    CompactDataCard(
                        label = "Your Balance",
                        value = "$currencySymbol${"%.2f".format(kotlin.math.abs(userBalance.toDouble()))}",
                        modifier = Modifier.weight(1f),
                        accentColor = if (userBalance >= java.math.BigDecimal.ZERO) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                    )
                }
            }

            // Feature Navigation Cards
            item {
                Text(
                    text = "Manage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                ModernListItem(
                    title = "One-Time Expenses",
                    subtitle = "Add and track individual expenses",
                    icon = Icons.Default.ShoppingCart,
                    onClick = onNavigateToOneTimeExpenses
                )
            }

            item {
                ModernListItem(
                    title = "Recurring Expenses",
                    subtitle = "Manage monthly bills and subscriptions",
                    icon = Icons.Default.Refresh,
                    onClick = onNavigateToRecurringExpenses
                )
            }

            item {
                ModernListItem(
                    title = "Balances & IOUs",
                    subtitle = "See who owes what and settle up",
                    icon = Icons.Default.AccountBalance,
                    onClick = onNavigateToBalances
                )
            }

            item {
                ModernListItem(
                    title = "Add Per Diem Entry",
                    subtitle = "Quick log daily usage items",
                    icon = Icons.Default.Add,
                    onClick = onNavigateToQuickPerDiem
                )
            }

            item {
                ModernListItem(
                    title = "Per-Diem Configuration",
                    subtitle = "Manage per-diem items and rates",
                    icon = Icons.Default.Settings,
                    onClick = onNavigateToPerDiem
                )
            }

            item {
                Text(
                    text = "Reports",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                ModernListItem(
                    title = "Monthly Reports",
                    subtitle = "View spending breakdown and summaries",
                    icon = Icons.Default.Assessment,
                    onClick = onNavigateToReports
                )
            }

            // Recent Expenses Section
            item {
                Text(
                    text = "Recent Expenses",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            when (val state = expenseState) {
                is OneTimeExpenseUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is OneTimeExpenseUiState.Error -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
                is OneTimeExpenseUiState.Success -> {
                    val recentExpenses = state.expenses.take(5)
                    if (recentExpenses.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Receipt,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "No expenses yet",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Start by adding your first expense",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(recentExpenses) { expense ->
                            RecentExpenseCard(
                                expense = expense,
                                currencySymbol = currencySymbol,
                                onClick = { onNavigateToOneTimeExpenses() }
                            )
                        }

                        // View All button
                        item {
                            TextButton(
                                onClick = onNavigateToOneTimeExpenses,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("View All Expenses")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "All household members can view and add expenses. Split bills automatically and settle up with ease.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecentExpenseCard(
    expense: OneTimeExpense,
    currencySymbol: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - expense details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = expense.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = expense.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = expense.date.toString(), // LocalDate.toString() already formats as YYYY-MM-DD
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right side - amount
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "$currencySymbol${"%.2f".format(expense.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
