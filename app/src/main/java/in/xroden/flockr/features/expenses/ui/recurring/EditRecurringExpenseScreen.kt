package `in`.xroden.flockr.features.expenses.ui.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.data.enums.ExpenseFrequency
import `in`.xroden.flockr.features.expenses.domain.RecurringExpenseViewModel
import `in`.xroden.flockr.features.expenses.model.RecurringExpense
import `in`.xroden.flockr.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRecurringExpenseScreen(
    houseId: String,
    expenseId: String,
    onNavigateBack: () -> Unit,
    viewModel: RecurringExpenseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Find the expense to edit
    val expense = (uiState as? `in`.xroden.flockr.features.expenses.domain.RecurringExpenseUiState.Success)
        ?.expenses?.find { it.id == expenseId }

    // If expense not found (or still loading), show loading or error
    if (expense == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        LaunchedEffect(houseId) {
            viewModel.loadRecurringExpenses(houseId)
        }
        return
    }

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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Recurring Bill", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val newAmount = amount.toBigDecimalOrNull() ?: expense.amount
                            viewModel.updateRecurringExpense(
                                houseId = houseId,
                                expenseId = expense.id,
                                name = name,
                                amount = newAmount,
                                dueDay = dueDay.toIntOrNull() ?: expense.dueDay,
                                category = category,
                                isActive = expense.isActive
                            )
                            onNavigateBack()
                        },
                        enabled = name.isNotBlank() &&
                                amount.toBigDecimalOrNull() != null &&
                                dueDay.toIntOrNull() in 1..31
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

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
                                frequency = freq
                                expandedFrequency = false
                            }
                        )
                    }
                }
            }

            // Custom Days
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
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = { reminderEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }

            // Reminder Days
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
                minLines = 3,
                maxLines = 5,
                shape = MaterialTheme.shapes.medium
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
