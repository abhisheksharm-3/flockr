package `in`.xroden.flockr.features.expenses.ui.perdiem

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.features.expenses.domain.PerDiemViewModel
import `in`.xroden.flockr.utils.getCurrencySymbol
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPerDiemConfigScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: PerDiemViewModel = hiltViewModel()
) {
    var itemName by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food & Beverages") }
    var unit by remember { mutableStateOf("unit") }
    var expandedCategory by remember { mutableStateOf(false) }

    val categories = listOf("Food & Beverages", "Household Supplies", "Utilities", "Other")
    
    val houseConfig by viewModel.houseConfig.collectAsState()
    val currencySymbol = houseConfig?.currencyCode?.let { getCurrencySymbol(it) } ?: "$"

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(houseId) {
        viewModel.loadHouseConfig(houseId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Add Per-Diem Item",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Item Details",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = itemName,
                onValueChange = { itemName = it },
                label = { Text("Item Name *") },
                placeholder = { Text("e.g., Milk, Coffee") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                value = rate,
                onValueChange = { rate = it },
                label = { Text("Rate per Unit *") },
                prefix = { Text(currencySymbol) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            OutlinedTextField(
                value = unit,
                onValueChange = { unit = it },
                label = { Text("Unit *") },
                placeholder = { Text("e.g., liter, pack, cup") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            ExposedDropdownMenuBox(
                expanded = expandedCategory,
                onExpandedChange = { expandedCategory = !expandedCategory }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category *") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                    shape = MaterialTheme.shapes.medium
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

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val rateValue = rate.toBigDecimalOrNull()
                    if (rateValue != null && itemName.isNotBlank() && unit.isNotBlank()) {
                        scope.launch {
                            viewModel.createConfig(houseId, itemName, rateValue, category, unit)
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = itemName.isNotBlank() && rate.toBigDecimalOrNull() != null && unit.isNotBlank(),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add Item", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
