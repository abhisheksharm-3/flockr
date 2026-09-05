package `in`.xroden.flockr.features.expenses.ui.recurring

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat

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
import `in`.xroden.flockr.ui.components.forms.FormSectionCard
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import `in`.xroden.flockr.features.expenses.presentation.RecurringExpenseViewModel
import `in`.xroden.flockr.data.enums.ExpenseFrequency
import `in`.xroden.flockr.data.enums.ExpenseSplitType
import `in`.xroden.flockr.utils.getCurrencySymbol
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.utils.rememberHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringExpenseScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onExpenseAdded: () -> Unit,
    viewModel: RecurringExpenseViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dueDay by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Utilities") }

    var frequency by remember { mutableStateOf(ExpenseFrequency.MONTHLY) }

    var customFrequencyDays by remember { mutableStateOf("") }
    var reminderDaysBefore by remember { mutableStateOf("3") }
    var reminderEnabled by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }
    var expandedCategory by remember { mutableStateOf(false) }
    var expandedFrequency by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var prepayEnabled by remember { mutableStateOf(false) }
    var firstPaymentDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDueDayPicker by remember { mutableStateOf(false) }

    var selectedMembers by remember { mutableStateOf<List<String>>(emptyList()) }
    var splitType by remember { mutableStateOf("equal") }
    var customAmounts by remember { mutableStateOf<Map<String, java.math.BigDecimal>>(emptyMap()) }
    var houseMembers by remember { mutableStateOf<List<MemberWithProfile>>(emptyList()) }

    val categories = listOf(
        "Utilities", "Rent", "Internet", "Insurance", "Subscription",
        "Groceries", "Food", "Entertainment", "Transport", "Shopping",
        "Healthcare", "Education", "Other"
    )

    val houseConfig by viewModel.houseConfig.collectAsStateWithLifecycle()
    val currencySymbol = getCurrencySymbol(houseConfig?.currencyCode ?: "$")

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(houseId) {
        viewModel.loadHouseConfig(houseId)
        runCatching {
            houseMembers = viewModel.getHouseMembers(houseId)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Add Recurring Bill", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BillDetailsSection(
                name = name,
                onNameChange = { name = it },
                amount = amount,
                onAmountChange = { amount = it },
                currencySymbol = currencySymbol,
                dueDay = dueDay,
                onDueDayChange = { dueDay = it },
                onShowDueDayPicker = { showDueDayPicker = true },
                category = category,
                onCategoryChange = { category = it },
                categories = categories,
                expandedCategory = expandedCategory,
                onExpandedCategoryChange = { expandedCategory = it },
                frequency = frequency,
                onFrequencyChange = { frequency = it },
                expandedFrequency = expandedFrequency,
                onExpandedFrequencyChange = { expandedFrequency = it },
                customFrequencyDays = customFrequencyDays,
                onCustomFrequencyDaysChange = { customFrequencyDays = it },
                isLoading = isLoading
            )

            ReminderSettingsSection(
                reminderEnabled = reminderEnabled,
                onReminderEnabledChange = { reminderEnabled = it },
                reminderDaysBefore = reminderDaysBefore,
                onReminderDaysBeforeChange = { reminderDaysBefore = it },
                isLoading = isLoading
            )

            NotesSection(
                notes = notes,
                onNotesChange = { notes = it },
                isLoading = isLoading
            )

            PaymentOptionsSection(
                prepayEnabled = prepayEnabled,
                onPrepayEnabledChange = { prepayEnabled = it },
                firstPaymentDate = firstPaymentDate,
                onShowDatePicker = { showDatePicker = true },
                isLoading = isLoading
            )

            if (houseMembers.size > 1) {
                SplitBillSection(
                    houseMembers = houseMembers,
                    selectedMembers = selectedMembers,
                    onSelectedMembersChange = { selectedMembers = it },
                    splitType = splitType,
                    onSplitTypeChange = { splitType = it },
                    customAmounts = customAmounts,
                    onCustomAmountsChange = { customAmounts = it },
                    currencySymbol = currencySymbol,
                    isLoading = isLoading
                )
            }

            SubmitButton(
                isLoading = isLoading,
                enabled = !isLoading && name.isNotBlank() && amount.toBigDecimalOrNull() != null && dueDay.toIntOrNull() != null,
                onClick = {
                    val amt = amount.toBigDecimalOrNull()
                    val day = dueDay.toIntOrNull()

                    if (name.isBlank()) { scope.launch { snackbarHostState.showSnackbar("Please enter a bill name") }; return@SubmitButton }
                    if (amt == null) { scope.launch { snackbarHostState.showSnackbar("Please enter a valid amount") }; return@SubmitButton }
                    if (day == null || day !in 1..31) { scope.launch { snackbarHostState.showSnackbar("Please enter a valid day (1-31)") }; return@SubmitButton }

                    val customDays = if (frequency == ExpenseFrequency.CUSTOM) {
                        customFrequencyDays.toIntOrNull()?.also {
                            if (it <= 0) { scope.launch { snackbarHostState.showSnackbar("Custom days must be greater than 0") }; return@SubmitButton }
                        } ?: run {
                            scope.launch { snackbarHostState.showSnackbar("Please enter custom days for custom frequency") }
                            return@SubmitButton
                        }
                    } else null

                    val reminderDays = if (reminderEnabled) reminderDaysBefore.toIntOrNull() ?: 3 else 3

                    if (splitType == "custom" && selectedMembers.isNotEmpty()) {
                        val totalCustom = customAmounts.values.fold(java.math.BigDecimal.ZERO) { acc, v -> acc + v }
                        // Must equal the total, else the split is invalid and mark-as-paid will
                        // later reject it (the one-time expense use case enforces sum == amount).
                        if (totalCustom.compareTo(amt) != 0) {
                            scope.launch { snackbarHostState.showSnackbar("Custom amounts must add up to the total bill amount") }
                            return@SubmitButton
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
                        splitType = if (selectedMembers.isNotEmpty()) ExpenseSplitType.valueOf(splitType.uppercase()) else null,
                        splitAmounts = if (splitType == "custom" && selectedMembers.isNotEmpty()) customAmounts else null,
                        prepayEnabled = prepayEnabled,
                        firstPaymentDate = firstPaymentDate
                    )

                    isLoading = false
                    onExpenseAdded()
                }
            )
        }
    }

    if (showDatePicker) {
        FirstPaymentDatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { date -> firstPaymentDate = date }
        )
    }

    if (showDueDayPicker) {
        DueDayPickerDialog(
            onDismissRequest = { showDueDayPicker = false },
            onDaySelected = { day -> dueDay = day }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillDetailsSection(
    name: String,
    onNameChange: (String) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    currencySymbol: String,
    dueDay: String,
    onDueDayChange: (String) -> Unit,
    onShowDueDayPicker: () -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    categories: List<String>,
    expandedCategory: Boolean,
    onExpandedCategoryChange: (Boolean) -> Unit,
    frequency: ExpenseFrequency,
    onFrequencyChange: (ExpenseFrequency) -> Unit,
    expandedFrequency: Boolean,
    onExpandedFrequencyChange: (Boolean) -> Unit,
    customFrequencyDays: String,
    onCustomFrequencyDaysChange: (String) -> Unit,
    isLoading: Boolean
) {
    FormSectionCard(icon = Icons.Default.Edit, title = "Bill Details") {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Bill Name *") },
            placeholder = { Text("e.g., Electric Bill, Rent") },
            leadingIcon = { Icon(Icons.Default.Edit, null) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )

        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            label = { Text("Amount *") },
            prefix = { Text(currencySymbol) },
            leadingIcon = { Icon(Icons.Default.AttachMoney, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )

        OutlinedTextField(
            value = dueDay,
            onValueChange = {
                if (it.isEmpty() || (it.toIntOrNull() != null && it.length <= 2)) {
                    onDueDayChange(it)
                }
            },
            label = { Text("Due Day of Month *") },
            placeholder = { Text("1-31") },
            leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
            trailingIcon = {
                IconButton(onClick = onShowDueDayPicker) {
                    Icon(Icons.Default.DateRange, "Select Date", tint = MaterialTheme.colorScheme.primary)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            supportingText = { Text("Day of month when bill is due (1-31)") }
        )

        ExposedDropdownMenuBox(
            expanded = expandedCategory,
            onExpandedChange = { onExpandedCategoryChange(!expandedCategory && !isLoading) }
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true,
                label = { Text("Category *") },
                leadingIcon = { Icon(Icons.Default.Category, null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )
            ExposedDropdownMenu(
                expanded = expandedCategory,
                onDismissRequest = { onExpandedCategoryChange(false) }
            ) {
                categories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = { onCategoryChange(cat); onExpandedCategoryChange(false) }
                    )
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = expandedFrequency,
            onExpandedChange = { onExpandedFrequencyChange(!expandedFrequency && !isLoading) }
        ) {
            OutlinedTextField(
                value = frequency.toDisplayName(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Frequency *") },
                leadingIcon = { Icon(Icons.Default.Repeat, null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFrequency) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )
            ExposedDropdownMenu(
                expanded = expandedFrequency,
                onDismissRequest = { onExpandedFrequencyChange(false) }
            ) {
                ExpenseFrequency.entries.forEach { freq ->
                    DropdownMenuItem(
                        text = { Text(freq.toDisplayName()) },
                        onClick = { onFrequencyChange(freq); onExpandedFrequencyChange(false) }
                    )
                }
            }
        }

        if (frequency == ExpenseFrequency.CUSTOM) {
            OutlinedTextField(
                value = customFrequencyDays,
                onValueChange = onCustomFrequencyDaysChange,
                label = { Text("Custom Days *") },
                placeholder = { Text("e.g., 45 for every 45 days") },
                leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                supportingText = { Text("Number of days between occurrences") }
            )
        }
    }
}

@Composable
private fun ReminderSettingsSection(
    reminderEnabled: Boolean,
    onReminderEnabledChange: (Boolean) -> Unit,
    reminderDaysBefore: String,
    onReminderDaysBeforeChange: (String) -> Unit,
    isLoading: Boolean
) {
    val haptics = rememberHaptics()
    FormSectionCard(icon = Icons.Default.Notifications, title = "Reminder Settings", iconTint = MaterialTheme.colorScheme.tertiary) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Enable Reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Get notified before bill is due", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = reminderEnabled, onCheckedChange = { haptics.toggle(it); onReminderEnabledChange(it) }, enabled = !isLoading)
        }

        if (reminderEnabled) {
            OutlinedTextField(
                value = reminderDaysBefore,
                onValueChange = onReminderDaysBeforeChange,
                label = { Text("Remind Days Before") },
                placeholder = { Text("3") },
                leadingIcon = { Icon(Icons.Default.Notifications, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                supportingText = { Text("Number of days before due date") }
            )
        }
    }
}

@Composable
private fun NotesSection(
    notes: String,
    onNotesChange: (String) -> Unit,
    isLoading: Boolean
) {
    FormSectionCard(icon = Icons.Default.Edit, title = "Notes", iconTint = MaterialTheme.colorScheme.secondary) {
        OutlinedTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = { Text("Additional Notes") },
            placeholder = { Text("e.g., Account number, payment instructions") },
            leadingIcon = { Icon(Icons.Default.Edit, null) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun PaymentOptionsSection(
    prepayEnabled: Boolean,
    onPrepayEnabledChange: (Boolean) -> Unit,
    firstPaymentDate: LocalDate?,
    onShowDatePicker: () -> Unit,
    isLoading: Boolean
) {
    val haptics = rememberHaptics()
    FormSectionCard(icon = Icons.Default.CalendarToday, title = "Payment Options", iconTint = MaterialTheme.colorScheme.primary) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Allow Prepayment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Enable paying this bill before due date", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = prepayEnabled, onCheckedChange = { haptics.toggle(it); onPrepayEnabledChange(it) }, enabled = !isLoading)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(enabled = !isLoading) { onShowDatePicker() }.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("First Payment Date (Optional)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Text(firstPaymentDate?.toString() ?: "Use default schedule", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.DateRange, "Select date")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SplitBillSection(
    houseMembers: List<MemberWithProfile>,
    selectedMembers: List<String>,
    onSelectedMembersChange: (List<String>) -> Unit,
    splitType: String,
    onSplitTypeChange: (String) -> Unit,
    customAmounts: Map<String, java.math.BigDecimal>,
    onCustomAmountsChange: (Map<String, java.math.BigDecimal>) -> Unit,
    currencySymbol: String,
    isLoading: Boolean
) {
    val haptics = rememberHaptics()
    FormSectionCard(icon = Icons.Default.Repeat, title = "Split Bill", iconTint = MaterialTheme.colorScheme.primary) {
        Text("Select members to split this bill with", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            houseMembers.forEach { member ->
                FilterChip(
                    selected = selectedMembers.contains(member.userId),
                    onClick = {
                        haptics.toggle(!selectedMembers.contains(member.userId))
                        onSelectedMembersChange(
                            if (selectedMembers.contains(member.userId)) selectedMembers - member.userId else selectedMembers + member.userId
                        )
                    },
                    label = { Text(member.fullName ?: member.userId) },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        if (selectedMembers.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Split Method", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = splitType == "equal",
                    onClick = { haptics.select(); onSplitTypeChange("equal") },
                    label = { Text("Equal Split") },
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                FilterChip(
                    selected = splitType == "custom",
                    onClick = { haptics.select(); onSplitTypeChange("custom") },
                    label = { Text("Custom Amounts") },
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (splitType == "custom") {
                Spacer(Modifier.height(16.dp))
                Text("Enter amount for each member", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))

                selectedMembers.forEach { memberId ->
                    val memberName = houseMembers.find { it.userId == memberId }?.fullName ?: memberId
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(memberName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        OutlinedTextField(
                            value = customAmounts[memberId]?.toString() ?: "",
                            onValueChange = {
                                val amt = it.toBigDecimalOrNull()
                                if (amt != null) onCustomAmountsChange(customAmounts + (memberId to amt))
                                else if (it.isEmpty()) onCustomAmountsChange(customAmounts - memberId)
                            },
                            label = { Text("Amount") },
                            prefix = { Text(currencySymbol) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.width(140.dp),
                            enabled = !isLoading,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SubmitButton(
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val haptics = rememberHaptics()
    Button(
        onClick = { haptics.tap(); onClick() },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
        } else {
            Text("Add Recurring Bill", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FirstPaymentDatePickerDialog(
    onDismissRequest: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val instant = Instant.fromEpochMilliseconds(millis)
                    onDateSelected(instant.toLocalDateTime(TimeZone.UTC).date)
                }
                onDismissRequest()
            }) { Text("OK", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel", fontWeight = FontWeight.Medium) }
        },
        shape = MaterialTheme.shapes.large
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DueDayPickerDialog(
    onDismissRequest: () -> Unit,
    onDaySelected: (String) -> Unit
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val instant = Instant.fromEpochMilliseconds(millis)
                    val date = instant.toLocalDateTime(TimeZone.UTC).date
                    onDaySelected(date.day.toString())
                }
                onDismissRequest()
            }) { Text("OK", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("Cancel", fontWeight = FontWeight.Medium) }
        },
        shape = MaterialTheme.shapes.large
    ) {
        DatePicker(state = datePickerState, title = {
            Text(modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp), text = "Select due day", style = MaterialTheme.typography.titleMedium)
        })
    }
}
