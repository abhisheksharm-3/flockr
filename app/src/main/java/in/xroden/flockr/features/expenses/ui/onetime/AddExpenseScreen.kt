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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.ui.components.cards.SectionCard
import `in`.xroden.flockr.features.expenses.domain.ExpenseViewModel
import `in`.xroden.flockr.features.house.domain.HouseManagementViewModel
import `in`.xroden.flockr.ui.util.getCurrencySymbol
import `in`.xroden.flockr.data.enums.ExpenseSplitType
import `in`.xroden.flockr.features.expenses.domain.CreateExpenseUiState
import `in`.xroden.flockr.features.expenses.ui.ExpenseCategories
import `in`.xroden.flockr.ui.theme.DateFormats
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    houseId: String,
    initialName: String? = null,
    initialQuantity: Int? = null,
    onNavigateBack: () -> Unit,
    onExpenseAdded: () -> Unit,
    viewModel: ExpenseViewModel = hiltViewModel(),
    houseManagementViewModel: HouseManagementViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf(initialName ?: "") }
    var amount by remember { mutableStateOf("") }
    var date by remember { 
        mutableStateOf(Clock.System.todayIn(TimeZone.currentSystemDefault()).toString())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var notes by remember {
        mutableStateOf(if (initialQuantity != null) "Quantity: $initialQuantity" else "")
    }
    var expandedCategory by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("Groceries") }
    var enableSplitting by remember { mutableStateOf(false) }
    var splitEqually by remember { mutableStateOf(true) }
    var houseMembers by remember { mutableStateOf<List<MemberWithProfile>>(emptyList()) }
    var selectedMembers by remember { mutableStateOf<Set<String>>(emptySet()) }
    var customSplits by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val categories = ExpenseCategories.DEFAULT

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val createState by viewModel.createState.collectAsState()
    
    val houseConfig by viewModel.houseConfig.collectAsState()
    val currencySymbol = houseConfig?.getCurrencySymbol() ?: "$"

    LaunchedEffect(houseId) {
        viewModel.loadHouseConfig(houseId)
        houseMembers = houseManagementViewModel.getHouseMembers(houseId)
    }

    // Handle create state
    LaunchedEffect(createState) {
        when (val state = createState) {
            is CreateExpenseUiState.Success -> {
                isLoading = false
                onExpenseAdded()
            }
            is CreateExpenseUiState.Error -> {
                isLoading = false
                snackbarHostState.showSnackbar("Error: ${state.message}")
            }
            is CreateExpenseUiState.Loading -> {
                isLoading = true
            }
            else -> {
                isLoading = false
            }
        }
    }

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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Text(
                text = "New Expense",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Basic Info Section
            SectionCard(title = "Expense Details") {
                // Expense Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Expense Name *") },
                    placeholder = { Text("e.g., Groceries, Electric Bill") },
                    leadingIcon = { Icon(Icons.Filled.Create, null) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                // Date Picker Card
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isLoading) { showDatePicker = true },
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column {
                                Text(
                                    "Date",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    date,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Change date",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

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
                        leadingIcon = { Icon(Icons.Filled.ShoppingBag, null) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) 
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                        enabled = !isLoading,
                        shape = MaterialTheme.shapes.medium,
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

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Add any additional details...") },
                    leadingIcon = { Icon(Icons.Filled.Description, null) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    minLines = 3,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )
            }

            // Bill Splitting Section
            SectionCard(
                title = "Split Bill",
                subtitle = if (enableSplitting) "Enabled" else "Disabled",
                action = {
                    Switch(
                        checked = enableSplitting,
                        onCheckedChange = { enableSplitting = it },
                        enabled = !isLoading
                    )
                }
            ) {
                if (enableSplitting) {
                    // Split Type Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = splitEqually,
                            onClick = { splitEqually = true },
                            label = { Text("Equal Split") },
                            leadingIcon = if (splitEqually) {
                                { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                            } else null,
                            enabled = !isLoading,
                            shape = MaterialTheme.shapes.medium
                        )
                        FilterChip(
                            selected = !splitEqually,
                            onClick = { splitEqually = false },
                            label = { Text("Custom Amounts") },
                            leadingIcon = if (!splitEqually) {
                                { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                            } else null,
                            enabled = !isLoading,
                            shape = MaterialTheme.shapes.medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Select members:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Member Selection
                    houseMembers.forEach { member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
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
                                        selectedMembers = if (checked) {
                                            selectedMembers + member.userId
                                        } else {
                                            selectedMembers - member.userId
                                        }
                                    },
                                    enabled = !isLoading
                                )
                                Column {
                                    Text(
                                        text = member.fullName ?: "Unknown",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = member.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Custom Amount Field
                            if (!splitEqually && selectedMembers.contains(member.userId)) {
                                OutlinedTextField(
                                    value = customSplits[member.userId] ?: "",
                                    onValueChange = { value ->
                                        customSplits = customSplits + (member.userId to value)
                                    },
                                    label = { Text(currencySymbol) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.width(100.dp),
                                    enabled = !isLoading,
                                    singleLine = true,
                                    shape = MaterialTheme.shapes.small
                                )
                            }
                        }
                    }

                    // Split Summary
                    if (selectedMembers.isNotEmpty() && amount.toDoubleOrNull() != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        val totalAmount = amount.toDoubleOrNull() ?: 0.0
                        val splitAmount = if (splitEqually) {
                            totalAmount / selectedMembers.size
                        } else {
                            customSplits.values.mapNotNull { it.toDoubleOrNull() }.sum()
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Split among ${selectedMembers.size} members",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (splitEqually) {
                                    "$currencySymbol${"%.2f".format(splitAmount)} each"
                                } else {
                                    "$currencySymbol${"%.2f".format(splitAmount)} total"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Submit Button
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    if (amt == null) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please enter a valid amount")
                        }
                        return@Button
                    }

                    isLoading = true

                    // Prepare split parameters
                    val splitWith = if (enableSplitting && selectedMembers.isNotEmpty()) {
                        selectedMembers.toList()
                    } else null
                    
                    val splitType = if (enableSplitting && selectedMembers.isNotEmpty()) {
                        if (splitEqually) ExpenseSplitType.EQUAL else ExpenseSplitType.AMOUNT
                    } else null
                    
                    val splitAmounts = if (enableSplitting && !splitEqually && selectedMembers.isNotEmpty()) {
                        selectedMembers.mapNotNull { userId ->
                            customSplits[userId]?.toBigDecimalOrNull()?.let { userId to it }
                        }.toMap()
                    } else null

                    // Parse date to kotlinx.datetime.LocalDate
                    val parsedDate: LocalDate = try {
                        val parts = date.split("-")
                        LocalDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                    } catch (e: Exception) {
                        kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
                    }

                    viewModel.createOneTimeExpense(
                        houseId = houseId,
                        name = name,
                        amount = BigDecimal.valueOf(amt),
                        category = category,
                        date = parsedDate,
                        notes = notes.takeIf { it.isNotBlank() },
                        splitWith = splitWith,
                        splitType = splitType,
                        splitAmounts = splitAmounts
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isLoading && name.isNotBlank() && amount.toDoubleOrNull() != null && date.isNotBlank(),
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
                        "Add Expense",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // DatePicker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = run {
                try {
                    val localDate = LocalDate.parse(date)
                    // Convert LocalDate to epoch milliseconds
                    localDate.toEpochDays() * 24 * 60 * 60 * 1000L
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        // Convert epoch millis to LocalDate
                        val epochDays = millis / (24 * 60 * 60 * 1000L)
                        val selectedDate = LocalDate.fromEpochDays(epochDays.toInt())
                        date = selectedDate.toString()
                    }
                    showDatePicker = false
                }) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", fontWeight = FontWeight.Medium)
                }
            },
            shape = MaterialTheme.shapes.large
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
