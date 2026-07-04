package `in`.xroden.flockr.features.expenses.ui.perdiem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import `in`.xroden.flockr.features.expenses.presentation.PerDiemEntryUiState
import `in`.xroden.flockr.features.expenses.presentation.PerDiemViewModel
import `in`.xroden.flockr.features.expenses.model.PerDiemEntryWithDetails
import `in`.xroden.flockr.ui.components.inputs.MonthSelector
import `in`.xroden.flockr.ui.components.loading.ListScreenSkeleton
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.utils.getCurrencySymbol
import `in`.xroden.flockr.utils.formatWithHouseConfig
import kotlinx.datetime.*
import java.util.Locale
import java.math.BigDecimal
import kotlin.time.Clock
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
        val now = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        mutableStateOf(LocalDate(now.year, now.month, 1))
    }

    // FIX: Collect the correct state flow from ViewModel
    val entryState by viewModel.entryState.collectAsStateWithLifecycle()

    val houseConfig by viewModel.houseConfig.collectAsStateWithLifecycle()
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
                        "Transactions",
                        style = MaterialTheme.typography.titleMedium,
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        // FIX: Handle the UI State (Loading/Success/Error) explicitly
        when (val state = entryState) {
            is PerDiemEntryUiState.Loading -> {
                ListScreenSkeleton(modifier = Modifier.fillMaxSize().padding(padding))
            }
            is PerDiemEntryUiState.Error -> {
                PerDiemErrorContent(
                    message = state.message,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
            is PerDiemEntryUiState.Success -> {
                PerDiemTransactionsContent(
                    entries = state.entries,
                    selectedMonth = selectedMonth,
                    onMonthChange = { selectedMonth = it },
                    currencySymbol = currencySymbol,
                    houseConfig = houseConfig,
                    onDeleteEntry = { entryId -> viewModel.deletePerDiemEntry(houseId, entryId) },
                    onUpdateEntry = { entryId, quantity, date, notes ->
                        viewModel.updatePerDiemEntry(houseId, entryId, quantity, date, notes)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun PerDiemErrorContent(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun PerDiemTransactionsContent(
    entries: List<PerDiemEntryWithDetails>,
    selectedMonth: LocalDate,
    onMonthChange: (LocalDate) -> Unit,
    currencySymbol: String,
    houseConfig: HouseConfig?,
    onDeleteEntry: (String) -> Unit,
    onUpdateEntry: (String, BigDecimal?, LocalDate?, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Month selector - using unified component
        item {
            MonthSelector(
                selectedMonth = selectedMonth,
                onMonthChange = onMonthChange
            )
        }

        // Total for the month - redesigned hero card
        item {
            MonthSpendingSummaryCard(
                entries = entries,
                selectedMonth = selectedMonth,
                currencySymbol = currencySymbol
            )
        }

        // Transactions list
        if (entries.isEmpty()) {
            item {
                EmptyTransactionsContent()
            }
        } else {
            // Group by date
            val groupedEntries = entries.groupBy { it.date }
                .toSortedMap(compareByDescending { it })

            groupedEntries.forEach { (date, dateEntries) ->
                item {
                    Text(
                        text = formatDate(date, houseConfig),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                items(dateEntries, key = { it.entryId }) { entry ->
                    PerDiemTransactionCard(
                        entry = entry,
                        currencySymbol = currencySymbol,
                        onDelete = { onDeleteEntry(entry.entryId) },
                        onUpdate = { quantity, date, notes ->
                            onUpdateEntry(entry.entryId, quantity, date, notes)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthSpendingSummaryCard(
    entries: List<PerDiemEntryWithDetails>,
    selectedMonth: LocalDate,
    currencySymbol: String
) {
    val totalAmount = entries.sumOf { it.totalCost.toDouble() }
    val monthName = selectedMonth.month.name.lowercase()
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Payments,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    "$monthName Spending",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    "${entries.size} transactions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Text(
                text = "$currencySymbol${String.format(Locale.getDefault(), "%.2f", totalAmount)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun EmptyTransactionsContent() {
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
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
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = entry.itemName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format(Locale.getDefault(), "%.1f", entry.quantity)} ${entry.unit} @ $currencySymbol${String.format(Locale.getDefault(), "%.2f", entry.rate)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // User Name
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = entry.userName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                if (!entry.notes.isNullOrBlank()) {
                     Surface(
                         color = MaterialTheme.colorScheme.surface,
                         shape = RoundedCornerShape(6.dp),
                         modifier = Modifier.padding(top = 4.dp)
                     ) {
                         Text(
                             text = entry.notes,
                             style = MaterialTheme.typography.bodySmall,
                             color = MaterialTheme.colorScheme.onSurfaceVariant,
                             modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                         )
                     }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$currencySymbol${String.format(Locale.getDefault(), "%.2f", entry.totalCost)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalIconButton(
            onClick = { onMonthChange(selectedMonth.minus(1, DateTimeUnit.MONTH)) },
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Icon(
                Icons.Default.ChevronLeft,
                "Previous month",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = "$monthName ${selectedMonth.year}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        val isFuture = selectedMonth.plus(1, DateTimeUnit.MONTH) > currentMonthStart

        FilledTonalIconButton(
            onClick = { onMonthChange(selectedMonth.plus(1, DateTimeUnit.MONTH)) },
            enabled = !isFuture,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Icon(
                Icons.Default.ChevronRight,
                "Next month",
                tint = if (!isFuture)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

private fun formatDate(date: LocalDate, houseConfig: HouseConfig?): String =
    date.formatWithHouseConfig(houseConfig)
