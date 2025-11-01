package `in`.xroden.flockr.ui.screens.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.data.model.PerDiemConfig
import `in`.xroden.flockr.ui.viewmodel.PerDiemViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerDiemConfigScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAddEntry: (String) -> Unit,
    viewModel: PerDiemViewModel = hiltViewModel()
) {
    val configs by viewModel.configs.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(houseId) {
        viewModel.loadConfigs(houseId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Per-Diem Configuration") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Add Item")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (configs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No per-diem items configured",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Add items like milk, groceries, etc.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(configs) { config ->
                    PerDiemConfigCard(
                        config = config,
                        onAddEntry = { onNavigateToAddEntry(config.id) },
                        onDelete = {
                            scope.launch {
                                viewModel.deleteConfig(config.id)
                                viewModel.loadConfigs(houseId)
                                snackbarHostState.showSnackbar("Item deleted")
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddPerDiemConfigDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { itemName, rate, category, unit ->
                scope.launch {
                    val result = viewModel.createConfig(houseId, itemName, rate, category, unit)
                    if (result.isSuccess) {
                        showAddDialog = false
                        viewModel.loadConfigs(houseId)
                        snackbarHostState.showSnackbar("Item added")
                    } else {
                        snackbarHostState.showSnackbar("Failed to add item")
                    }
                }
            }
        )
    }
}

@Composable
fun PerDiemConfigCard(
    config: PerDiemConfig,
    onAddEntry: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = config.itemName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "$${config.rate}/${config.unit} • ${config.category}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                Button(onClick = onAddEntry) {
                    Text("Add Entry")
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPerDiemConfigDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String) -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Groceries") }
    var unit by remember { mutableStateOf("L") }
    var expandedCategory by remember { mutableStateOf(false) }

    val categories = listOf("Groceries", "Dairy", "Beverages", "Household", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Per-Diem Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item Name") },
                    placeholder = { Text("e.g., Milk") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = rate,
                    onValueChange = { rate = it },
                    label = { Text("Rate") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit") },
                    placeholder = { Text("e.g., L, kg, unit") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = !expandedCategory }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    rate.toDoubleOrNull()?.let { rateValue ->
                        onConfirm(itemName, rateValue, category, unit)
                    }
                },
                enabled = itemName.isNotBlank() && rate.toDoubleOrNull() != null && unit.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

