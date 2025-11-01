package `in`.xroden.flockr.ui.screens.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPerDiemEntryScreen(
    houseId: String,
    configId: String,
    onNavigateBack: () -> Unit,
    perDiemViewModel: `in`.xroden.flockr.ui.viewmodel.PerDiemViewModel = hiltViewModel()
) {
    var quantity by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Get the config to display item name
    val configs by perDiemViewModel.configs.collectAsState()
    val config = configs.firstOrNull { it.id == configId }

    LaunchedEffect(houseId) {
        perDiemViewModel.loadConfigs(houseId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Per-Diem Entry") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (config == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Adding entry for: ${config.itemName}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Rate: $${config.rate}/${config.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity (${config.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    enabled = !isLoading
                )

                Button(
                    onClick = {
                        val quantityDouble = quantity.toDoubleOrNull()
                        if (quantityDouble != null) {
                            isLoading = true
                            scope.launch {
                                val currentDate = java.time.LocalDate.now().toString()
                                perDiemViewModel.createPerDiemEntry(
                                    configId = configId,
                                    houseId = houseId,
                                    quantity = quantityDouble,
                                    date = currentDate,
                                    notes = notes.ifBlank { null },
                                    itemName = config.itemName,
                                    onSuccess = {
                                        isLoading = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Entry added successfully")
                                        }
                                        onNavigateBack()
                                    },
                                    onError = { error ->
                                        isLoading = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Error: $error")
                                        }
                                    }
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && quantity.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Add Entry")
                    }
                }
            }
        }
    }
}
