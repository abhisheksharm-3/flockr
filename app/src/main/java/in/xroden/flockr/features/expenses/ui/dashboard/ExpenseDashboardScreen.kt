package `in`.xroden.flockr.features.expenses.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import `in`.xroden.flockr.features.expenses.model.OneTimeExpense
import `in`.xroden.flockr.features.expenses.domain.OneTimeExpenseViewModel
import `in`.xroden.flockr.features.expenses.domain.MonthlySummaryViewModel
import `in`.xroden.flockr.features.expenses.domain.OneTimeExpenseUiState
import `in`.xroden.flockr.features.expenses.domain.MonthlySummaryUiState
import `in`.xroden.flockr.features.expenses.model.SpendByMember
import `in`.xroden.flockr.ui.theme.CategoryBlue
import `in`.xroden.flockr.ui.theme.CategoryGreen
import `in`.xroden.flockr.ui.theme.CategoryOrange
import `in`.xroden.flockr.ui.theme.CategoryPurple
import `in`.xroden.flockr.ui.theme.CategoryRed
import `in`.xroden.flockr.utils.getCurrencySymbol
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.TimeZone
import java.math.BigDecimal

/**
 * Central Finance Dashboard - Hub for all finance features
 * Optimized for performance and clean UI logic.
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
    onNavigateToExpenseDetail: (String) -> Unit,
    expenseViewModel: OneTimeExpenseViewModel = hiltViewModel(),
    summaryViewModel: MonthlySummaryViewModel = hiltViewModel()
) {
    val expenseState by expenseViewModel.expenseState.collectAsState()
    val houseConfig by expenseViewModel.houseConfig.collectAsState()
    val summaryState by summaryViewModel.summaryState.collectAsState()
    
    val currentUserId = expenseViewModel.getCurrentUserId()
    
    // Derived state for heavy calculations
    val currencySymbol by remember {
        derivedStateOf { houseConfig?.getCurrencySymbol() ?: "$" }
    }

    val summaryData by remember {
        derivedStateOf {
            when (val state = summaryState) {
                is MonthlySummaryUiState.Success -> Pair(state.summary, state.spendByMember)
                else -> Pair(null, emptyList<SpendByMember>())
            }
        }
    }

    LaunchedEffect(houseId) {
        expenseViewModel.loadExpenses(houseId)
        expenseViewModel.loadHouseConfig(houseId)
        
        // Calculate current month securely
        runCatching {
            val currentMonth = kotlin.time.Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date.toString().substring(0, 7) + "-01"
            summaryViewModel.loadMonthlySummary(houseId, currentMonth)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Finance", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
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
            // Header Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                val (monthlySummary, spendByMember) = summaryData
                val totalThisMonth = monthlySummary?.totalExpenses?.toDouble() ?: 0.0
                val userSpending = spendByMember.find { it.userId == currentUserId }?.totalSpent ?: BigDecimal.ZERO

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FinanceStatCard(
                        label = "THIS MONTH",
                        value = "$currencySymbol${"%.2f".format(totalThisMonth)}",
                        modifier = Modifier.weight(1f),
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                    FinanceStatCard(
                        label = "YOUR EXPENSE",
                        value = "$currencySymbol${"%.2f".format(userSpending)}",
                        modifier = Modifier.weight(1f),
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        isPositive = true
                    )
                }
            }

            // Feature Navigation Title
            item {
                Text(
                    text = "Manage",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Feature Cards
            item {
                FinanceFeatureCard(
                    title = "One-Time Expenses",
                    subtitle = "Add and track individual expenses",
                    icon = Icons.Default.ShoppingCart,
                    accentColor = CategoryBlue,
                    onClick = onNavigateToOneTimeExpenses
                )
            }

            item {
                FinanceFeatureCard(
                    title = "Recurring Expenses",
                    subtitle = "Manage monthly bills and subscriptions",
                    icon = Icons.Default.Refresh,
                    accentColor = CategoryPurple,
                    onClick = onNavigateToRecurringExpenses
                )
            }

            item {
                FinanceFeatureCard(
                    title = "Balances & IOUs",
                    subtitle = "See who owes what and settle up",
                    icon = Icons.Default.AccountBalance,
                    accentColor = CategoryOrange,
                    onClick = onNavigateToBalances
                )
            }

            item {
                FinanceFeatureCard(
                    title = "Add Per Diem Entry",
                    subtitle = "Quick log daily usage items",
                    icon = Icons.Default.Add,
                    accentColor = CategoryGreen,
                    onClick = onNavigateToQuickPerDiem
                )
            }

            item {
                FinanceFeatureCard(
                    title = "Per-Diem Configuration",
                    subtitle = "Manage per-diem items and rates",
                    icon = Icons.Default.Settings,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    onClick = onNavigateToPerDiem
                )
            }

            // Reports Title
            item {
                Text(
                    text = "Reports",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                FinanceFeatureCard(
                    title = "Monthly Reports",
                    subtitle = "View spending breakdown and summaries",
                    icon = Icons.Default.Assessment,
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToReports
                )
            }

            // Recent Expenses Section
            item {
                Text(
                    text = "Recent Expenses",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            when (val state = expenseState) {
                is OneTimeExpenseUiState.Loading -> {
                    items(3) {
                        `in`.xroden.flockr.ui.components.loading.SkeletonExpenseCard()
                    }
                }
                is OneTimeExpenseUiState.Error -> {
                    item {
                        // Error fallback UI without logs
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
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
                                    text = "Could not load recent expenses",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                is OneTimeExpenseUiState.Success -> {
                    val recentExpenses = state.expenses.take(5)
                    if (recentExpenses.isEmpty()) {
                        item {
                            EmptyRecentExpensesCard()
                        }
                    } else {
                        items(
                            items = recentExpenses,
                            key = { it.id } // Stable key
                        ) { expense ->
                            RecentExpenseCard(
                                expense = expense,
                                currencySymbol = currencySymbol,
                                onClick = { onNavigateToExpenseDetail(expense.id) }
                            )
                        }

                        item {
                            ViewAllExpensesButton(onClick = onNavigateToOneTimeExpenses)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun EmptyRecentExpensesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "No expenses yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Start by adding your first expense",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ViewAllExpensesButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        Text(
            "View All Expenses",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun FinanceStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accentColor: Color,
    isPositive: Boolean = true
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}

@Composable
private fun FinanceFeatureCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(accentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = expense.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = expense.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = expense.date.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "$currencySymbol${"%.2f".format(expense.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
