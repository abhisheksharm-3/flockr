package `in`.xroden.flockr.ui.screens.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.data.model.RecurringExpense
import `in`.xroden.flockr.ui.viewmodel.RecurringExpenseViewModel
import `in`.xroden.flockr.ui.viewmodel.RecurringExpenseUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringExpensesScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: RecurringExpenseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(houseId) {
        viewModel.loadRecurringExpenses(houseId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recurring Bills") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Add Recurring Bill")
            }
        }
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "No recurring bills yet",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Text(
                                "Add bills like rent, utilities, etc.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.expenses) { expense ->
                            RecurringExpenseCard(
                                expense = expense,
                                onMarkPaid = {
                                    viewModel.markAsPaid(expense.id, expense.houseId, expense.amount)
                                },
                                onToggleActive = {
                                    viewModel.toggleActive(expense.id, expense.houseId, !expense.isActive)
                                },
                                onDelete = {
                                    viewModel.deleteRecurringExpense(expense.id, expense.houseId)
                                }
                            )
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
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddRecurringExpenseDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name, amount, dueDay, category ->
                viewModel.addRecurringExpense(houseId, name, amount, dueDay, category)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun RecurringExpenseCard(
    expense: RecurringExpense,
    onMarkPaid: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (!expense.isActive) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        } else {
            CardDefaults.cardColors()
        }
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = expense.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Due on day ${expense.dueDay} of each month",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    if (!expense.isActive) {
                        Text(
                            text = "Inactive",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                Text(
                    text = "$${String.format("%.2f", expense.amount)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onMarkPaid,
                    modifier = Modifier.weight(1f),
                    enabled = expense.isActive
                ) {
                    Text("Mark Paid")
                }
                OutlinedButton(
                    onClick = onToggleActive,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (expense.isActive) "Pause" else "Activate")
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Recurring Bill") },
            text = { Text("Are you sure you want to delete ${expense.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringExpenseDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Double, Int, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dueDay by remember { mutableStateOf("1") }
    var category by remember { mutableStateOf("") }

    val categories = listOf("Rent", "Utilities", "Internet", "Phone", "Insurance", "Subscription", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Recurring Bill") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Bill Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("e.g., Rent") }
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    prefix = { Text("$") }
                )
                OutlinedTextField(
                    value = dueDay,
                    onValueChange = {
                        if (it.isEmpty() || (it.toIntOrNull()?.let { day -> day in 1..31 } == true)) {
                            dueDay = it
                        }
                    },
                    label = { Text("Due Day (1-31)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    val dueDayValue = dueDay.toIntOrNull()
                    if (amountValue != null && dueDayValue != null && name.isNotBlank() && category.isNotBlank()) {
                        onAdd(name, amountValue, dueDayValue, category)
                    }
                },
                enabled = name.isNotBlank() &&
                         amount.toDoubleOrNull() != null &&
                         dueDay.toIntOrNull() != null &&
                         category.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

