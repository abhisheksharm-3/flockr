package `in`.xroden.flockr.ui.screens.home

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
import `in`.xroden.flockr.data.model.House
import `in`.xroden.flockr.ui.components.ScreenLogger
import `in`.xroden.flockr.ui.components.buttons.FlockrPrimaryButton
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.ui.components.logUserAction
import `in`.xroden.flockr.ui.components.logScreenState
import `in`.xroden.flockr.ui.components.logScreenError
import `in`.xroden.flockr.ui.theme.PositiveGreen
import `in`.xroden.flockr.ui.viewmodel.HomeViewModel

private const val SCREEN_NAME = "CreateHouse"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateHouseScreen(
    onHouseCreated: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    ScreenLogger(screenName = SCREEN_NAME) {
        CreateHouseScreenContent(onHouseCreated, onNavigateBack, viewModel)
    }
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

    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        if (uiState is `in`.xroden.flockr.ui.viewmodel.HomeUiState.Error) {
            val msg = (uiState as `in`.xroden.flockr.ui.viewmodel.HomeUiState.Error).message
            logScreenError(SCREEN_NAME, msg)
            isCreating = false
            snackbarHostState.showSnackbar(msg)
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
                        logUserAction(SCREEN_NAME, "Create Button Clicked", "name=$houseName, hasAddress=${address.isNotBlank()}")
                        
                        if (houseName.isBlank()) {
                            nameError = "Name is required"
                            logScreenError(SCREEN_NAME, "Validation failed: Name is required")
                            return@FlockrPrimaryButton
                        }
                        if (houseName.length < 2) {
                            nameError = "Name must be at least 2 characters"
                            logScreenError(SCREEN_NAME, "Validation failed: Name too short")
                            return@FlockrPrimaryButton
                        }
                        
                        logScreenState(SCREEN_NAME, "Creating", "name=$houseName, currency=$currency")
                        isCreating = true
                        val currencySymbol = currencies.find { it.first == currency }?.second ?: "$"
                        viewModel.createHouse(
                            name = houseName,
                            address = address.takeIf { it.isNotBlank() },
                            latitude = null,
                            longitude = null,
                            currencyCode = currency,
                            currencySymbol = currencySymbol,
                            onSuccess = { house ->
                                logScreenState(SCREEN_NAME, "Created Successfully", "houseId=${house.id}, inviteCode=${house.inviteCode}")
                                isCreating = false
                                createdHouse = house
                                showSuccessDialog = true
                            }
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
            shape = RoundedCornerShape(16.dp),
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
                        .clip(RoundedCornerShape(32.dp))
                        .background(PositiveGreen.copy(alpha = 0.1f))
                        .border(2.dp, PositiveGreen, RoundedCornerShape(32.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = PositiveGreen,
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
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
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
                    shape = RoundedCornerShape(10.dp)
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
