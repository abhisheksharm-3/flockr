package `in`.xroden.flockr.features.house.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout

import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import `in`.xroden.flockr.features.house.model.House
import `in`.xroden.flockr.ui.components.forms.FormSectionCard
import androidx.compose.foundation.shape.RoundedCornerShape
import `in`.xroden.flockr.ui.components.loading.ListScreenSkeleton
import `in`.xroden.flockr.features.house.presentation.HouseSettingsViewModel
import `in`.xroden.flockr.features.house.presentation.HouseSettingsUiState
import `in`.xroden.flockr.features.house.presentation.UpdateHouseSettingsUiState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.utils.rememberHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseSettingsScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAuditLog: () -> Unit = {},
    onDeleteHouse: () -> Unit = {},
    viewModel: HouseSettingsViewModel = hiltViewModel()
) {
    val haptics = rememberHaptics()
    // UI State
    val settingsUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    var house by remember { mutableStateOf<House?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
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
    var dateFormat by remember { mutableStateOf("YYYY-MM-DD") }
    var firstDayOfWeek by remember { mutableStateOf(0) }
    var timezone by remember { mutableStateOf("UTC") }
    var nameError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(houseId) {
        viewModel.loadHouseSettings(houseId)
    }

    LaunchedEffect(settingsUiState) {
        when (val state = settingsUiState) {
            is HouseSettingsUiState.Success -> {
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
            is HouseSettingsUiState.Error -> {
                isLoading = false
            }
            is HouseSettingsUiState.Loading -> {
                isLoading = true
            }
        }
    }

    // Observe update state for success/error
    LaunchedEffect(updateState) {
        when (val state = updateState) {
            is UpdateHouseSettingsUiState.Success -> {
                isSaving = false
                haptics.success()
                viewModel.resetUpdateState()
                snackbarHostState.showSnackbar("Settings saved successfully")
                onNavigateBack()
            }
            is UpdateHouseSettingsUiState.Error -> {
                isSaving = false
                haptics.error()
                viewModel.resetUpdateState()
                snackbarHostState.showSnackbar(state.message)
            }
            is UpdateHouseSettingsUiState.Loading -> {
                isSaving = true
            }
            is UpdateHouseSettingsUiState.Idle -> Unit
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            HouseSettingsTopBar(
                isSaving = isSaving,
                saveEnabled = !isSaving && nameError == null && houseName.isNotBlank(),
                onNavigateBack = onNavigateBack,
                onSave = {
                    isSaving = true
                    scope.launch {
                        val nameChanged = houseName != house?.name
                        val addressChanged = address != (house?.address ?: "")
                        // Single awaited save so house + config both persist; navigation on
                        // success (LaunchedEffect below) can no longer cancel a pending write.
                        val result = viewModel.saveSettings(
                            houseId = houseId,
                            name = if (nameChanged) houseName else null,
                            address = if (addressChanged) address.takeIf { it.isNotBlank() } else null,
                            currencyCode = currency,
                            dateFormat = dateFormat,
                            firstDayOfWeek = firstDayOfWeek,
                            timezone = timezone
                        )
                        isSaving = false
                        if (result.isFailure) {
                            snackbarHostState.showSnackbar(
                                result.exceptionOrNull()?.message ?: "Failed to save settings"
                            )
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // bottomBar removed
    ) { padding ->
        if (isLoading) {
            ListScreenSkeleton(modifier = Modifier.fillMaxSize().padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                BasicInformationSection(
                    houseName = houseName,
                    nameError = nameError,
                    onHouseNameChange = {
                        houseName = it
                        nameError = when {
                            it.isBlank() -> "Name is required"
                            it.length < 2 -> "Name must be at least 2 characters"
                            else -> null
                        }
                    },
                    address = address,
                    onAddressChange = { address = it }
                )

                HeaderImageSection(
                    headerImageUrl = house?.headerImageUrl,
                    onUploadClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )

                LocalizationSection(
                    currency = currency,
                    onCurrencyChange = { currency = it },
                    dateFormat = dateFormat,
                    onDateFormatChange = { dateFormat = it },
                    firstDayOfWeek = firstDayOfWeek,
                    onFirstDayChange = { firstDayOfWeek = it },
                    timezone = timezone,
                    onTimezoneChange = { timezone = it }
                )

                house?.let { h ->
                    HouseInformationCard(house = h)
                }

                ActivityLogSection(onNavigateToAuditLog = onNavigateToAuditLog)

                // Delete House Section (Owner Only)
                if (house?.ownerId == currentUserId) {
                    DangerZoneSection(onDeleteClick = { showDeleteDialog = true })
                } else if (currentUserId != null) {
                    // Leave House Section (non-owners)
                    LeaveHouseSection(onLeaveClick = { showLeaveDialog = true })
                }
            }
        }
    }

    // Leave Confirmation Dialog
    if (showLeaveDialog) {
        LeaveHouseDialog(
            houseName = house?.name,
            isSaving = isSaving,
            onDismiss = { showLeaveDialog = false },
            onConfirm = {
                scope.launch {
                    isSaving = true
                    val result = viewModel.leaveHouse(houseId)
                    isSaving = false
                    showLeaveDialog = false
                    if (result.isSuccess) {
                        onDeleteHouse()
                    } else {
                        snackbarHostState.showSnackbar(
                            result.exceptionOrNull()?.message ?: "Failed to leave house"
                        )
                    }
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        DeleteHouseDialog(
            houseName = house?.name,
            isSaving = isSaving,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
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
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HouseSettingsTopBar(
    isSaving: Boolean,
    saveEnabled: Boolean,
    onNavigateBack: () -> Unit,
    onSave: () -> Unit
) {
    val haptics = rememberHaptics()
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
                onClick = { haptics.tap(); onSave() },
                enabled = saveEnabled
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
}

@Composable
private fun BasicInformationSection(
    houseName: String,
    nameError: String?,
    onHouseNameChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit
) {
    FormSectionCard(
        title = "Basic Information",
        icon = Icons.Default.Home,
        iconTint = MaterialTheme.colorScheme.primary
    ) {
        // House Name
        OutlinedTextField(
            value = houseName,
            onValueChange = onHouseNameChange,
            label = { Text("House Name *") },
            placeholder = { Text("e.g., Smith Family, Downtown Apartment") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = nameError != null,
            supportingText = nameError?.let { { Text(it) } },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Address
        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            label = { Text("Address (Optional)") },
            placeholder = { Text("123 Main St, City, State") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
    }
}

@Composable
private fun HeaderImageSection(
    headerImageUrl: String?,
    onUploadClick: () -> Unit
) {
    FormSectionCard(
        title = "Header Image",
        icon = Icons.Default.Image,
        iconTint = MaterialTheme.colorScheme.tertiary
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (headerImageUrl != null) {
                AsyncImage(
                    model = headerImageUrl,
                    contentDescription = "Header Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp),
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
                onClick = onUploadClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Upload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload Header Image", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalizationSection(
    currency: String,
    onCurrencyChange: (String) -> Unit,
    dateFormat: String,
    onDateFormatChange: (String) -> Unit,
    firstDayOfWeek: Int,
    onFirstDayChange: (Int) -> Unit,
    timezone: String,
    onTimezoneChange: (String) -> Unit
) {
    var expandedCurrency by remember { mutableStateOf(false) }
    var expandedDateFormat by remember { mutableStateOf(false) }
    var expandedFirstDay by remember { mutableStateOf(false) }
    var expandedTimezone by remember { mutableStateOf(false) }

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

    FormSectionCard(
        title = "Currency & Localization",
        icon = Icons.Default.Language,
        iconTint = MaterialTheme.colorScheme.secondary
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Currency
            ExposedDropdownMenuBox(
                expanded = expandedCurrency,
                onExpandedChange = { expandedCurrency = !expandedCurrency }
            ) {
                OutlinedTextField(
                    value = "${currencies.find { it.first == currency }?.second} $currency",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Currency") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCurrency)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
                ExposedDropdownMenu(
                    expanded = expandedCurrency,
                    onDismissRequest = { expandedCurrency = false }
                ) {
                    currencies.forEach { (code, symbol) ->
                        key(code) {
                            DropdownMenuItem(
                                text = { Text("$symbol $code") },
                                onClick = {
                                    onCurrencyChange(code)
                                    expandedCurrency = false
                                }
                            )
                        }
                    }
                }
            }

            // Date Format
            ExposedDropdownMenuBox(
                expanded = expandedDateFormat,
                onExpandedChange = { expandedDateFormat = !expandedDateFormat }
            ) {
                OutlinedTextField(
                    value = dateFormat,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date Format") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDateFormat)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
                ExposedDropdownMenu(
                    expanded = expandedDateFormat,
                    onDismissRequest = { expandedDateFormat = false }
                ) {
                    listOf("YYYY-MM-DD", "DD/MM/YYYY", "MM/DD/YYYY", "DD-MM-YYYY").forEach { format ->
                        DropdownMenuItem(
                            text = { Text(format) },
                            onClick = {
                                onDateFormatChange(format)
                                expandedDateFormat = false
                            }
                        )
                    }
                }
            }

            // First Day of Week
            ExposedDropdownMenuBox(
                expanded = expandedFirstDay,
                onExpandedChange = { expandedFirstDay = !expandedFirstDay }
            ) {
                val days = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
                OutlinedTextField(
                    value = days[firstDayOfWeek],
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("First Day of Week") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFirstDay)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
                ExposedDropdownMenu(
                    expanded = expandedFirstDay,
                    onDismissRequest = { expandedFirstDay = false }
                ) {
                    days.forEachIndexed { index, day ->
                        DropdownMenuItem(
                            text = { Text(day) },
                            onClick = {
                                onFirstDayChange(index)
                                expandedFirstDay = false
                            }
                        )
                    }
                }
            }

            // Timezone
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
                OutlinedTextField(
                    value = timezone,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Timezone") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTimezone)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
                ExposedDropdownMenu(
                    expanded = expandedTimezone,
                    onDismissRequest = { expandedTimezone = false }
                ) {
                    timezones.forEach { tz ->
                        key(tz) {
                            DropdownMenuItem(
                                text = { Text(tz) },
                                onClick = {
                                    onTimezoneChange(tz)
                                    expandedTimezone = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HouseInformationCard(house: House) {
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
                    house.inviteCode?.let { code ->
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

@Composable
private fun ActivityLogSection(onNavigateToAuditLog: () -> Unit) {
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
}

@Composable
private fun DangerZoneSection(onDeleteClick: () -> Unit) {
    val haptics = rememberHaptics()
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
            onClick = { haptics.tap(); onDeleteClick() },
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

@Composable
private fun LeaveHouseSection(onLeaveClick: () -> Unit) {
    val haptics = rememberHaptics()
    Spacer(modifier = Modifier.height(24.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    Spacer(modifier = Modifier.height(24.dp))

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = { haptics.tap(); onLeaveClick() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Leave House", fontWeight = FontWeight.SemiBold)
        }
        Text(
            text = "You will lose access to this house until you're invited again",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun LeaveHouseDialog(
    houseName: String?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val haptics = rememberHaptics()
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Leave House?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) },
        text = {
            Text(
                "Are you sure you want to leave \"${houseName}\"? You'll need a new invite to rejoin.",
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            Button(
                onClick = { haptics.error(); onConfirm() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
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
                    Text("Leave", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun DeleteHouseDialog(
    houseName: String?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val haptics = rememberHaptics()
    AlertDialog(
        onDismissRequest = onDismiss,
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
                    "Are you sure you want to delete \"${houseName}\"?",
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
                onClick = { haptics.error(); onConfirm() },
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
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontWeight = FontWeight.SemiBold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large
    )
}
