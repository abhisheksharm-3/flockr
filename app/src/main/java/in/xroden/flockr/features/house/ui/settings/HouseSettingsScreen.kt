package `in`.xroden.flockr.features.house.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import `in`.xroden.flockr.features.house.model.House
import `in`.xroden.flockr.ui.components.cards.SectionCard
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.features.house.domain.HouseSettingsViewModel

/**
 * House Settings Screen
 * Only accessible to Owners and Admins
 * Allows editing house details, currency, and other settings
 */
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseSettingsScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAuditLog: () -> Unit = {},
    onDeleteHouse: () -> Unit = {},
    viewModel: HouseSettingsViewModel = hiltViewModel()
) {
    // UI State
    val settingsUiState by viewModel.uiState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    var house by remember { mutableStateOf<House?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var currentUserId by remember { mutableStateOf<String?>(null) }
    
    // Image Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadHeaderImage(houseId, uri)
        }
    }

    // Form fields
    var houseName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD") }
    var expandedCurrency by remember { mutableStateOf(false) }
    var dateFormat by remember { mutableStateOf("YYYY-MM-DD") }
    var expandedDateFormat by remember { mutableStateOf(false) }
    var firstDayOfWeek by remember { mutableStateOf(0) }
    var expandedFirstDay by remember { mutableStateOf(false) }
    var timezone by remember { mutableStateOf("UTC") }
    var expandedTimezone by remember { mutableStateOf(false) }
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
        viewModel.loadHouseSettings(houseId)
    }

    LaunchedEffect(settingsUiState) {
        when (val state = settingsUiState) {
            is `in`.xroden.flockr.features.settings.domain.HouseSettingsUiState.Success -> {
                isLoading = false
                currency = state.config.currencyCode
                dateFormat = state.config.dateFormat
                firstDayOfWeek = state.config.firstDayOfWeek
                timezone = state.config.timezone
                // Load house separately for house name and address
                scope.launch {
                    val loadedHouse = viewModel.getHouse(houseId)
                    house = loadedHouse
                    loadedHouse?.let {
                        houseName = it.name
                        address = it.address ?: ""
                        currentUserId = viewModel.getCurrentUserId()
                    }
                }
            }
            is `in`.xroden.flockr.features.settings.domain.HouseSettingsUiState.Error -> {
                isLoading = false
            }
            is `in`.xroden.flockr.features.settings.domain.HouseSettingsUiState.Loading -> {
                isLoading = true
            }
        }
    }

    // Observe update state for success/error
    LaunchedEffect(updateState) {
        when (val state = updateState) {
            is `in`.xroden.flockr.features.settings.domain.UpdateHouseSettingsUiState.Success -> {
                isSaving = false
                snackbarHostState.showSnackbar("Settings saved successfully")
                onNavigateBack()
            }
            is `in`.xroden.flockr.features.settings.domain.UpdateHouseSettingsUiState.Error -> {
                isSaving = false
                snackbarHostState.showSnackbar(state.message)
            }
            is `in`.xroden.flockr.features.settings.domain.UpdateHouseSettingsUiState.Loading -> {
                isSaving = true
            }
            is `in`.xroden.flockr.features.settings.domain.UpdateHouseSettingsUiState.Idle -> {
                // Do nothing
            }
        }
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets.systemBars,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "House Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
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
                actions = {
                    TextButton(
                        onClick = {
                            if (houseName.length < 2) {
                                nameError = "Name must be at least 2 characters"
                                return@TextButton
                            }

                            isSaving = true
                            scope.launch {
                                val nameChanged = houseName != house?.name
                                val addressChanged = address != (house?.address ?: "")

                                if (nameChanged || addressChanged) {
                                    viewModel.updateHouse(
                                        houseId = houseId,
                                        name = if (nameChanged) houseName else null,
                                        address = if (addressChanged) address.takeIf { it.isNotBlank() } else null
                                    )
                                }

                                viewModel.updateHouseConfig(
                                    houseId = houseId,
                                    currencyCode = currency,
                                    dateFormat = dateFormat,
                                    firstDayOfWeek = firstDayOfWeek,
                                    timezone = timezone
                                )

                                isSaving = false
                                snackbarHostState.showSnackbar("Settings saved")
                            }
                        },
                        enabled = !isSaving && nameError == null && houseName.isNotBlank()
                    ) {
                        if (isSaving) {
                             CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                             Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // bottomBar removed
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Edit House Details",
                        style = MaterialTheme.typography.headlineMedium,
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

                // Header Image Section
                SectionCard(title = "Header Image") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (house?.headerImageUrl != null) {
                            AsyncImage(
                                model = house?.headerImageUrl,
                                contentDescription = "Header Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(MaterialTheme.shapes.medium),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No header image",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Add a header image to personalize your household",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upload Header Image", fontWeight = FontWeight.SemiBold)
                        }
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
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
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
                                    androidx.compose.runtime.key(code) {
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
                        Text(
                            text = "This will be used for all expense displays",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Date Format
                        Text(
                            text = "Date Format",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        ExposedDropdownMenuBox(
                            expanded = expandedDateFormat,
                            onExpandedChange = { expandedDateFormat = !expandedDateFormat }
                        ) {
                            FlockrTextField(
                                value = dateFormat,
                                onValueChange = {},
                                readOnly = true,
                                placeholder = "Select date format",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDateFormat)
                                }
                            )
                            ExposedDropdownMenu(
                                expanded = expandedDateFormat,
                                onDismissRequest = { expandedDateFormat = false }
                            ) {
                                listOf("YYYY-MM-DD", "DD/MM/YYYY", "MM/DD/YYYY", "DD-MM-YYYY").forEach { format ->
                                    DropdownMenuItem(
                                        text = { Text(format) },
                                        onClick = {
                                            dateFormat = format
                                            expandedDateFormat = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // First Day of Week
                        Text(
                            text = "First Day of Week",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        ExposedDropdownMenuBox(
                            expanded = expandedFirstDay,
                            onExpandedChange = { expandedFirstDay = !expandedFirstDay }
                        ) {
                            val days = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
                            FlockrTextField(
                                value = days[firstDayOfWeek],
                                onValueChange = {},
                                readOnly = true,
                                placeholder = "Select first day",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFirstDay)
                                }
                            )
                            ExposedDropdownMenu(
                                expanded = expandedFirstDay,
                                onDismissRequest = { expandedFirstDay = false }
                            ) {
                                days.forEachIndexed { index, day ->
                                    DropdownMenuItem(
                                        text = { Text(day) },
                                        onClick = {
                                            firstDayOfWeek = index
                                            expandedFirstDay = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Timezone
                        Text(
                            text = "Timezone",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        ExposedDropdownMenuBox(
                            expanded = expandedTimezone,
                            onExpandedChange = { expandedTimezone = !expandedTimezone }
                        ) {
                            val timezones = listOf(
                                "UTC",
                                "America/New_York",
                                "America/Chicago",
                                "America/Denver",
                                "America/Los_Angeles",
                                "Europe/London",
                                "Europe/Paris",
                                "Asia/Tokyo",
                                "Asia/Shanghai",
                                "Asia/Kolkata",
                                "Australia/Sydney"
                            )
                            FlockrTextField(
                                value = timezone,
                                onValueChange = {},
                                readOnly = true,
                                placeholder = "Select timezone",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTimezone)
                                }
                            )
                            ExposedDropdownMenu(
                                expanded = expandedTimezone,
                                onDismissRequest = { expandedTimezone = false }
                            ) {
                                timezones.forEach { tz ->
                                    androidx.compose.runtime.key(tz) {
                                        DropdownMenuItem(
                                            text = { Text(tz) },
                                            onClick = {
                                                timezone = tz
                                                expandedTimezone = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // House Information Card
                house?.let { h ->
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

                // Activity Log Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Activity Log",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "View all house activities",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = onNavigateToAuditLog,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View Activity Log", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Delete House Section (Owner Only)
                if (house?.ownerId == currentUserId) {
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(24.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Danger Zone",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete House", fontWeight = FontWeight.SemiBold)
                        }

                        Text(
                            text = "This will permanently delete the house and all its data",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    "Delete House?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Are you sure you want to delete \"${house?.name}\"?",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        "This action cannot be undone. All expenses, balances, and house data will be permanently deleted.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isSaving = true
                            val result = viewModel.deleteHouse(houseId)
                            isSaving = false
                            showDeleteDialog = false
                            if (result.isSuccess) {
                                // Redirect immediately
                                onDeleteHouse()
                                // Show snackbar after redirect (user won't see it but it's for consistency)
                                snackbarHostState.showSnackbar("House deleted")
                            } else {
                                snackbarHostState.showSnackbar(
                                    result.exceptionOrNull()?.message ?: "Failed to delete house"
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = !isSaving,
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Delete", fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large
        )
    }
}
