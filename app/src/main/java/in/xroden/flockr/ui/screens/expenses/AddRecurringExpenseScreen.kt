package `in`.xroden.flockr.ui.screens.expenses

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import `in`.xroden.flockr.ui.components.cards.SectionCard
import `in`.xroden.flockr.ui.viewmodel.ExpenseViewModel
import `in`.xroden.flockr.ui.viewmodel.RecurringExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringExpenseScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onExpenseAdded: () -> Unit,
    viewModel: RecurringExpenseViewModel = hiltViewModel(),
    expenseViewModel: ExpenseViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dueDay by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Utilities") }
    var frequency by remember { mutableStateOf("monthly") }
    var customFrequencyDays by remember { mutableStateOf("") }
    var reminderDaysBefore by remember { mutableStateOf("3") }
    var reminderEnabled by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }
    var expandedCategory by remember { mutableStateOf(false) }
    var expandedFrequency by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val categories = listOf(
        "Utilities", "Rent", "Internet", "Insurance", "Subscription",
        "Groceries", "Food", "Entertainment", "Transport", "Shopping",
        "Healthcare", "Education", "Other"
    )

    val frequencies = listOf(
        "Daily", "Weekly", "Biweekly", "Monthly", "Quarterly",
        "Semiannual", "Annual", "Custom"
    )
    val houseConfig by expenseViewModel.houseConfig.collectAsState()
    val currencySymbol = houseConfig?.currencySymbol ?: "$"

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(houseId) {
        expenseViewModel.loadHouseConfig(houseId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Add Recurring Bill",
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
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Text(
                text = "New Recurring Bill",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Bill Details
            SectionCard(title = "Bill Details") {
                // Bill Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Bill Name *") },
                    placeholder = { Text("e.g., Electric Bill, Rent") },
                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount *") },
                    prefix = { Text(currencySymbol) },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                // Due Day
                OutlinedTextField(
                    value = dueDay,
                    onValueChange = { dueDay = it },
                    label = { Text("Due Day of Month *") },
                    placeholder = { Text("1-31") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    supportingText = { Text("Day of month when bill is due (1-31)") }
                )

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = !expandedCategory && !isLoading }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category *") },
                        leadingIcon = { Icon(Icons.Default.Category, null) },
                        trailingIcon = { 
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) 
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
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

                // Frequency Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedFrequency,
                    onExpandedChange = { expandedFrequency = !expandedFrequency && !isLoading }
                ) {
                    OutlinedTextField(
                        value = frequency.capitalize(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Frequency *") },
                        leadingIcon = { Icon(Icons.Default.Repeat, null) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFrequency)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
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

                // Custom Frequency Days (if frequency is custom)
                if (frequency.lowercase() == "custom") {
                    OutlinedTextField(
                        value = customFrequencyDays,
                        onValueChange = { customFrequencyDays = it },
                        label = { Text("Custom Days *") },
                        placeholder = { Text("e.g., 45 for every 45 days") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        supportingText = { Text("Number of days between occurrences") }
                    )
                }
            }

            // Reminder Settings
            SectionCard(title = "Reminder Settings") {
                // Reminder Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Reminders",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Get notified before bill is due",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it },
                        enabled = !isLoading
                    )
                }

                // Reminder Days Before (if enabled)
                if (reminderEnabled) {
                    OutlinedTextField(
                        value = reminderDaysBefore,
                        onValueChange = { reminderDaysBefore = it },
                        label = { Text("Remind Days Before") },
                        placeholder = { Text("3") },
                        leadingIcon = { Icon(Icons.Default.Notifications, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        supportingText = { Text("Number of days before due date") }
                    )
                }
            }

            // Notes Section
            SectionCard(title = "Notes (Optional)") {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Additional Notes") },
                    placeholder = { Text("e.g., Account number, payment instructions") },
                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )
            }

            // Submit Button
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    val day = dueDay.toIntOrNull()
                    
                    if (name.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please enter a bill name")
                        }
                        return@Button
                    }

                    if (amt == null) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please enter a valid amount")
                        }
                        return@Button
                    }
                    
                    if (day == null || day !in 1..31) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please enter a valid day (1-31)")
                        }
                        return@Button
                    }

                    // Validate custom frequency days if frequency is custom
                    val customDays = if (frequency.lowercase() == "custom") {
                        customFrequencyDays.toIntOrNull()?.also {
                            if (it <= 0) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Custom days must be greater than 0")
                                }
                                return@Button
                            }
                        } ?: run {
                            scope.launch {
                                snackbarHostState.showSnackbar("Please enter custom days for custom frequency")
                            }
                            return@Button
                        }
                    } else null

                    val reminderDays = if (reminderEnabled) {
                        reminderDaysBefore.toIntOrNull() ?: 3
                    } else 3

                    isLoading = true

                    viewModel.createRecurringExpense(
                        houseId = houseId,
                        name = name,
                        amount = amt,
                        dueDay = day,
                        category = category,
                        frequency = frequency,
                        customFrequencyDays = customDays,
                        reminderDaysBefore = reminderDays,
                        reminderEnabled = reminderEnabled,
                        notes = notes.ifBlank { null },
                        onSuccess = {
                            isLoading = false
                            onExpenseAdded()
                        },
                        onError = { errorMessage ->
                            isLoading = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Error: $errorMessage")
                            }
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isLoading && name.isNotBlank() && amount.toDoubleOrNull() != null && dueDay.toIntOrNull() != null,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Add Recurring Bill",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

