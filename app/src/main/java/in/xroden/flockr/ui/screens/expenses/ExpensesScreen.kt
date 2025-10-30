package `in`.xroden.flockr.ui.screens.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.data.model.OneTimeExpense
import `in`.xroden.flockr.ui.viewmodel.ExpenseViewModel
import `in`.xroden.flockr.ui.viewmodel.ExpenseUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onAddExpense: () -> Unit,
    onNavigateToBalances: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(houseId) {
        viewModel.loadExpenses(houseId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expenses") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToBalances) {
                        Icon(Icons.Default.ThumbUp, "Balances")
                    }
                    IconButton(onClick = onNavigateToDashboard) {
                        Icon(Icons.Default.MailOutline, "Dashboard")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpense) {
                Icon(Icons.Default.Add, "Add Expense")
            }
        }
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
                if (state.expenses.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No expenses yet. Add one!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.expenses) { expense ->
                            ExpenseCard(expense = expense)
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Error: ${state.message}",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadExpenses(houseId) }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseCard(expense: OneTimeExpense) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = expense.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$${"%.2f".format(expense.amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${expense.category} • ${expense.date}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            expense.notes?.let { notes ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onExpenseAdded: () -> Unit,
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var date by remember {
        mutableStateOf(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE))
    }
    var category by remember { mutableStateOf("Groceries") }
    var notes by remember { mutableStateOf("") }
    var expandedCategory by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val categories = listOf("Groceries", "Utilities", "Rent", "Internet", "Entertainment", "Other")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Expense") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Expense Name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                enabled = !isLoading
            )

            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Date (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            ExposedDropdownMenuBox(
                expanded = expandedCategory,
                onExpandedChange = { expandedCategory = !expandedCategory && !isLoading }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    enabled = !isLoading
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

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                enabled = !isLoading
            )

            Button(
                onClick = {
                    amount.toDoubleOrNull()?.let { amt ->
                        isLoading = true
                        viewModel.createExpense(
                            houseId = houseId,
                            name = name,
                            amount = amt,
                            date = date,
                            category = category,
                            notes = notes.takeIf { it.isNotBlank() },
                            splits = null,
                            onSuccess = {
                                isLoading = false
                                onExpenseAdded()
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && amount.toDoubleOrNull() != null && date.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Add Expense")
                }
            }
        }
    }
}
