package `in`.xroden.flockr.features.expenses.ui.recurring

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.data.enums.ExpenseFrequency
import `in`.xroden.flockr.features.expenses.model.RecurringExpense
import `in`.xroden.flockr.features.expenses.presentation.RecurringExpenseViewModel
import `in`.xroden.flockr.features.expenses.presentation.RecurringExpenseUiState
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.ui.theme.*
import `in`.xroden.flockr.utils.getCurrencySymbol
import `in`.xroden.flockr.utils.getTodayInHouseTimezone
import `in`.xroden.flockr.utils.formatWithHouseConfig
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringExpensesScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAddBill: () -> Unit = {},
    onNavigateToEditBill: (String) -> Unit = {},
    onNavigateToHistory: (String, String) -> Unit = { _, _ -> },
    viewModel: RecurringExpenseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val houseConfig by viewModel.houseConfig.collectAsStateWithLifecycle()
    val currencySymbol = remember(houseConfig) {
        houseConfig?.getCurrencySymbol() ?: "$"
    }

    var selectedExpense by remember { mutableStateOf<RecurringExpense?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(houseId) {
        viewModel.loadRecurringExpenses(houseId)
        viewModel.loadHouseConfig(houseId)
    }

    // Delete confirmation dialog
    if (showDeleteDialog && selectedExpense != null) {
        DeleteBillConfirmationDialog(
            billName = selectedExpense?.name,
            onConfirm = {
                selectedExpense?.let { expense ->
                    viewModel.deleteRecurringExpense(houseId, expense.id)
                    showDeleteDialog = false
                    selectedExpense = null
                }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = { RecurringExpensesTopBar(onNavigateBack) },
        floatingActionButton = { AddBillFab(onNavigateToAddBill) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        RecurringExpensesContent(
            state = uiState,
            padding = padding,
            currencySymbol = currencySymbol,
            houseConfig = houseConfig,
            onMarkAsPaid = { expense ->
                val houseTimezone = houseConfig?.timezone
                val tz = houseTimezone?.let { runCatching { TimeZone.of(it) }.getOrNull() } ?: TimeZone.currentSystemDefault()
                viewModel.markAsPaid(
                    houseId = houseId,
                    expenseId = expense.id,
                    amount = expense.amount,
                    paymentDate = kotlin.time.Clock.System.todayIn(tz)
                )
            },
            onEdit = onNavigateToEditBill,
            onDeleteRequest = { expense ->
                selectedExpense = expense
                showDeleteDialog = true
            },
            onHistory = onNavigateToHistory,
            onRetry = { viewModel.loadRecurringExpenses(houseId) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringExpensesTopBar(onNavigateBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text("Recurring Bills", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}

@Composable
private fun AddBillFab(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icon(Icons.Default.Add, "Add") },
        text = { Text("Add Bill", fontWeight = FontWeight.SemiBold) },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
private fun DeleteBillConfirmationDialog(
    billName: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Delete Recurring Bill?", fontWeight = FontWeight.Bold) },
        text = { Text("Are you sure you want to delete '$billName'? This will also delete all payment history for this bill.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = MaterialTheme.shapes.medium
            ) { Text("Delete", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = MaterialTheme.shapes.medium) { Text("Cancel", fontWeight = FontWeight.SemiBold) }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
private fun RecurringExpensesContent(
    state: RecurringExpenseUiState,
    padding: PaddingValues,
    currencySymbol: String,
    houseConfig: HouseConfig?,
    onMarkAsPaid: (RecurringExpense) -> Unit,
    onEdit: (String) -> Unit,
    onDeleteRequest: (RecurringExpense) -> Unit,
    onHistory: (String, String) -> Unit,
    onRetry: () -> Unit
) {
    when (state) {
        is RecurringExpenseUiState.Loading -> {
            `in`.xroden.flockr.ui.components.loading.ListScreenSkeleton(
                modifier = Modifier.padding(padding)
            )
        }
        is RecurringExpenseUiState.Success -> {
            if (state.expenses.isEmpty()) {
                EmptyRecurringState(Modifier.fillMaxSize().padding(padding))
            } else {
                RecurringExpensesList(
                    expenses = state.expenses,
                    padding = padding,
                    currencySymbol = currencySymbol,
                    houseConfig = houseConfig,
                    onMarkAsPaid = onMarkAsPaid,
                    onEdit = onEdit,
                    onDeleteRequest = onDeleteRequest,
                    onHistory = onHistory
                )
            }
        }
        is RecurringExpenseUiState.Error -> {
            RecurringExpensesErrorState(padding = padding, onRetry = onRetry)
        }
    }
}

@Composable
private fun RecurringExpensesList(
    expenses: List<RecurringExpense>,
    padding: PaddingValues,
    currencySymbol: String,
    houseConfig: HouseConfig?,
    onMarkAsPaid: (RecurringExpense) -> Unit,
    onEdit: (String) -> Unit,
    onDeleteRequest: (RecurringExpense) -> Unit,
    onHistory: (String, String) -> Unit
) {
    val sortedGroups = remember(expenses) {
        val groupedExpenses = expenses.groupBy { it.frequency }
        val frequencyOrder = listOf(
            ExpenseFrequency.DAILY,
            ExpenseFrequency.WEEKLY,
            ExpenseFrequency.BIWEEKLY,
            ExpenseFrequency.MONTHLY,
            ExpenseFrequency.QUARTERLY,
            ExpenseFrequency.SEMIANNUAL,
            ExpenseFrequency.ANNUAL,
            ExpenseFrequency.CUSTOM
        )
        frequencyOrder.mapNotNull { freq -> groupedExpenses[freq]?.let { freq to it } }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Overall Header
        item {
            RecurringExpensesSummaryHeader(count = expenses.size)
        }

        // Grouped by frequency
        sortedGroups.forEach { (frequency, groupExpenses) ->
            item {
                FrequencySection(
                    frequency = frequency.name,
                    count = groupExpenses.size,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(
                items = groupExpenses,
                key = { it.id }
            ) { expense ->
                RecurringExpenseCard(
                    expense = expense,
                    currencySymbol = currencySymbol,
                    houseConfig = houseConfig,
                    onMarkAsPaid = { onMarkAsPaid(expense) },
                    onEdit = { onEdit(expense.id) },
                    onDelete = { onDeleteRequest(expense) },
                    onHistory = { onHistory(expense.id, expense.name) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun RecurringExpensesSummaryHeader(count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Recurring Bills", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    text = "$count active bill${if (count != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Receipt, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun RecurringExpensesErrorState(padding: PaddingValues, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.Error, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
            Text("Error loading recurring expenses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            Button(
                onClick = onRetry,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Refresh, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Retry", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "groceries", "food" -> CategoryGreen
        "utilities", "services" -> CategoryBlue
        "entertainment" -> CategoryPurple
        "transport" -> CategoryYellow
        "shopping" -> CategoryPink
        "rent", "housing" -> CategoryOrange
        "healthcare" -> CategoryTeal
        "education" -> CategoryIndigo
        else -> CategoryBlue
    }
}

private fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "groceries", "food" -> Icons.Default.ShoppingCart
        "utilities", "services" -> Icons.Default.Build
        "entertainment" -> Icons.Default.Movie
        "transport" -> Icons.Default.DirectionsCar
        "shopping" -> Icons.Default.ShoppingBag
        "rent", "housing" -> Icons.Default.Home
        "healthcare" -> Icons.Default.LocalHospital
        "education" -> Icons.Default.School
        else -> Icons.Default.Receipt
    }
}

@Composable
private fun EmptyRecurringState(modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(
            modifier = Modifier.size(80.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Receipt, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(24.dp))
        Text("No Recurring Bills", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Add recurring bills to track them automatically",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun FrequencySection(frequency: String, count: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = frequency.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.extraSmall) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun RecurringExpenseCard(
    expense: RecurringExpense,
    currencySymbol: String,
    houseConfig: HouseConfig?,
    onMarkAsPaid: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = houseConfig.getTodayInHouseTimezone()
    
    val isPaidThisPeriod = remember(expense.lastPaidDate, expense.frequency) {
        if (expense.lastPaidDate == null) return@remember false
        when (expense.frequency) {
             ExpenseFrequency.MONTHLY -> expense.lastPaidDate.month == today.month && expense.lastPaidDate.year == today.year
             ExpenseFrequency.WEEKLY -> (today.toEpochDays() - expense.lastPaidDate.toEpochDays()) < 7
             ExpenseFrequency.ANNUAL -> expense.lastPaidDate.year == today.year
             else -> false 
        }
    }

    val nextDueDate = getNextDueDate(expense.dueDay, today)
    // Use epoch-day difference so year boundaries (Dec -> Jan) don't yield a negative "Overdue".
    val daysUntilDue = (nextDueDate.toEpochDays() - today.toEpochDays()).toInt()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            // Top row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.medium).background(getCategoryColor(expense.category).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(getCategoryIcon(expense.category), null, tint = getCategoryColor(expense.category), modifier = Modifier.size(24.dp))
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(expense.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(expense.category, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Text("$currencySymbol${expense.amount}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(12.dp))

            // Due Date & Status
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Event, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Due: ${nextDueDate.formatWithHouseConfig(houseConfig)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isPaidThisPeriod) {
                    Text("Paid", style = MaterialTheme.typography.labelSmall, color = CategoryGreen, fontWeight = FontWeight.Bold)
                } else if (daysUntilDue < 0) {
                    Text("Overdue", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                } else if (daysUntilDue <= 3) {
                    Text("Due Soon", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isPaidThisPeriod) {
                    Button(
                        onClick = onMarkAsPaid,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Paid", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                     OutlinedButton(
                        onClick = {},
                        enabled = false, 
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        border = BorderStroke(1.dp, CategoryGreen)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp), tint = CategoryGreen)
                        Spacer(Modifier.width(4.dp))
                        Text("Done", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge, color = CategoryGreen)
                    }
                }

                OutlinedButton(
                    onClick = onHistory,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.History, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("History", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

private fun getNextDueDate(dueDay: Int, today: kotlinx.datetime.LocalDate): kotlinx.datetime.LocalDate {
    val currentMonthDue = kotlinx.datetime.LocalDate(
        today.year,
        today.month,
        dueDay.coerceAtMost(today.month.length(today.year % 4 == 0 && (today.year % 100 != 0 || today.year % 400 == 0)))
    )

    return if (currentMonthDue < today) {
        // Calculate next month by adding 1 month to the first day of current month
        val firstOfNextMonth = kotlinx.datetime.LocalDate(today.year, today.month, 1)
            .plus(1, kotlinx.datetime.DateTimeUnit.MONTH)
        val nextMonth = firstOfNextMonth.month
        val nextYear = firstOfNextMonth.year
        kotlinx.datetime.LocalDate(
            nextYear,
            nextMonth,
            dueDay.coerceAtMost(nextMonth.length(nextYear % 4 == 0 && (nextYear % 100 != 0 || nextYear % 400 == 0)))
        )
    } else {
        currentMonthDue
    }
}

private fun kotlinx.datetime.Month.length(isLeapYear: Boolean): Int {
    return when (this) {
        kotlinx.datetime.Month.FEBRUARY -> if (isLeapYear) 29 else 28
        kotlinx.datetime.Month.APRIL, kotlinx.datetime.Month.JUNE, kotlinx.datetime.Month.SEPTEMBER, kotlinx.datetime.Month.NOVEMBER -> 30
        else -> 31
    }
}
