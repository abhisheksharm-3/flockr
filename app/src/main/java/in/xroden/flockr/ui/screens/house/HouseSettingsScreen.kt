package `in`.xroden.flockr.ui.screens.house

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.launch
import `in`.xroden.flockr.data.model.House
import `in`.xroden.flockr.ui.components.cards.SectionCard
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.ui.viewmodel.HouseManagementViewModel
import `in`.xroden.flockr.ui.viewmodel.HouseSettingsViewModel

/**
 * House Settings Screen
 * Only accessible to Owners and Admins
 * Allows editing house details, currency, and other settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseSettingsScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: HouseSettingsViewModel = hiltViewModel(),
    houseManagementViewModel: HouseManagementViewModel = hiltViewModel()
) {
    var house by remember { mutableStateOf<House?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Form fields
    var houseName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD") }
    var expandedCurrency by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currencies = listOf(
        "USD" to "$",
        "EUR" to "€",
        "GBP" to "£",
        "JPY" to "¥",
        "INR" to "₹",
        "CAD" to "C$",
        "AUD" to "A$",
        "CNY" to "¥"
    )

    LaunchedEffect(houseId) {
        isLoading = true
        scope.launch {
            houseManagementViewModel.loadHouse(houseId)
            val loadedHouse = houseManagementViewModel.currentHouse.value
            if (loadedHouse != null) {
                house = loadedHouse
                houseName = loadedHouse.name
                address = loadedHouse.address ?: ""
            }
            
            // Load house config for currency
            val config = viewModel.getHouseConfig(houseId)
            if (config != null) {
                currency = config.currencyCode
            }
            
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "House Settings",
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
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Edit House Details",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Update house information and preferences",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Basic Information
                SectionCard(title = "Basic Information") {
                    // House Name
                    Column {
                        Text(
                            text = "House Name *",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        FlockrTextField(
                            value = houseName,
                            onValueChange = { 
                                houseName = it
                                nameError = when {
                                    it.isBlank() -> "Name is required"
                                    it.length < 2 -> "Name must be at least 2 characters"
                                    else -> null
                                }
                            },
                            placeholder = "e.g., Smith Family, Downtown Apartment",
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            isError = nameError != null
                        )
                        nameError?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Address
                    Column {
                        Text(
                            text = "Address (Optional)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        FlockrTextField(
                            value = address,
                            onValueChange = { address = it },
                            placeholder = "123 Main St, City, State",
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }

                // Currency Settings
                SectionCard(title = "Currency & Localization") {
                    Column {
                        Text(
                            text = "Currency",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        ExposedDropdownMenuBox(
                            expanded = expandedCurrency,
                            onExpandedChange = { expandedCurrency = !expandedCurrency }
                        ) {
                            FlockrTextField(
                                value = "${currencies.find { it.first == currency }?.second} $currency",
                                onValueChange = {},
                                readOnly = true,
                                placeholder = "Select currency",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.AttachMoney,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCurrency)
                                }
                            )
                            ExposedDropdownMenu(
                                expanded = expandedCurrency,
                                onDismissRequest = { expandedCurrency = false }
                            ) {
                                currencies.forEach { (code, symbol) ->
                                    DropdownMenuItem(
                                        text = { Text("$symbol $code") },
                                        onClick = {
                                            currency = code
                                            expandedCurrency = false
                                        }
                                    )
                                }
                            }
                        }
                        Text(
                            text = "This will be used for all expense displays",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                }

                // House Information Card
                house?.let { h ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "House Information",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    h.inviteCode?.let { code ->
                                        Text(
                                            text = "Invite Code: $code",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Save Button
                Button(
                    onClick = {
                        if (houseName.isBlank()) {
                            nameError = "Name is required"
                            return@Button
                        }
                        if (houseName.length < 2) {
                            nameError = "Name must be at least 2 characters"
                            return@Button
                        }

                        isSaving = true
                        scope.launch {
                            val nameChanged = houseName != house?.name
                            val addressChanged = address != (house?.address ?: "")
                            val currencySymbol = currencies.find { it.first == currency }?.second ?: "$"

                            var success = true
                            
                            // Update house details if changed
                            if (nameChanged || addressChanged) {
                                val result = viewModel.updateHouse(
                                    houseId = houseId,
                                    name = if (nameChanged) houseName else null,
                                    address = if (addressChanged) address.takeIf { it.isNotBlank() } else null
                                )
                                success = result.isSuccess
                            }

                            // Update currency if changed
                            if (success) {
                                val result = viewModel.updateCurrency(
                                    houseId = houseId,
                                    currencyCode = currency,
                                    currencySymbol = currencySymbol
                                )
                                success = result.isSuccess
                            }

                            isSaving = false
                            
                            if (success) {
                                snackbarHostState.showSnackbar("Settings saved successfully")
                                onNavigateBack()
                            } else {
                                snackbarHostState.showSnackbar("Failed to save settings")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isSaving && nameError == null && houseName.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSaving) {
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
                            "Save Changes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

