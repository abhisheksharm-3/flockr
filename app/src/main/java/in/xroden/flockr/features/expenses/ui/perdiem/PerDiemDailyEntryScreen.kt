package `in`.xroden.flockr.features.expenses.ui.perdiem

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import `in`.xroden.flockr.features.expenses.model.PerDiemConfig
import `in`.xroden.flockr.features.expenses.domain.PerDiemViewModel
import `in`.xroden.flockr.features.expenses.domain.PerDiemConfigUiState
import `in`.xroden.flockr.ui.util.getCurrencySymbol
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal

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
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Log Daily Usage",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            // Sticky Bottom Bar for Action
            AnimatedVisibility(
                visible = selectedConfig != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
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
                                    snackbarHostState.showSnackbar("Entry Logged: ${selectedConfig!!.itemName}")
                                    // Reset but allow logging another? or close? Maybe reset.
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
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = !isLoading,
                        shape = MaterialTheme.shapes.large
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Default.Check, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Log Entry", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val state = configState) {
            is PerDiemConfigUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            is PerDiemConfigUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is PerDiemConfigUiState.Success -> {
                val configs = state.configs
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            "Select an item",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    if (configs.isEmpty()) {
                        item {
                            Text("No items configured yet.", style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    items(configs) { config ->
                        val isSelected = selectedConfig?.id == config.id
                        val containerColor by animateColorAsState(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
                        )
                        
                        Card(
                            onClick = {
                                selectedConfig = if (isSelected) null else config
                                if (!isSelected) {
                                    quantity = "" // Reset on new selection
                                    notes = ""
                                }
                            },
                             colors = CardDefaults.cardColors(
                                containerColor = containerColor
                            ),
                            shape = MaterialTheme.shapes.extraLarge,
                            modifier = Modifier.fillMaxWidth().animateContentSize()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            config.itemName,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "$currencySymbol${config.rate} / ${config.unit}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { 
                                             selectedConfig = if (isSelected) null else config
                                             if (!isSelected) { quantity = "" }
                                        }
                                    )
                                }

                                AnimatedVisibility(visible = isSelected) {
                                    Column(
                                        modifier = Modifier.padding(top = 24.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        
                                        // Inputs
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            // Date Field (Fake ReadOnly for now)
                                            OutlinedTextField(
                                                value = date,
                                                onValueChange = { date = it }, // Editable for manual fix
                                                label = { Text("Date") },
                                                modifier = Modifier.weight(1f),
                                                shape = MaterialTheme.shapes.medium
                                            )
                                            
                                            // Quantity
                                            OutlinedTextField(
                                                value = quantity,
                                                onValueChange = { quantity = it },
                                                label = { Text("Qty (${config.unit})") },
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = KeyboardType.Number,
                                                    imeAction = ImeAction.Next
                                                ),
                                                modifier = Modifier.weight(1f),
                                                shape = MaterialTheme.shapes.medium
                                            )
                                        }
                                        
                                        OutlinedTextField(
                                            value = notes,
                                            onValueChange = { notes = it },
                                            label = { Text("Notes") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = MaterialTheme.shapes.medium
                                        )
                                        
                                        // Cost Preview
                                        val qty = quantity.toDoubleOrNull() ?: 0.0
                                        val total = qty * config.rate.toDouble()
                                        if (total > 0) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha=0.5f), MaterialTheme.shapes.medium)
                                                    .padding(16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Total Cost", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                                Text(
                                                    "$currencySymbol${"%.2f".format(total)}", 
                                                    style = MaterialTheme.typography.titleMedium, 
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}
