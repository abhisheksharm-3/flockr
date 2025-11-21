package `in`.xroden.flockr.features.expenses.ui.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.data.enums.ExpenseDueStatus
import `in`.xroden.flockr.features.expenses.model.RecurringExpense
import `in`.xroden.flockr.features.expenses.domain.ExpenseViewModel
import `in`.xroden.flockr.features.expenses.domain.OneTimeExpenseUiState
import `in`.xroden.flockr.features.expenses.domain.RecurringExpenseViewModel
import `in`.xroden.flockr.features.expenses.domain.RecurringExpenseUiState
import `in`.xroden.flockr.ui.util.getCurrencySymbol

// FIX: Correct Kotlinx DateTime Imports
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDate

// Java Time Imports (Only for YearMonth logic which is missing in Kotlinx)
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Modern Bills Screen with Calendar View
 * Inspired by best practices in bill management UIs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAddBill: () -> Unit = {},
    onNavigateToHistory: (String, String) -> Unit = { _, _ -> },
    viewModel: ExpenseViewModel = hiltViewModel(),
    recurringViewModel: RecurringExpenseViewModel = hiltViewModel()
) {
    val uiState by recurringViewModel.uiState.collectAsState()
    val houseConfig by viewModel.houseConfig.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }

    LaunchedEffect(houseId) {
        recurringViewModel.loadRecurringExpenses(houseId)
        viewModel.loadHouseConfig(houseId)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            BillsTopBar(
                onNavigateBack = onNavigateBack,
                selectedMonth = selectedMonth,
                onMonthChange = { selectedMonth = it }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddBill,
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, "Add Bill", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs: Recurring / Pay Bills
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Recurring", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Pay Bills", fontWeight = FontWeight.SemiBold) }
                )
            }

            when (val state = uiState) {
                is RecurringExpenseUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is RecurringExpenseUiState.Success -> {
                    val expenses = state.expenses
                    val currencySymbol = getCurrencySymbol(houseConfig?.currencyCode ?: "USD")

                    if (selectedTab == 0) {
                        // Recurring Bills View with Calendar
                        RecurringBillsContent(
                            expenses = expenses,
                            selectedMonth = selectedMonth,
                            currencySymbol = currencySymbol,
                            onExpenseClick = { /* TODO: Navigate to details */ },
                            onHistoryClick = { expense -> onNavigateToHistory(expense.id, expense.name) },
                            onMarkAsPaid = { expense ->
                                recurringViewModel.markAsPaid(
                                    houseId = houseId,
                                    expenseId = expense.id,
                                    amount = expense.amount,
                                    paymentDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
                                )
                            }
                        )
                    } else {
                        // Pay Bills View (One-time bills to pay)
                        PayBillsContent(
                            expenses = expenses.filter {
                                it.dueStatus == ExpenseDueStatus.OVERDUE ||
                                        it.dueStatus == ExpenseDueStatus.DUE_TODAY ||
                                        it.dueStatus == ExpenseDueStatus.DUE_SOON
                            },
                            currencySymbol = currencySymbol,
                            onMarkAsPaid = { expense ->
                                recurringViewModel.markAsPaid(
                                    houseId = houseId,
                                    expenseId = expense.id,
                                    amount = expense.amount,
                                    paymentDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
                                )
                            }
                        )
                    }
                }
                is RecurringExpenseUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            state.message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsTopBar(
    onNavigateBack: () -> Unit,
    selectedMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
    ) {
        // Top row with back button and title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }

            Text(
                "Bills",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Row {
                IconButton(onClick = { /* TODO: Filter */ }) {
                    Icon(Icons.Default.FilterList, "Filter")
                }
                IconButton(onClick = { /* TODO: More options */ }) {
                    Icon(Icons.Default.MoreVert, "More")
                }
            }
        }

        // Month selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                selectedMonth.format(DateTimeFormatter.ofPattern("MMMM ''yy")),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Row {
                IconButton(onClick = { onMonthChange(selectedMonth.minusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month")
                }
                IconButton(onClick = { onMonthChange(selectedMonth.plusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month")
                }
            }
        }
    }
}

@Composable
fun RecurringBillsContent(
    expenses: List<RecurringExpense>,
    selectedMonth: YearMonth,
    currencySymbol: String,
    onExpenseClick: (RecurringExpense) -> Unit,
    onHistoryClick: (RecurringExpense) -> Unit,
    onMarkAsPaid: (RecurringExpense) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Calendar Grid
        BillsCalendarGrid(
            selectedMonth = selectedMonth,
            expenses = expenses,
            onDateClick = { /* TODO */ }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Summary Stats
        BillsSummaryRow(
            expenses = expenses,
            currencySymbol = currencySymbol
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Filter tabs
        BillsFilterChips(
            onFilterSelected = { /* TODO */ }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Bills List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp) // FAB space
        ) {
            items(expenses) { expense ->
                ModernBillCard(
                    expense = expense,
                    currencySymbol = currencySymbol,
                    onMarkAsPaid = { onMarkAsPaid(expense) },
                    onHistoryClick = { onHistoryClick(expense) },
                    onClick = { onExpenseClick(expense) }
                )
            }
        }
    }
}

@Composable
fun BillsCalendarGrid(
    selectedMonth: YearMonth,
    expenses: List<RecurringExpense>,
    onDateClick: (Int) -> Unit
) {
    val today = java.time.LocalDate.now() // Use Java Time for Calendar math
    val daysInMonth = selectedMonth.lengthOfMonth()
    val firstDayOfWeek = selectedMonth.atDay(1).dayOfWeek.value % 7

    // Group expenses by due day
    val expensesByDay = expenses.groupBy { it.dueDay }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Weekday headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Text(
                        day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar days
            val weeks = (daysInMonth + firstDayOfWeek + 6) / 7
            for (week in 0 until weeks) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (dayOfWeek in 0..6) {
                        val dayNumber = week * 7 + dayOfWeek - firstDayOfWeek + 1

                        if (dayNumber in 1..daysInMonth) {
                            val dateExpenses = expensesByDay[dayNumber] ?: emptyList()
                            val isToday = selectedMonth.year == today.year &&
                                    selectedMonth.monthValue == today.monthValue &&
                                    dayNumber == today.dayOfMonth

                            CalendarDay(
                                day = dayNumber,
                                isToday = isToday,
                                hasOverdue = dateExpenses.any { it.dueStatus == ExpenseDueStatus.OVERDUE },
                                hasDueSoon = dateExpenses.any { it.dueStatus == ExpenseDueStatus.DUE_SOON || it.dueStatus == ExpenseDueStatus.DUE_TODAY },
                                hasPaid = dateExpenses.any { it.lastPaidDate != null },
                                onClick = { onDateClick(dayNumber) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                if (week < weeks - 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun CalendarDay(
    day: Int,
    isToday: Boolean,
    hasOverdue: Boolean,
    hasDueSoon: Boolean,
    hasPaid: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        hasOverdue -> Color(0xFFFFEBEE)
        hasDueSoon -> Color(0xFFFFF9C4)
        hasPaid -> Color(0xFFE8F5E9)
        else -> Color.Transparent
    }

    val borderColor = when {
        isToday -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(backgroundColor)
            .border(
                width = if (isToday) 2.dp else 0.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.extraSmall
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isToday -> MaterialTheme.colorScheme.primary
                    hasOverdue -> Color(0xFFD32F2F)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            // Indicator dots
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                if (hasOverdue) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD32F2F))
                    )
                }
                if (hasDueSoon && !hasOverdue) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFA726))
                    )
                }
                if (hasPaid) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF66BB6A))
                    )
                }
            }
        }
    }
}

@Composable
fun BillsSummaryRow(
    expenses: List<RecurringExpense>,
    currencySymbol: String
) {
    val upcoming = expenses.filter { it.dueStatus == ExpenseDueStatus.UPCOMING }
    val overdue = expenses.filter { it.dueStatus == ExpenseDueStatus.OVERDUE }
    val paid = expenses.filter { it.lastPaidDate != null }

    val upcomingTotal = upcoming.sumOf { it.amount.toDouble() }
    val overdueTotal = overdue.sumOf { it.amount.toDouble() }
    val paidTotal = paid.sumOf { it.amount.toDouble() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryCard(
            label = "Upcoming",
            amount = upcomingTotal,
            currencySymbol = currencySymbol,
            color = Color(0xFF2196F3),
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            label = "Overdue",
            amount = overdueTotal,
            currencySymbol = currencySymbol,
            color = Color(0xFFD32F2F),
            modifier = Modifier.weight(1f)
        )
        SummaryCard(
            label = "Paid",
            amount = paidTotal,
            currencySymbol = currencySymbol,
            color = Color(0xFF66BB6A),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SummaryCard(
    label: String,
    amount: Double,
    currencySymbol: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "$currencySymbol${String.format("%.2f", amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BillsFilterChips(
    onFilterSelected: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(listOf("Upcoming", "Overdue", "Paid")) { filter ->
            FilterChip(
                selected = false,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter) },
                shape = MaterialTheme.shapes.large
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernBillCard(
    expense: RecurringExpense,
    currencySymbol: String,
    onMarkAsPaid: () -> Unit,
    onHistoryClick: () -> Unit,
    onClick: () -> Unit
) {
    val statusColor = when (expense.dueStatus) {
        ExpenseDueStatus.OVERDUE -> Color(0xFFD32F2F)
        ExpenseDueStatus.DUE_TODAY -> Color(0xFFFFA726)
        ExpenseDueStatus.DUE_SOON -> Color(0xFFFDD835)
        ExpenseDueStatus.UPCOMING -> Color(0xFF66BB6A)
        else -> Color.Gray
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Icon and Info
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(statusColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = statusColor
                    )
                }

                Column {
                    Text(
                        expense.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        formatDueStatusMessage(expense.dueStatus, expense.nextDueDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                    Text(
                        getFrequencyDescription(expense.frequency),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right: Amount and due date
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    "${currencySymbol}${String.format("%.2f", expense.amount.toDouble())}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (expense.nextDueDate != null) {
                    val dueDate = try {
                        val dateString = expense.nextDueDate.toString()
                        dateString.substring(8, 10).toInt() // Extract day
                    } catch (e: Exception) {
                        expense.dueDay
                    }

                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                dueDate.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            val month = try {
                                val parsedDate = expense.nextDueDate!! // It is not null here
                                // FIX: Explicitly specify Locale to avoid overload ambiguity
                                parsedDate.month.name.take(3).uppercase(Locale.getDefault())
                            } catch (e: Exception) {
                                Clock.System.todayIn(TimeZone.currentSystemDefault()).month.name.take(3).uppercase(Locale.getDefault())
                            }
                            Text(
                                month,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
        
        // Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onHistoryClick) {
                Icon(Icons.Default.History, "History", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("History")
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            var showConfirmation by remember { mutableStateOf(false) }
            
            Button(
                onClick = { showConfirmation = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Check, "Mark Paid", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Mark Paid")
            }

            if (showConfirmation) {
                AlertDialog(
                    onDismissRequest = { showConfirmation = false },
                    title = { Text("Mark as Paid?") },
                    text = { Text("Confirm payment of ${currencySymbol}${String.format("%.2f", expense.amount.toDouble())} for ${expense.name}") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onMarkAsPaid()
                                showConfirmation = false
                            }
                        ) {
                            Text("Confirm")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmation = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PayBillsContent(
    expenses: List<RecurringExpense>,
    currencySymbol: String,
    onMarkAsPaid: (RecurringExpense) -> Unit
) {
    if (expenses.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF66BB6A)
                )
                Text(
                    "All bills are paid!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "You're all caught up",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            item {
                Text(
                    "Cleared 0 / ${expenses.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(expenses) { expense ->
                PayBillCard(
                    expense = expense,
                    currencySymbol = currencySymbol,
                    onMarkAsPaid = { onMarkAsPaid(expense) }
                )
            }
        }
    }
}

@Composable
fun PayBillCard(
    expense: RecurringExpense,
    currencySymbol: String,
    onMarkAsPaid: () -> Unit
) {
    var showConfirmation by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column {
                    Text(
                        expense.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${currencySymbol}${String.format("%.2f", expense.amount.toDouble())}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        formatDueStatusMessage(expense.dueStatus, expense.nextDueDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = when (expense.dueStatus) {
                            ExpenseDueStatus.OVERDUE -> Color(0xFFD32F2F)
                            ExpenseDueStatus.DUE_TODAY -> Color(0xFFFFA726)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Button(
                onClick = { showConfirmation = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF66BB6A)
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Check, "Mark as Paid", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Paid")
            }
        }
    }

    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text("Mark as Paid?") },
            text = { Text("Confirm payment of ${currencySymbol}${String.format("%.2f", expense.amount.toDouble())} for ${expense.name}") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onMarkAsPaid()
                        showConfirmation = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Helper Functions
private fun formatDueStatusMessage(dueStatus: ExpenseDueStatus?, nextDueDate: kotlinx.datetime.LocalDate?): String {
    val daysUntilDue = nextDueDate?.let {
        // FIX: Use Kotlinx DateTime logic
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        (it.toEpochDays() - today.toEpochDays())
    }

    return when (dueStatus) {
        ExpenseDueStatus.OVERDUE -> "Overdue"
        ExpenseDueStatus.DUE_TODAY -> "Due Today"
        ExpenseDueStatus.DUE_SOON -> "Due in ${daysUntilDue ?: 0} days"
        ExpenseDueStatus.UPCOMING -> "Upcoming in ${daysUntilDue ?: 0} days"
        else -> "Not scheduled"
    }
}

private fun getFrequencyDescription(frequency: `in`.xroden.flockr.data.enums.ExpenseFrequency): String {
    return when (frequency) {
        `in`.xroden.flockr.data.enums.ExpenseFrequency.DAILY -> "Daily"
        `in`.xroden.flockr.data.enums.ExpenseFrequency.WEEKLY -> "Weekly"
        `in`.xroden.flockr.data.enums.ExpenseFrequency.BIWEEKLY -> "Bi-weekly"
        `in`.xroden.flockr.data.enums.ExpenseFrequency.MONTHLY -> "Monthly"
        `in`.xroden.flockr.data.enums.ExpenseFrequency.QUARTERLY -> "Quarterly"
        `in`.xroden.flockr.data.enums.ExpenseFrequency.SEMIANNUAL -> "Semi-annual"
        `in`.xroden.flockr.data.enums.ExpenseFrequency.ANNUAL -> "Annual"
        `in`.xroden.flockr.data.enums.ExpenseFrequency.CUSTOM -> "Custom"
    }
}