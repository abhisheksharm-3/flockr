package `in`.xroden.flockr.features.expenses.ui.perdiem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import `in`.xroden.flockr.features.expenses.model.PerDiemConfig
import `in`.xroden.flockr.ui.components.cards.SectionCard
import `in`.xroden.flockr.features.expenses.domain.PerDiemViewModel
import `in`.xroden.flockr.features.expenses.domain.PerDiemConfigUiState
import `in`.xroden.flockr.ui.theme.DateFormats
import `in`.xroden.flockr.ui.util.getCurrencySymbol
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal

/**
 * Modern screen for adding daily per-diem entries.
 * Shows all active per-diem configs and allows quick entry of quantities.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerDiemDailyEntryScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: PerDiemViewModel = hiltViewModel()
) {
    var selectedConfig by remember { mutableStateOf<PerDiemConfig?>(null) }
    var quantity by remember { mutableStateOf("") }
    var date by remember { 
        mutableStateOf(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString())
    }
    var notes by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val configState by viewModel.configState.collectAsState()
    val houseConfig by viewModel.houseConfig.collectAsState()
    val currencySymbol = getCurrencySymbol(houseConfig?.currencyCode ?: "$")
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(houseId) {
        viewModel.loadConfigs(houseId)
        viewModel.loadHouseConfig(houseId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Daily Entry",
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
                ),
                actions = {
                    Button(
                        onClick = {
                            val quantityDouble = quantity.toDoubleOrNull()
                            if (quantityDouble == null || quantityDouble <= 0) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Please enter a valid quantity")
                                }
                                return@Button
                            }

                            isLoading = true
                            scope.launch {
                                try {
                                    viewModel.createPerDiemEntry(
                                        houseId = houseId,
                                        configId = selectedConfig!!.id,
                                        quantity = BigDecimal(quantityDouble),
                                        date = LocalDate.parse(date),
                                        itemName = selectedConfig!!.itemName,
                                        notes = notes.takeIf { it.isNotBlank() }
                                    )
                                    snackbarHostState.showSnackbar("Entry added successfully")
                                    quantity = ""
                                    notes = ""
                                    selectedConfig = null
                                    isLoading = false
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Failed: ${e.message}")
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading && selectedConfig != null && quantity.toDoubleOrNull()?.let { it > 0 } == true,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Log Usage", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val state = configState) {
            is PerDiemConfigUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is PerDiemConfigUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "Error Loading Configs",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(onClick = { viewModel.loadConfigs(houseId) }) {
                            Text("Retry")
                        }
                    }
                }
            }
            is PerDiemConfigUiState.Success -> {
                val configs = state.configs
                if (configs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = "No Per Diem Items",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Add per diem config items first from the Per-Diem Config screen",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header
                        item {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Select Item",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Choose an item to log usage for today",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Config Items
                        items(configs) { config ->
                            val isSelected = selectedConfig?.id == config.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        selectedConfig = if (isSelected) null else config
                                        quantity = ""
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                ),
                                shape = MaterialTheme.shapes.large,
                                border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = config.itemName,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Black, // Extra bold for clear hierarchy
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "$currencySymbol${"%.2f".format(config.rate.toDouble())} per ${config.unit}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                    
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.AddCircleOutline,
                                            contentDescription = "Add",
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Entry Details (only show when item is selected)
                        if (selectedConfig != null) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                SectionCard(title = "Entry Details for ${selectedConfig!!.itemName}") {
                                    // Date
                                    OutlinedTextField(
                                        value = date,
                                        onValueChange = { date = it },
                                        label = { Text("Date *") },
                                        placeholder = { Text("YYYY-MM-DD") },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !isLoading,
                                        singleLine = true,
                                        shape = MaterialTheme.shapes.medium
                                    )

                                    // Quantity
                                    OutlinedTextField(
                                        value = quantity,
                                        onValueChange = { quantity = it },
                                        label = { Text("Quantity (${selectedConfig!!.unit}) *") },
                                        placeholder = { Text("Enter amount") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !isLoading,
                                        singleLine = true,
                                        shape = MaterialTheme.shapes.medium,
                                        textStyle = MaterialTheme.typography.bodyLarge
                                    )

                                    // Cost Estimate
                                    val quantityDouble = quantity.toDoubleOrNull()
                                    if (quantityDouble != null && quantityDouble > 0) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = MaterialTheme.shapes.small,
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Total Cost",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                                Text(
                                                    text = "$currencySymbol${"%.2f".format(quantityDouble * selectedConfig!!.rate.toDouble())}",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Notes
                                    OutlinedTextField(
                                        value = notes,
                                        onValueChange = { notes = it },
                                        label = { Text("Notes (Optional)") },
                                        placeholder = { Text("Add details...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2,
                                        enabled = !isLoading,
                                        shape = MaterialTheme.shapes.medium
                                    )
                                }
                                // Spacer at bottom for scrolling
                                Spacer(modifier = Modifier.height(64.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
