package `in`.xroden.flockr.features.expenses.ui.perdiem

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.features.expenses.domain.PerDiemEntryUiState
import `in`.xroden.flockr.features.expenses.domain.PerDiemViewModel
import `in`.xroden.flockr.features.expenses.model.PerDiemEntryWithDetails
import `in`.xroden.flockr.ui.util.getCurrencySymbol
import kotlinx.datetime.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.math.BigDecimal

/**
 * Screen to view all per diem transactions
 * Accessible from Quick Entry screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerDiemTransactionsScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: PerDiemViewModel = hiltViewModel()
) {
    // State for month selection (default to 1st of current month)
    var selectedMonth by remember {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        mutableStateOf(LocalDate(now.year, now.month, 1))
    }

    // FIX: Collect the correct state flow from ViewModel
    val entryState by viewModel.entryState.collectAsState()

    val houseConfig by viewModel.houseConfig.collectAsState()
    val currencySymbol = getCurrencySymbol(houseConfig?.currencyCode ?: "$")

    LaunchedEffect(houseId, selectedMonth) {
        // Format as "YYYY-MM" for the API/ViewModel
        val monthStr = "${selectedMonth.year}-${selectedMonth.monthNumber.toString().padStart(2, '0')}"
        viewModel.loadEntriesWithDetails(houseId, monthStr)
        viewModel.loadHouseConfig(houseId)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Per Diem Transactions",
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
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        // FIX: Handle the UI State (Loading/Success/Error) explicitly
        when (val state = entryState) {
            is PerDiemEntryUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is PerDiemEntryUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            is PerDiemEntryUiState.Success -> {
                val entries = state.entries

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Month selector
                    item {
                        MonthSelector(
                            selectedMonth = selectedMonth,
                            onMonthChange = { selectedMonth = it }
                        )
                    }

                    // Total for the month
                    item {
                        val totalAmount = entries.sumOf { it.totalCost.toDouble() }

                        // Format Month Name
                        val monthName = selectedMonth.month.name.lowercase()
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
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
                                Text(
                                    text = "Total for $monthName ${selectedMonth.year}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "$currencySymbol${String.format("%.2f", totalAmount)}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Transactions list
                    if (entries.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Receipt,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = "No transactions",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "No per diem entries for this month",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    } else {
                        // Group by date
                        val groupedEntries = entries.groupBy { it.date }
                            .toSortedMap(compareByDescending { it })

                        groupedEntries.forEach { (date, dateEntries) ->
                            item {
                                Text(
                                    text = formatDate(date),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }

                            items(dateEntries) { entry ->
                                PerDiemTransactionCard(
                                    entry = entry,
                                    currencySymbol = currencySymbol,
                                    onDelete = { viewModel.deletePerDiemEntry(houseId, entry.entryId) },
                                    onUpdate = { quantity, date, notes ->
                                        viewModel.updatePerDiemEntry(houseId, entry.entryId, quantity, date, notes)
                                    }
                                )
                            }
                        }
                    }

                    // Bottom spacing
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PerDiemTransactionCard(
    entry: PerDiemEntryWithDetails,
    currencySymbol: String,
    onDelete: () -> Unit,
    onUpdate: (BigDecimal?, LocalDate?, String?) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = entry.itemName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${String.format("%.1f", entry.quantity)} ${entry.unit} @ $currencySymbol${String.format("%.2f", entry.rate)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                // User Name
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = entry.userName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                if (!entry.notes.isNullOrBlank()) {
                    Text(
                        text = entry.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "$currencySymbol${String.format("%.2f", entry.totalCost)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = {
                                showMenu = false
                                showEditDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                showMenu = false
                                showDeleteDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
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

    if (showEditDialog) {
        EditTransactionDialog(
            entry = entry,
            onDismiss = { showEditDialog = false },
            onConfirm = { quantity, notes ->
                onUpdate(quantity, null, notes) // Date editing omitted for simplicity for now
                showEditDialog = false
            }
        )
    }
}

@Composable
fun EditTransactionDialog(
    entry: PerDiemEntryWithDetails,
    onDismiss: () -> Unit,
    onConfirm: (BigDecimal, String?) -> Unit
) {
    var quantityStr by remember { mutableStateOf(entry.quantity.toString()) }
    var notes by remember { mutableStateOf(entry.notes ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text("Quantity (${entry.unit})") },
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val quantity = quantityStr.toBigDecimalOrNull()
                    if (quantity != null) {
                        onConfirm(quantity, notes.ifBlank { null })
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun MonthSelector(
    selectedMonth: LocalDate,
    onMonthChange: (LocalDate) -> Unit
) {
    val currentMonthStart = remember {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        LocalDate(now.year, now.month, 1)
    }

    val monthName = selectedMonth.month.name.lowercase()
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onMonthChange(selectedMonth.minus(1, DateTimeUnit.MONTH)) }) {
                Icon(
                    Icons.Default.ChevronLeft,
                    "Previous month",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "$monthName ${selectedMonth.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            val isFuture = selectedMonth.plus(1, DateTimeUnit.MONTH) > currentMonthStart

            IconButton(
                onClick = { onMonthChange(selectedMonth.plus(1, DateTimeUnit.MONTH)) },
                enabled = !isFuture
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    "Next month",
                    tint = if (!isFuture)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}

private fun formatDate(date: LocalDate): String {
    return try {
        val javaDate = java.time.LocalDate.of(date.year, date.monthNumber, date.dayOfMonth)
        javaDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
    } catch (e: Exception) {
        "${date.dayOfMonth}/${date.monthNumber}/${date.year}"
    }
}