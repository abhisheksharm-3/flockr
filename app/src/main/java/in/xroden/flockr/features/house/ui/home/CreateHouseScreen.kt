package `in`.xroden.flockr.features.house.ui.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.features.house.model.House
import `in`.xroden.flockr.ui.components.buttons.FlockrPrimaryButton
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.features.house.domain.HomeViewModel

private const val SCREEN_NAME = "CreateHouse"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHouseScreen(
    onHouseCreated: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    CreateHouseScreenContent(onHouseCreated, onNavigateBack, viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateHouseScreenContent(
    onHouseCreated: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: HomeViewModel
) {
    var houseName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD") }
    var expandedCurrency by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }
    var createdHouse by remember { mutableStateOf<House?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }

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

    val createState by viewModel.createState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(createState) {
        when (val state = createState) {
            is `in`.xroden.flockr.features.house.domain.CreateHouseUiState.Success -> {
                isCreating = false
                createdHouse = state.house
                showSuccessDialog = true
            }
            is `in`.xroden.flockr.features.house.domain.CreateHouseUiState.Error -> {
                isCreating = false
                snackbarHostState.showSnackbar(state.message)
            }
            is `in`.xroden.flockr.features.house.domain.CreateHouseUiState.Loading -> {
                isCreating = true
            }
            is `in`.xroden.flockr.features.house.domain.CreateHouseUiState.Idle -> {
                // Do nothing
            }
        }
    }

    // Show success dialog
    if (showSuccessDialog && createdHouse != null) {
        HouseCreatedSuccessDialog(
            house = createdHouse!!,
            onDismiss = {
                showSuccessDialog = false
                onHouseCreated(createdHouse!!.id)
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Create Household",
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
                ),
                windowInsets = androidx.compose.foundation.layout.WindowInsets.systemBars
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets.systemBars
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(bottom = 16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    text = "Create Household",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Set up a new household to manage together. You'll receive an invite code to share with others.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Form Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Household Name Field
                Column {
                    Text(
                        text = "Household Name *",
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

                // Currency Field
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
                }

                // Address Field
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
                    Text(
                        text = "You can add this later from household settings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Create Button
                FlockrPrimaryButton(
                    text = if (isCreating) "Creating..." else "Create Household",
                    onClick = {
                        if (houseName.isBlank()) {
                            nameError = "Name is required"
                            return@FlockrPrimaryButton
                        }
                        if (houseName.length < 2) {
                            nameError = "Name must be at least 2 characters"
                            return@FlockrPrimaryButton
                        }
                        
                        isCreating = true
                        viewModel.createHouse(
                            name = houseName,
                            address = address.takeIf { it.isNotBlank() },
                            latitude = null,
                            longitude = null,
                            currencyCode = currency
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = houseName.isNotBlank() && !isCreating && nameError == null,
                    isLoading = isCreating
                )
            }
        }
    }
}

@Composable
fun HouseCreatedSuccessDialog(
    house: House,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Success Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f))
                        .border(2.dp, MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.extraLarge),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Title
                Text(
                    text = "Household Created!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                // House Name
                Text(
                    text = house.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                HorizontalDivider()

                // Invite Code Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Invite Code",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                MaterialTheme.shapes.medium
                            )
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = house.inviteCode ?: "N/A",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 4.sp
                        )
                    }

                    Text(
                        text = "Share this code with others to invite them to your household",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // Continue Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "Continue to Household",
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}
