package `in`.xroden.flockr.features.expenses.ui.recurring

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
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.ui.components.cards.SectionCard
import `in`.xroden.flockr.features.expenses.domain.ExpenseViewModel
import `in`.xroden.flockr.features.expenses.domain.RecurringExpenseViewModel
import androidx.compose.foundation.clickable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    
    // New fields for prepay and custom date
    var prepayEnabled by remember { mutableStateOf(false) }
    var firstPaymentDate by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    // New fields for bill splitting
    var selectedMembers by remember { mutableStateOf<List<String>>(emptyList()) }
    var splitType by remember { mutableStateOf("equal") }
    var customAmounts by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var houseMembers by remember { mutableStateOf<List<MemberWithProfile>>(emptyList()) }

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
        // Load house members for splitting
        try {
            val members = expenseViewModel.getHouseMembers(houseId)
            houseMembers = members
        } catch (e: Exception) {
            android.util.Log.e("AddRecurringExpense", "Error loading members", e)
        }
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
                    shape = MaterialTheme.shapes.large,
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
                    shape = MaterialTheme.shapes.large,
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
                    shape = MaterialTheme.shapes.large,
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
                        shape = MaterialTheme.shapes.large,
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
                        shape = MaterialTheme.shapes.large,
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
                        shape = MaterialTheme.shapes.large,
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
                        shape = MaterialTheme.shapes.large,
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
                    shape = MaterialTheme.shapes.large,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )
            }
            
            // Payment Options Section
            SectionCard(title = "Payment Options") {
                // Prepay Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Allow Prepayment",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Enable paying this bill before due date",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = prepayEnabled,
                        onCheckedChange = { prepayEnabled = it },
                        enabled = !isLoading
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Custom First Payment Date
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isLoading) { showDatePicker = true }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "First Payment Date (Optional)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                firstPaymentDate ?: "Use default schedule",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.DateRange, "Select date")
                    }
                }
            }
            
            // Split Bill Section (Optional)
            if (houseMembers.size > 1) {
                SectionCard(title = "Split Bill (Optional)") {
                    Text(
                        "Select members to split this bill with",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Member selection chips
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        houseMembers.forEach { member ->
                            FilterChip(
                                selected = selectedMembers.contains(member.userId),
                                onClick = {
                                    selectedMembers = if (selectedMembers.contains(member.userId)) {
                                        selectedMembers - member.userId
                                    } else {
                                        selectedMembers + member.userId
                                    }
                                },
                                label = { Text(member.fullName ?: member.userId) },
                                enabled = !isLoading
                            )
                        }
                    }
                    
                    // Split type selector (if members selected)
                    if (selectedMembers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            "Split Method",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FilterChip(
                                selected = splitType == "equal",
                                onClick = { splitType = "equal" },
                                label = { Text("Equal Split") },
                                enabled = !isLoading,
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = splitType == "custom",
                                onClick = { splitType = "custom" },
                                label = { Text("Custom Amounts") },
                                enabled = !isLoading,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        // Custom amounts input (if custom selected)
                        if (splitType == "custom") {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                "Enter amount for each member",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            selectedMembers.forEach { memberId ->
                                val memberName = houseMembers.find { it.userId == memberId }?.fullName ?: memberId
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        memberName,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    OutlinedTextField(
                                        value = customAmounts[memberId]?.toString() ?: "",
                                        onValueChange = { 
                                            val amt = it.toDoubleOrNull()
                                            if (amt != null) {
                                                customAmounts = customAmounts + (memberId to amt)
                                            } else if (it.isEmpty()) {
                                                customAmounts = customAmounts - memberId
                                            }
                                        },
                                        label = { Text("Amount") },
                                        prefix = { Text(currencySymbol) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.width(140.dp),
                                        enabled = !isLoading,
                                        singleLine = true,
                                        shape = MaterialTheme.shapes.medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
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

                    // Validate custom amounts if custom split selected
                    if (splitType == "custom" && selectedMembers.isNotEmpty()) {
                        val totalCustom = customAmounts.values.sum()
                        if (totalCustom > amt) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Custom amounts exceed total bill amount")
                            }
                            return@Button
                        }
                    }

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
                        splitWith = if (selectedMembers.isNotEmpty()) selectedMembers else null,
                        splitType = if (selectedMembers.isNotEmpty()) splitType else null,
                        splitAmounts = if (splitType == "custom" && selectedMembers.isNotEmpty()) customAmounts else null,
                        prepayEnabled = prepayEnabled,
                        firstPaymentDate = firstPaymentDate,
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
                shape = MaterialTheme.shapes.medium
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
    
    // DatePicker dialog for custom first payment date
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = LocalDate.ofEpochDay(millis / (1000 * 60 * 60 * 24))
                        firstPaymentDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

