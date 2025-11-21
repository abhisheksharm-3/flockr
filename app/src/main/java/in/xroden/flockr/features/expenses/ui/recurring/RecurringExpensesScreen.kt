package `in`.xroden.flockr.features.expenses.ui.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import `in`.xroden.flockr.data.enums.ExpenseFrequency
import `in`.xroden.flockr.features.expenses.model.RecurringExpense
import `in`.xroden.flockr.features.expenses.domain.RecurringExpenseViewModel
import `in`.xroden.flockr.features.expenses.domain.RecurringExpenseUiState
import `in`.xroden.flockr.features.expenses.domain.ExpenseViewModel
import `in`.xroden.flockr.ui.theme.*
import `in`.xroden.flockr.ui.util.getCurrencySymbol
// FIX: Kotlinx DateTime Imports
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.util.Locale



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringExpensesScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAddBill: () -> Unit = {},
    // FIX: Use RecurringExpenseViewModel for recurring logic
    viewModel: RecurringExpenseViewModel = hiltViewModel(),
    // Keep ExpenseViewModel only for House Config if needed, otherwise move config logic to RecurringVM
    expenseViewModel: ExpenseViewModel = hiltViewModel()
) {
    // FIX: Collect recurring state specifically
    val uiState by viewModel.uiState.collectAsState()
    val houseConfig by expenseViewModel.houseConfig.collectAsState()
    val currencySymbol = getCurrencySymbol(houseConfig?.currencyCode ?: "$")

    var showEditDialog by remember { mutableStateOf(false) }
    var selectedExpense by remember { mutableStateOf<RecurringExpense?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(houseId) {
        viewModel.loadRecurringExpenses(houseId)
        expenseViewModel.loadHouseConfig(houseId)
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
                            // FIX: Removed callbacks (onSuccess/onError) as they don't exist in VM
                            viewModel.deleteRecurringExpense(houseId, expense.id)
                            showDeleteDialog = false
                            selectedExpense = null
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
        contentWindowInsets = WindowInsets.systemBars,
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
                shape = MaterialTheme.shapes.medium
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when (val state = uiState) {
            is RecurringExpenseUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is RecurringExpenseUiState.Success -> {
                if (state.expenses.isEmpty()) {
                    EmptyRecurringState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    )
                } else {
                    val recurringExpenses = state.expenses

                    // Group expenses by frequency
                    val groupedExpenses = recurringExpenses.groupBy { it.frequency }
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
                                shape = MaterialTheme.shapes.medium
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
                                            text = "${recurringExpenses.size} active bill${if (recurringExpenses.size != 1) "s" else ""}",
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
                                    frequency = frequency.name,
                                    count = expenses.size,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                )
                            }

                            items(expenses) { expense ->
                                RecurringExpenseCard(
                                    expense = expense,
                                    currencySymbol = currencySymbol,
                                    onMarkAsPaid = {
                                        viewModel.markAsPaid(
                                            houseId = houseId,
                                            expenseId = expense.id,
                                            amount = expense.amount,
                                            // FIX: Use Kotlinx Date logic
                                            paymentDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
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
            is RecurringExpenseUiState.Error -> {
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

    // Edit Dialog
    if (showEditDialog && selectedExpense != null) {
        EditRecurringExpenseDialog(
            expense = selectedExpense!!,
            onDismiss = {
                showEditDialog = false
                selectedExpense = null
            },
            onSave = { updatedExpense ->
                // FIX: Correct parameters for updateRecurringExpense (using BigDecimal logic)
                viewModel.updateRecurringExpense(
                    houseId = houseId,
                    expenseId = updatedExpense.id,
                    name = updatedExpense.name,
                    amount = updatedExpense.amount,
                    dueDay = updatedExpense.dueDay,
                    category = updatedExpense.category,
                    isActive = updatedExpense.isActive
                )
                showEditDialog = false
                selectedExpense = null
            }
        )
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
    // FIX: Initialize frequency as Enum
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

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
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
                        // FIX: Use toDisplayName helper
                        value = frequency.toDisplayName(),
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
                        ExpenseFrequency.entries.forEach { freq ->
                            DropdownMenuItem(
                                text = { Text(freq.toDisplayName()) },
                                onClick = {
                                    // FIX: Assign Enum directly
                                    frequency = freq
                                    expandedFrequency = false
                                }
                            )
                        }
                    }
                }

                // Custom Days (if frequency is custom)
                // FIX: Compare Enum
                if (frequency == ExpenseFrequency.CUSTOM) {
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
                            // FIX: Use toBigDecimalOrNull for amount conversion
                            val newAmount = amount.toBigDecimalOrNull() ?: expense.amount

                            val updatedExpense = expense.copy(
                                name = name,
                                amount = newAmount,
                                dueDay = dueDay.toIntOrNull() ?: expense.dueDay,
                                category = category,
                                frequency = frequency,
                                customFrequencyDays = if (frequency == ExpenseFrequency.CUSTOM)
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
                                amount.toBigDecimalOrNull() != null &&
                                dueDay.toIntOrNull() in 1..31
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

// Helper function reused
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

@Composable
fun EmptyRecurringState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Receipt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Recurring Bills",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add recurring bills to track them automatically",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FrequencySection(
    frequency: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = frequency.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "($count)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RecurringExpenseCard(
    expense: RecurringExpense,
    currencySymbol: String,
    onMarkAsPaid: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(getCategoryColor(expense.category).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(expense.category),
                            contentDescription = null,
                            tint = getCategoryColor(expense.category),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = expense.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = expense.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "$currencySymbol${expense.amount}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onMarkAsPaid,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Paid")
                }

                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(0.5f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                }

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(0.5f),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
