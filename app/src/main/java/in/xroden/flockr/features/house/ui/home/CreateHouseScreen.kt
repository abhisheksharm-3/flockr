package `in`.xroden.flockr.features.house.ui.home


import android.util.Log
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.animation.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
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
    var currentStep by remember { mutableIntStateOf(0) }

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
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 0) {
                            currentStep--
                        } else {
                            onNavigateBack()
                        }
                    }) {
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
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets.systemBars,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Step Indicator
                    (0..2).forEach { step ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (step <= currentStep) 
                                        MaterialTheme.colorScheme.primary 
                                    else 
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (currentStep < 2) {
                        Button(
                            onClick = { 
                                when (currentStep) {
                                    0 -> {
                                        if (houseName.isBlank()) {
                                            nameError = "Name is required"
                                        } else if (houseName.length < 2) {
                                            nameError = "Name must be at least 2 characters"
                                        } else {
                                            currentStep++
                                        }
                                    }
                                    1 -> currentStep++
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Continue", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                        }
                    } else {
                        // Create Button
                        Button(
                            onClick = {
                                isCreating = true
                                viewModel.createHouse(
                                    name = houseName,
                                    address = address.takeIf { it.isNotBlank() },
                                    latitude = null,
                                    longitude = null,
                                    currencyCode = currency
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = !isCreating,
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (isCreating) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Create Household", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.Check, null)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        androidx.compose.animation.AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    androidx.compose.animation.slideInHorizontally { width -> width } + androidx.compose.animation.fadeIn() togetherWith
                        androidx.compose.animation.slideOutHorizontally { width -> -width } + androidx.compose.animation.fadeOut()
                } else {
                    androidx.compose.animation.slideInHorizontally { width -> -width } + androidx.compose.animation.fadeIn() togetherWith
                        androidx.compose.animation.slideOutHorizontally { width -> width } + androidx.compose.animation.fadeOut()
                }
            },
            modifier = Modifier.padding(padding)
        ) { step ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                when (step) {
                    0 -> {
                        // Step 1: Basic Info
                        Text(
                            text = "Let's start with the basics",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Give your household a name to easily identify it.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Household Name Field
                        Column {
                            Text(
                                text = "Household Name *",
                                style = MaterialTheme.typography.labelLarge,
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
                                placeholder = "e.g., Smith Family, Downtown Apt",
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = {
                                    Icon(Icons.Default.Home, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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

                        // Address Field
                        Column {
                            Text(
                                text = "Address (Optional)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            FlockrTextField(
                                value = address,
                                onValueChange = { address = it },
                                placeholder = "123 Main St, City, State",
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = {
                                    Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            )
                        }
                    }
                    1 -> {
                        // Step 2: Settings
                        Text(
                            text = "Customize your experience",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Set up your preferred currency for expense tracking.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Currency Field
                        Column {
                            Text(
                                text = "Currency",
                                style = MaterialTheme.typography.labelLarge,
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
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    leadingIcon = {
                                        Icon(Icons.Default.AttachMoney, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    }
                    2 -> {
                        // Step 3: Review
                        Text(
                            text = "Review & Create",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Make sure everything looks correct before creating your household.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            shape = MaterialTheme.shapes.large,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Home, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text("Name", style = MaterialTheme.typography.labelMedium)
                                        Text(houseName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AttachMoney, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text("Currency", style = MaterialTheme.typography.labelMedium)
                                        Text(currency, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (address.isNotBlank()) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text("Address", style = MaterialTheme.typography.labelMedium)
                                            Text(address, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
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
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f))
                        .border(2.dp, MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.medium),
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
                        modifier = Modifier.padding(vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
