package `in`.xroden.flockr.features.expenses.ui.onetime

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.features.expenses.presentation.OneTimeExpenseViewModel
import `in`.xroden.flockr.features.expenses.ui.ExpenseCategories
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import `in`.xroden.flockr.utils.formatWithHouseConfig
import java.math.BigDecimal
import java.math.RoundingMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.utils.rememberHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseScreen(
    houseId: String,
    expenseId: String,
    onNavigateBack: () -> Unit,
    viewModel: OneTimeExpenseViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var date by remember {
        mutableStateOf(kotlin.time.Clock.System.todayIn(TimeZone.currentSystemDefault()).toString())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var expandedCategory by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("Groceries") }

    // Split state
    var houseMembers by remember { mutableStateOf<List<MemberWithProfile>>(emptyList()) }
    var enableSplitting by remember { mutableStateOf(false) }
    var splitEqually by remember { mutableStateOf(true) }
    var selectedMembers by remember { mutableStateOf<Set<String>>(emptySet()) }
    var customSplits by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val categories = ExpenseCategories.DEFAULT
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val expenseState by viewModel.selectedExpense.collectAsStateWithLifecycle()

    val houseConfig by viewModel.houseConfig.collectAsStateWithLifecycle()
    val currencySymbol = remember(houseConfig) {
        houseConfig?.getCurrencySymbol() ?: "$"
    }

    LaunchedEffect(houseId, expenseId) {
        viewModel.loadHouseConfig(houseId)
        viewModel.loadOneTimeExpense(expenseId)
        houseMembers = viewModel.getHouseMembers(houseId)
    }

    LaunchedEffect(expenseState) {
        expenseState?.let { expense ->
            name = expense.name
            amount = expense.amount.toString()
            date = expense.date.toString()
            category = expense.category
            notes = expense.notes ?: ""

            // Initialize splits
            if (!expense.splits.isNullOrEmpty()) {
                enableSplitting = true
                selectedMembers = expense.splits.map { it.userId }.toSet()

                // Check if splits are roughly equal
                val amounts = expense.splits.map { it.amountOwed }
                val firstAmount = amounts.firstOrNull()
                val areEqual = amounts.all { it.compareTo(firstAmount) == 0 }

                splitEqually = areEqual

                if (!areEqual) {
                    customSplits = expense.splits.associate { it.userId to it.amountOwed.toString() }
                }
            } else {
                enableSplitting = false
                selectedMembers = emptySet()
                customSplits = emptyMap()
            }

            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Expense", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
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
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExpenseDetailsCard(
                    name = name,
                    onNameChange = { name = it },
                    amount = amount,
                    onAmountChange = { amount = it },
                    currencySymbol = currencySymbol,
                    isSaving = isSaving
                )

                DateCategoryCard(
                    // `date` stays ISO (source of truth for saving); display honors the house format.
                    date = runCatching { LocalDate.parse(date).formatWithHouseConfig(houseConfig) }.getOrDefault(date),
                    onDateClick = { showDatePicker = true },
                    category = category,
                    categories = categories,
                    expandedCategory = expandedCategory,
                    onExpandedChange = { expandedCategory = it },
                    onCategorySelected = { category = it; expandedCategory = false },
                    onDismissDropdown = { expandedCategory = false },
                    isSaving = isSaving
                )

                NotesCard(
                    notes = notes,
                    onNotesChange = { notes = it },
                    isSaving = isSaving
                )

                SplitBillCard(
                    enableSplitting = enableSplitting,
                    onEnableSplittingChange = { enableSplitting = it },
                    splitEqually = splitEqually,
                    onSplitEquallyChange = { splitEqually = it },
                    houseMembers = houseMembers,
                    selectedMembers = selectedMembers,
                    onSelectedMembersChange = { selectedMembers = it },
                    customSplits = customSplits,
                    onCustomSplitChange = { userId, value -> customSplits = customSplits + (userId to value) },
                    amount = amount,
                    currencySymbol = currencySymbol,
                    isSaving = isSaving
                )

                SaveButton(
                    isSaving = isSaving,
                    enabled = !isSaving && name.isNotBlank() && amount.toBigDecimalOrNull() != null && date.isNotBlank(),
                    onClick = {
                        val amt = amount.toBigDecimalOrNull()
                        if (amt == null || amt <= BigDecimal.ZERO) {
                            scope.launch { snackbarHostState.showSnackbar("Please enter a valid amount") }
                            return@SaveButton
                        }
                        isSaving = true
                        val parsedDate: LocalDate = runCatching {
                            val parts = date.split("-")
                            LocalDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                        }.getOrElse {
                            runCatching {
                                kotlin.time.Clock.System.todayIn(TimeZone.of(houseConfig?.timezone ?: TimeZone.currentSystemDefault().id))
                            }.getOrDefault(kotlin.time.Clock.System.todayIn(TimeZone.currentSystemDefault()))
                        }
                        val splitAmounts = if (enableSplitting && selectedMembers.isNotEmpty()) {
                            if (splitEqually) {
                                // Divisor must include the payer (who has no split row), matching
                                // create semantics; dividing by selectedMembers.size alone overcharges.
                                val splitAmount = amt.divide(BigDecimal(selectedMembers.size + 1), 2, RoundingMode.HALF_UP)
                                selectedMembers.associateWith { splitAmount }
                            } else {
                                selectedMembers.mapNotNull { userId ->
                                    customSplits[userId]?.toBigDecimalOrNull()?.let { userId to it }
                                }.toMap()
                            }
                        } else null
                        viewModel.updateOneTimeExpense(
                            houseId = houseId, expenseId = expenseId, name = name, amount = amt,
                            category = category, date = parsedDate, notes = notes.takeIf { it.isNotBlank() }, splitAmounts = splitAmounts
                        )
                        onNavigateBack()
                    }
                )
            }
        }
    }

    if (showDatePicker) {
        ExpenseDatePickerDialog(
            date = date,
            onDateSelected = { date = it },
            onDismiss = { showDatePicker = false }
        )
    }
}

/**
 * Shared section header: a rounded icon chip followed by a title.
 */
@Composable
private fun SectionHeader(
    icon: ImageVector,
    iconTint: Color,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(10.dp),
            color = iconTint.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconTint)
            }
        }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ExpenseDetailsCard(
    name: String,
    onNameChange: (String) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    currencySymbol: String,
    isSaving: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(Icons.Filled.Receipt, MaterialTheme.colorScheme.primary, "Expense Details")

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Expense Name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                label = { Text("Amount") },
                prefix = { Text(currencySymbol) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateCategoryCard(
    date: String,
    onDateClick: () -> Unit,
    category: String,
    categories: List<String>,
    expandedCategory: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCategorySelected: (String) -> Unit,
    onDismissDropdown: () -> Unit,
    isSaving: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(Icons.Filled.DateRange, MaterialTheme.colorScheme.tertiary, "Date & Category")

            Card(
                modifier = Modifier.fillMaxWidth().clickable(enabled = !isSaving) { onDateClick() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Date", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(date, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                    Icon(Icons.Filled.Edit, "Change date", tint = MaterialTheme.colorScheme.primary)
                }
            }

            ExposedDropdownMenuBox(
                expanded = expandedCategory,
                onExpandedChange = { onExpandedChange(!expandedCategory && !isSaving) }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
                ExposedDropdownMenu(expanded = expandedCategory, onDismissRequest = onDismissDropdown) {
                    categories.forEach { cat ->
                        DropdownMenuItem(text = { Text(cat) }, onClick = { onCategorySelected(cat) })
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesCard(
    notes: String,
    onNotesChange: (String) -> Unit,
    isSaving: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(Icons.Filled.Description, MaterialTheme.colorScheme.secondary, "Notes")

            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                placeholder = { Text("Add any additional details...") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                minLines = 3,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
    }
}

@Composable
private fun SplitBillCard(
    enableSplitting: Boolean,
    onEnableSplittingChange: (Boolean) -> Unit,
    splitEqually: Boolean,
    onSplitEquallyChange: (Boolean) -> Unit,
    houseMembers: List<MemberWithProfile>,
    selectedMembers: Set<String>,
    onSelectedMembersChange: (Set<String>) -> Unit,
    customSplits: Map<String, String>,
    onCustomSplitChange: (String, String) -> Unit,
    amount: String,
    currencySymbol: String,
    isSaving: Boolean
) {
    val haptics = rememberHaptics()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Group, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Column {
                        Text("Split Bill", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (enableSplitting) "Enabled" else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(checked = enableSplitting, onCheckedChange = { haptics.toggle(it); onEnableSplittingChange(it) }, enabled = !isSaving)
            }

            if (enableSplitting) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = splitEqually,
                        onClick = { haptics.select(); onSplitEquallyChange(true) },
                        label = { Text("Equal Split") },
                        leadingIcon = if (splitEqually) { { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) } } else null,
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp)
                    )
                    FilterChip(
                        selected = !splitEqually,
                        onClick = { haptics.select(); onSplitEquallyChange(false) },
                        label = { Text("Custom Amounts") },
                        leadingIcon = if (!splitEqually) { { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) } } else null,
                        enabled = !isSaving,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Text("Select members:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)

                houseMembers.forEach { member ->
                    key(member.userId) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = selectedMembers.contains(member.userId),
                                    onCheckedChange = { checked ->
                                        haptics.toggle(checked)
                                        onSelectedMembersChange(if (checked) selectedMembers + member.userId else selectedMembers - member.userId)
                                    },
                                    enabled = !isSaving
                                )
                                Column {
                                    Text(member.fullName ?: "Unknown", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(member.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (!splitEqually && selectedMembers.contains(member.userId)) {
                                OutlinedTextField(
                                    value = customSplits[member.userId] ?: "",
                                    onValueChange = { value -> onCustomSplitChange(member.userId, value) },
                                    label = { Text(currencySymbol) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.width(100.dp),
                                    enabled = !isSaving,
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }

                if (selectedMembers.isNotEmpty() && amount.toDoubleOrNull() != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    val totalAmount = amount.toDoubleOrNull() ?: 0.0
                    val splitAmount = if (splitEqually) totalAmount / selectedMembers.size else customSplits.values.mapNotNull { it.toDoubleOrNull() }.sum()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Split among ${selectedMembers.size} members", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (splitEqually) "$currencySymbol${"%.2f".format(splitAmount)} each" else "$currencySymbol${"%.2f".format(splitAmount)} total",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SaveButton(
    isSaving: Boolean,
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
        if (isSaving) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
        } else {
            Text("Save Changes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseDatePickerDialog(
    date: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = runCatching {
            val localDate = LocalDate.parse(date)
            localDate.toEpochDays() * 24 * 60 * 60 * 1000L
        }.getOrDefault(System.currentTimeMillis())
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val epochDays = millis / (24 * 60 * 60 * 1000L)
                    val selectedDate = LocalDate.fromEpochDays(epochDays.toInt())
                    onDateSelected(selectedDate.toString())
                }
                onDismiss()
            }) { Text("OK", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", fontWeight = FontWeight.Medium) }
        },
        shape = MaterialTheme.shapes.large
    ) {
        DatePicker(state = datePickerState)
    }
}
