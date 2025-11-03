package `in`.xroden.flockr.ui.screens.expenses

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.ui.components.cards.DataCard
import `in`.xroden.flockr.ui.components.cards.CompactDataCard
import `in`.xroden.flockr.ui.components.data.BalanceDisplay
import `in`.xroden.flockr.ui.components.data.BalanceSize
import `in`.xroden.flockr.ui.components.data.CompactStatDisplay
import `in`.xroden.flockr.ui.components.lists.ModernListItem
import `in`.xroden.flockr.ui.viewmodel.ExpenseViewModel

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
    onNavigateToReports: () -> Unit,
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    LaunchedEffect(houseId) {
        viewModel.loadExpenses(houseId)
        // TODO: Load dashboard stats
    }

    Scaffold(
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CompactDataCard(
                        label = "This Month",
                        value = "$0.00", // TODO: Real data
                        modifier = Modifier.weight(1f),
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                    CompactDataCard(
                        label = "Your Balance",
                        value = "$0.00", // TODO: Real data
                        modifier = Modifier.weight(1f),
                        accentColor = MaterialTheme.colorScheme.secondary
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
                    onClick = onNavigateToOneTimeExpenses,
                    showChevron = true
                )
            }

            item {
                ModernListItem(
                    title = "Recurring Expenses",
                    subtitle = "Manage monthly bills and subscriptions",
                    icon = Icons.Default.Refresh,
                    onClick = onNavigateToRecurringExpenses,
                    showChevron = true
                )
            }

            item {
                ModernListItem(
                    title = "Balances & IOUs",
                    subtitle = "See who owes what and settle up",
                    icon = Icons.Default.AccountBalance,
                    onClick = onNavigateToBalances,
                    showChevron = true
                )
            }

            item {
                ModernListItem(
                    title = "Per-Diem Tracking",
                    subtitle = "Track daily items like milk, bread, etc.",
                    icon = Icons.Default.DateRange,
                    onClick = onNavigateToPerDiem,
                    showChevron = true
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
                    onClick = onNavigateToReports,
                    showChevron = true
                )
            }

            // Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
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
