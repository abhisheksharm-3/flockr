package `in`.xroden.flockr.features.expenses.ui.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.features.expenses.model.RecurringExpense
import `in`.xroden.flockr.ui.theme.*
import `in`.xroden.flockr.features.expenses.domain.ExpenseUiState
import `in`.xroden.flockr.features.expenses.domain.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringExpensesScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAddBill: () -> Unit = {},
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val houseConfig by viewModel.houseConfig.collectAsState()
    val currencySymbol = houseConfig?.currencySymbol ?: "$"

    var showEditDialog by remember { mutableStateOf(false) }
    var selectedExpense by remember { mutableStateOf<RecurringExpense?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(houseId) {
        viewModel.loadRecurringExpenses(houseId)
        viewModel.loadHouseConfig(houseId)
    }

    // Delete confirmation dialog
    if (showDeleteDialog && selectedExpense != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete Recurring Bill?") },
            text = {
                Text("Are you sure you want to delete '${selectedExpense?.name}'? This will also delete all payment history for this bill.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedExpense?.let { expense ->
                            viewModel.deleteRecurringExpense(
                                expenseId = expense.id,
                                onSuccess = {
                                    showDeleteDialog = false
                                    selectedExpense = null
                                },
                                onError = { _: String ->
                                    // Error handling - could show snackbar
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets.systemBars,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Recurring Bills",
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddBill,
                icon = { Icon(Icons.Default.Add, "Add") },
                text = { Text("Add Bill") },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.large
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when (val state = uiState) {
            is ExpenseUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is ExpenseUiState.Success -> {
                if (state.recurringExpenses.isEmpty()) {
                    EmptyRecurringState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    )
                } else {
                    // Group expenses by frequency
                    val groupedExpenses = state.recurringExpenses.groupBy { it.frequency }
                    val frequencyOrder = listOf("daily", "weekly", "biweekly", "monthly", "quarterly", "semiannual", "annual", "custom")
                    val sortedGroups = frequencyOrder.mapNotNull { freq ->
                        groupedExpenses[freq]?.let { freq to it }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Overall Header
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                ),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "Recurring Bills",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "${state.recurringExpenses.size} active bill${if (state.recurringExpenses.size != 1) "s" else ""}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Receipt,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        // Grouped by frequency
                        sortedGroups.forEach { (frequency, expenses) ->
                            item {
                                FrequencySection(
                                    frequency = frequency,
                                    count = expenses.size,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                )
                            }

                            items(expenses) { expense ->
                                RecurringExpenseCard(
                                    expense = expense,
                                    currencySymbol = currencySymbol,
                                    onMarkAsPaid = {
                                        viewModel.markRecurringExpenseAsPaid(
                                            expenseId = expense.id,
                                            houseId = houseId,
                                            amount = expense.amount,
                                            paymentDate = java.time.LocalDate.now().toString()
                                        )
                                    },
                                    onEdit = {
                                        selectedExpense = expense
                                        showEditDialog = true
                                    },
                                    onDelete = {
                                        selectedExpense = expense
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }

                        // Bottom Spacer
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
            is ExpenseUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Error loading recurring expenses",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(
                            onClick = { viewModel.loadRecurringExpenses(houseId) },
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && selectedExpense != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Recurring Bill?") },
            text = {
                Text("Are you sure you want to delete \"${selectedExpense?.name}\"? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedExpense?.let { expense ->
                            viewModel.deleteRecurringExpense(
                                expenseId = expense.id,
                                onSuccess = {
                                    showDeleteDialog = false
                                    selectedExpense = null
                                },
                                onError = { _: String ->
                                    // Show error in snackbar or dialog
                                    showDeleteDialog = false
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Dialog (opens AddRecurringExpenseScreen in edit mode)
    // For now, we'll show a simple dialog. Later we can navigate to edit screen
    if (showEditDialog && selectedExpense != null) {
        EditRecurringExpenseDialog(
            expense = selectedExpense!!,
            onDismiss = {
                showEditDialog = false
                selectedExpense = null
            },
            onSave = { updatedExpense ->
                viewModel.updateRecurringExpense(
                    expenseId = updatedExpense.id,
                    name = updatedExpense.name,
                    amount = updatedExpense.amount,
                    dueDay = updatedExpense.dueDay,
                    category = updatedExpense.category,
                    frequency = updatedExpense.frequency,
                    customFrequencyDays = updatedExpense.customFrequencyDays,
                    reminderDaysBefore = updatedExpense.reminderDaysBefore,
                    reminderEnabled = updatedExpense.reminderEnabled,
                    notes = updatedExpense.notes,
                    onSuccess = {
                        showEditDialog = false
                        selectedExpense = null
                    },
                    onError = { error ->
                        // Show error
                        showEditDialog = false
                    }
                )
            }
        )
    }
}

@Composable
fun RecurringExpenseCard(
    expense: RecurringExpense,
    currencySymbol: String,
    onMarkAsPaid: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    // Determine status color and text
    val (statusColor, statusText, statusBg) = when (expense.dueStatus) {
        "overdue" -> Triple(
            androidx.compose.ui.graphics.Color(0xFFDC2626),
            "Overdue",
            androidx.compose.ui.graphics.Color(0xFFFEE2E2)
        )
        "due_today" -> Triple(
            androidx.compose.ui.graphics.Color(0xFFEA580C),
            "Due Today",
            androidx.compose.ui.graphics.Color(0xFFFFEDD5)
        )
        "upcoming" -> Triple(
            androidx.compose.ui.graphics.Color(0xFFD97706),
            "Due in ${expense.daysUntilDue} days",
            androidx.compose.ui.graphics.Color(0xFFFEF3C7)
        )
        "pending" -> Triple(
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Pending",
            MaterialTheme.colorScheme.surfaceVariant
        )
        else -> Triple(
            MaterialTheme.colorScheme.primary,
            "Scheduled",
            MaterialTheme.colorScheme.primaryContainer
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Icon, Name, Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon based on category
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(getCategoryColor(expense.category).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(expense.category),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = getCategoryColor(expense.category)
                        )
                    }

                    // Name and Category
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = expense.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = expense.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // More Menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        )
                    }
                }
            }

            // Amount Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Amount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$currencySymbol%.2f".format(expense.amount),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Status Badge
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = statusBg
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Divider
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Frequency
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Frequency",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = formatFrequency(expense.frequency, expense.customFrequencyDays),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Due Date
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Next Due",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = expense.nextDueDate?.let { formatDueDate(it) }
                            ?: "Day ${expense.dueDay}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Reminder Info (if enabled)
            if (expense.reminderEnabled) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Reminder ${expense.reminderDaysBefore} days before",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Notes (if present)
            expense.notes?.let { notes ->
                if (notes.isNotBlank()) {
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            // Last Paid Info
            expense.lastPaidDate?.let { lastPaid ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = androidx.compose.ui.graphics.Color(0xFF16A34A)
                    )
                    Text(
                        text = "Last paid on ${formatDueDate(lastPaid)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color(0xFF16A34A),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Action Button
            if (expense.dueStatus in listOf("overdue", "due_today", "upcoming")) {
                Button(
                    onClick = onMarkAsPaid,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Mark as Paid",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun FrequencySection(
    frequency: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    val (title, icon) = when (frequency.lowercase()) {
        "daily" -> "Daily Bills" to Icons.Default.Today
        "weekly" -> "Weekly Bills" to Icons.Default.CalendarViewWeek
        "biweekly" -> "Bi-Weekly Bills" to Icons.Default.CalendarMonth
        "monthly" -> "Monthly Bills" to Icons.Default.CalendarMonth
        "quarterly" -> "Quarterly Bills" to Icons.Default.DateRange
        "semiannual" -> "Semi-Annual Bills" to Icons.Default.DateRange
        "annual" -> "Annual Bills" to Icons.Default.DateRange
        "custom" -> "Custom Schedule" to Icons.Default.Schedule
        else -> frequency.replaceFirstChar { it.uppercase() } to Icons.Default.Receipt
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "($count)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Helper function to format frequency
private fun formatFrequency(frequency: String, customDays: Int?): String {
    return when (frequency.lowercase()) {
        "daily" -> "Daily"
        "weekly" -> "Weekly"
        "biweekly" -> "Bi-Weekly"
        "monthly" -> "Monthly"
        "quarterly" -> "Quarterly"
        "semiannual" -> "Semi-Annual"
        "annual" -> "Annual"
        "custom" -> customDays?.let { "Every $it days" } ?: "Custom"
        else -> frequency.capitalize()
    }
}

// Helper function to format due date
private fun formatDueDate(dateString: String): String {
    return try {
        val date = java.time.LocalDate.parse(dateString)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy")
        date.format(formatter)
    } catch (e: Exception) {
        dateString
    }
}

@Composable
fun EmptyRecurringState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(
                    2.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    MaterialTheme.shapes.large
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Repeat,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No Recurring Bills",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Add recurring expenses like rent, utilities, and subscriptions",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// Helper Functions (reused from OneTimeExpensesScreen)
private fun getCategoryColor(category: String): androidx.compose.ui.graphics.Color {
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

private fun getCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecurringExpenseDialog(
    expense: RecurringExpense,
    onDismiss: () -> Unit,
    onSave: (RecurringExpense) -> Unit
) {
    var name by remember { mutableStateOf(expense.name) }
    var amount by remember { mutableStateOf(expense.amount.toString()) }
    var dueDay by remember { mutableStateOf(expense.dueDay.toString()) }
    var category by remember { mutableStateOf(expense.category) }
    var frequency by remember { mutableStateOf(expense.frequency) }
    var customDays by remember { mutableStateOf(expense.customFrequencyDays?.toString() ?: "") }
    var reminderDays by remember { mutableStateOf(expense.reminderDaysBefore.toString()) }
    var reminderEnabled by remember { mutableStateOf(expense.reminderEnabled) }
    var notes by remember { mutableStateOf(expense.notes ?: "") }

    var expandedCategory by remember { mutableStateOf(false) }
    var expandedFrequency by remember { mutableStateOf(false) }

    val categories = listOf(
        "Utilities", "Rent", "Internet", "Insurance", "Subscription",
        "Groceries", "Food", "Entertainment", "Transport", "Shopping",
        "Healthcare", "Education", "Other"
    )

    val frequencies = listOf(
        "Daily", "Weekly", "Biweekly", "Monthly", "Quarterly",
        "Semiannual", "Annual", "Custom"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Text(
                    text = "Edit Recurring Bill",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Bill Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                // Due Day
                OutlinedTextField(
                    value = dueDay,
                    onValueChange = { dueDay = it },
                    label = { Text("Due Day (1-31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                // Category
                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = !expandedCategory }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = MaterialTheme.shapes.medium
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }

                // Frequency
                ExposedDropdownMenuBox(
                    expanded = expandedFrequency,
                    onExpandedChange = { expandedFrequency = !expandedFrequency }
                ) {
                    OutlinedTextField(
                        value = frequency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Frequency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFrequency) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = MaterialTheme.shapes.medium
                    )
                    ExposedDropdownMenu(
                        expanded = expandedFrequency,
                        onDismissRequest = { expandedFrequency = false }
                    ) {
                        frequencies.forEach { freq ->
                            DropdownMenuItem(
                                text = { Text(freq) },
                                onClick = {
                                    frequency = freq.lowercase()
                                    expandedFrequency = false
                                }
                            )
                        }
                    }
                }

                // Custom Days (if frequency is custom)
                if (frequency.lowercase() == "custom") {
                    OutlinedTextField(
                        value = customDays,
                        onValueChange = { customDays = it },
                        label = { Text("Custom Days") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )
                }

                // Reminder Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable Reminders",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it }
                    )
                }

                // Reminder Days (if enabled)
                if (reminderEnabled) {
                    OutlinedTextField(
                        value = reminderDays,
                        onValueChange = { reminderDays = it },
                        label = { Text("Remind Days Before") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    shape = MaterialTheme.shapes.medium
                )

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val updatedExpense = expense.copy(
                                name = name,
                                amount = amount.toDoubleOrNull() ?: expense.amount,
                                dueDay = dueDay.toIntOrNull() ?: expense.dueDay,
                                category = category,
                                frequency = frequency,
                                customFrequencyDays = if (frequency.lowercase() == "custom")
                                    customDays.toIntOrNull() else null,
                                reminderDaysBefore = reminderDays.toIntOrNull() ?: 3,
                                reminderEnabled = reminderEnabled,
                                notes = notes.ifBlank { null }
                            )
                            onSave(updatedExpense)
                        },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        enabled = name.isNotBlank() &&
                                 amount.toDoubleOrNull() != null &&
                                 dueDay.toIntOrNull() in 1..31
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
