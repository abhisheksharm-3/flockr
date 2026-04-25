package `in`.xroden.flockr.features.expenses.ui.onetime

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.features.expenses.presentation.AddExpenseFormState
import `in`.xroden.flockr.features.expenses.presentation.AddExpenseUiState
import `in`.xroden.flockr.features.expenses.presentation.AddExpenseViewModel
import `in`.xroden.flockr.features.expenses.ui.ExpenseCategories
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    houseId: String,
    initialName: String? = null,
    initialQuantity: Int? = null,
    onNavigateBack: () -> Unit,
    onExpenseAdded: () -> Unit,
    viewModel: AddExpenseViewModel = hiltViewModel()
) {
    val formState by viewModel.formState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }
    var expandedCategory by remember { mutableStateOf(false) }

    LaunchedEffect(houseId) {
        viewModel.initialize(houseId, initialName, initialQuantity)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { onExpenseAdded() }
    }

    LaunchedEffect(uiState) {
        if (uiState is AddExpenseUiState.Error) {
            snackbarHostState.showSnackbar("Error: ${(uiState as AddExpenseUiState.Error).message}")
            viewModel.resetUiState()
        }
    }

    val isLoading = uiState is AddExpenseUiState.Loading
    val categories = ExpenseCategories.DEFAULT

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Add Expense",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.background
            ) {
                Button(
                    onClick = { viewModel.submit(houseId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .navigationBarsPadding()
                        .height(56.dp),
                    enabled = !isLoading && formState.name.isNotBlank() && formState.amount.toBigDecimalOrNull() != null && formState.date != null,
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Add Expense", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
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
            ExpenseDetailsCard(
                name = formState.name,
                amount = formState.amount,
                currencySymbol = formState.currencySymbol,
                isLoading = isLoading,
                onNameChange = viewModel::onNameChange,
                onAmountChange = viewModel::onAmountChange
            )

            DateCategoryCard(
                date = formState.date?.toString() ?: "",
                category = formState.category,
                isLoading = isLoading,
                expandedCategory = expandedCategory,
                onExpandedChange = { expandedCategory = it },
                onDateClick = { showDatePicker = true },
                onCategoryChange = viewModel::onCategoryChange,
                categories = categories
            )

            NotesCard(
                notes = formState.notes,
                isLoading = isLoading,
                onNotesChange = viewModel::onNotesChange
            )

            SplitBillCard(
                formState = formState,
                isLoading = isLoading,
                onSplitEnabledChange = viewModel::onSplitEnabledChange,
                onSplitEqualChange = viewModel::onSplitEqualChange,
                onMemberSelectionChange = viewModel::onMemberSelectionChange,
                onCustomSplitChange = viewModel::onCustomSplitChange
            )
        }
    }

    if (showDatePicker) {
        ExpenseDatePicker(
            currentDate = formState.date,
            onDateSelected = { date ->
                viewModel.onDateChange(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun ExpenseDetailsCard(
    name: String,
    amount: String,
    currencySymbol: String,
    isLoading: Boolean,
    onNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CardSectionHeader(icon = Icons.Filled.Receipt, title = "Expense Details", tint = MaterialTheme.colorScheme.primary)

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Expense Name") },
                placeholder = { Text("e.g., Groceries, Electric Bill") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true,
                shape = MaterialTheme.shapes.small
            )

            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                label = { Text("Amount") },
                prefix = { Text(currencySymbol) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true,
                shape = MaterialTheme.shapes.small
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateCategoryCard(
    date: String,
    category: String,
    isLoading: Boolean,
    expandedCategory: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onDateClick: () -> Unit,
    onCategoryChange: (String) -> Unit,
    categories: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CardSectionHeader(icon = Icons.Filled.DateRange, title = "Date & Category", tint = MaterialTheme.colorScheme.tertiary)

            Card(
                modifier = Modifier.fillMaxWidth().clickable(enabled = !isLoading, onClick = onDateClick),
                shape = MaterialTheme.shapes.small,
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

            ExposedDropdownMenuBox(expanded = expandedCategory, onExpandedChange = { onExpandedChange(it && !isLoading) }) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                    enabled = !isLoading,
                    shape = MaterialTheme.shapes.small
                )
                ExposedDropdownMenu(expanded = expandedCategory, onDismissRequest = { onExpandedChange(false) }) {
                    categories.forEach { cat ->
                        DropdownMenuItem(text = { Text(cat) }, onClick = { onCategoryChange(cat); onExpandedChange(false) })
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesCard(
    notes: String,
    isLoading: Boolean,
    onNotesChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CardSectionHeader(icon = Icons.Filled.Description, title = "Notes", tint = MaterialTheme.colorScheme.secondary)
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                placeholder = { Text("Add any additional details...") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                minLines = 3,
                shape = MaterialTheme.shapes.small
            )
        }
    }
}

@Composable
private fun SplitBillCard(
    formState: AddExpenseFormState,
    isLoading: Boolean,
    onSplitEnabledChange: (Boolean) -> Unit,
    onSplitEqualChange: (Boolean) -> Unit,
    onMemberSelectionChange: (String, Boolean) -> Unit,
    onCustomSplitChange: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CardSectionIconOnly(icon = Icons.Filled.Group, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Split Bill", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (formState.isSplitEnabled) "Enabled" else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(checked = formState.isSplitEnabled, onCheckedChange = onSplitEnabledChange, enabled = !isLoading)
            }

            if (formState.isSplitEnabled) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = formState.isSplitEqual,
                        onClick = { onSplitEqualChange(true) },
                        label = { Text("Equal Split") },
                        leadingIcon = if (formState.isSplitEqual) { { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) } } else null,
                        enabled = !isLoading
                    )
                    FilterChip(
                        selected = !formState.isSplitEqual,
                        onClick = { onSplitEqualChange(false) },
                        label = { Text("Custom Amounts") },
                        leadingIcon = if (!formState.isSplitEqual) { { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) } } else null,
                        enabled = !isLoading
                    )
                }

                Text("Select members:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)

                formState.houseMembers.forEach { member ->
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
                                    checked = formState.selectedMemberIds.contains(member.userId),
                                    onCheckedChange = { checked -> onMemberSelectionChange(member.userId, checked) },
                                    enabled = !isLoading
                                )
                                Column {
                                    Text(member.fullName ?: "Unknown", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(member.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (!formState.isSplitEqual && formState.selectedMemberIds.contains(member.userId)) {
                                OutlinedTextField(
                                    value = formState.customSplits[member.userId] ?: "",
                                    onValueChange = { value -> onCustomSplitChange(member.userId, value) },
                                    label = { Text(formState.currencySymbol) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.width(100.dp),
                                    enabled = !isLoading,
                                    singleLine = true,
                                    shape = MaterialTheme.shapes.small
                                )
                            }
                        }
                    }
                }

                if (formState.selectedMemberIds.isNotEmpty() && formState.amount.toBigDecimalOrNull() != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    val totalAmount = formState.amount.toDoubleOrNull() ?: 0.0
                    val splitDisplay = if (formState.isSplitEqual) {
                        "${"%.2f".format(totalAmount / formState.selectedMemberIds.size)} each"
                    } else {
                        "${"%.2f".format(formState.customSplits.values.mapNotNull { it.toDoubleOrNull() }.sum())} total"
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Split among ${formState.selectedMemberIds.size} members", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${formState.currencySymbol}$splitDisplay", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CardSectionHeader(icon: ImageVector, title: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CardSectionIconOnly(icon = icon, tint = tint)
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CardSectionIconOnly(icon: ImageVector, tint: Color) {
    Surface(modifier = Modifier.size(40.dp), shape = MaterialTheme.shapes.extraSmall, color = tint.copy(alpha = 0.1f)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseDatePicker(
    currentDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = currentDate?.toEpochDays()?.times(24 * 60 * 60 * 1000L)
            ?: System.currentTimeMillis()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val epochDays = millis / (24 * 60 * 60 * 1000L)
                    onDateSelected(LocalDate.fromEpochDays(epochDays.toInt()))
                }
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
