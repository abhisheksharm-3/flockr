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
import androidx.compose.material3.ExposedDropdownMenuAnchorType
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
import `in`.xroden.flockr.features.house.domain.CreateHouseUiState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import coil.compose.AsyncImage

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

    // Image Upload State
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
    
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        selectedImageUri = uri
        if (uri != null) {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    imageBytes = inputStream.readBytes()
                }
            } catch (e: Exception) {
                Log.e(SCREEN_NAME, "Error reading image", e)
            }
        }
    }
    
    // Localization
    var currency by remember { mutableStateOf("USD") }
    var currencyExpanded by remember { mutableStateOf(false) }
    var dateFormat by remember { mutableStateOf("dd/MM/yyyy") }
    var dateFormatExpanded by remember { mutableStateOf(false) }
    var timezone by remember { mutableStateOf(kotlinx.datetime.TimeZone.currentSystemDefault().id) }
    var timezoneExpanded by remember { mutableStateOf(false) }
    
    var isCreating by remember { mutableStateOf(false) }
    var createdHouse by remember { mutableStateOf<House?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var currentStep by remember { mutableIntStateOf(0) }

    val currencies = listOf(
        "USD" to "$", "EUR" to "€", "GBP" to "£", "JPY" to "¥", "INR" to "₹", "CAD" to "C$", "AUD" to "A$", "CNY" to "¥"
    )
    
    val dateFormats = listOf("dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd")

    val createState by viewModel.createState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(createState) {
        when (val state = createState) {
            is CreateHouseUiState.Success -> {
                isCreating = false
                createdHouse = state.house
                showSuccessDialog = true
            }
            is CreateHouseUiState.Error -> {
                isCreating = false
                snackbarHostState.showSnackbar(state.message)
            }
            is CreateHouseUiState.Loading -> isCreating = true
            else -> {}
        }
    }

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
                title = { Text("Create Household", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 0) currentStep-- else onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(24.dp)) {
                // Progress
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0..3).forEach { step ->
                        Box(
                            modifier = Modifier.weight(1f).height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (step <= currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }

                Button(
                    onClick = {
                        when (currentStep) {
                            0 -> if (houseName.isBlank()) nameError = "Required" else currentStep++
                            1 -> currentStep++ // Address optional
                            2 -> currentStep++ // Localization
                            3 -> viewModel.createHouse(
                                houseName, 
                                address.ifBlank { null }, 
                                null, 
                                null, 
                                currency, 
                                dateFormat, 
                                1, 
                                timezone,
                                headerImageBytes = imageBytes
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    enabled = !isCreating
                ) {
                    if (isCreating) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text(if (currentStep == 3) "Create Household" else "Continue")
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith slideOutHorizontally { width -> -width } + fadeOut()
                } else {
                    slideInHorizontally { width -> -width } + fadeIn() togetherWith slideOutHorizontally { width -> width } + fadeOut()
                }
            },
            modifier = Modifier.padding(padding)
        ) { step ->
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
                when (step) {
                    0 -> { // Name & Image
                        Text("Theme & Identity", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Name your household and add a cover image.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(24.dp))
                        
                        // Image Picker Step 0
                         Card(
                            onClick = { 
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                             Box(Modifier.fillMaxSize()) {
                                 if (selectedImageUri != null) {
                                     AsyncImage(
                                         model = selectedImageUri,
                                         contentDescription = "Selected Cover Image",
                                         modifier = Modifier.fillMaxSize(),
                                         contentScale = ContentScale.Crop
                                     )
                                     // Overlay
                                     Box(
                                         modifier = Modifier
                                             .fillMaxSize()
                                             .background(Color.Black.copy(alpha = 0.3f)),
                                         contentAlignment = Alignment.Center
                                     ) {
                                         Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                 imageVector = Icons.Default.Edit, 
                                                 contentDescription = null,
                                                 tint = Color.White,
                                                 modifier = Modifier.size(32.dp)
                                             )
                                             Text("Change Image", color = Color.White, style = MaterialTheme.typography.labelMedium)
                                         }
                                     }
                                 } else {
                                     Column(
                                         modifier = Modifier.align(Alignment.Center),
                                         horizontalAlignment = Alignment.CenterHorizontally, 
                                         verticalArrangement = Arrangement.Center
                                     ) {
                                         Icon(
                                             imageVector = Icons.Default.AddPhotoAlternate, 
                                             contentDescription = null,
                                             tint = MaterialTheme.colorScheme.primary,
                                             modifier = Modifier.size(40.dp)
                                         )
                                         Spacer(Modifier.height(12.dp))
                                         Text(
                                             "Tap to add cover image", 
                                             style = MaterialTheme.typography.labelLarge,
                                             color = MaterialTheme.colorScheme.primary
                                         )
                                     }
                                 }
                             }
                        }
                        
                        Spacer(Modifier.height(24.dp))

                        FlockrTextField(
                            value = houseName,
                            onValueChange = { houseName = it; nameError = null },
                            label = "Household Name",
                            placeholder = "e.g. The Smith House",
                            isError = nameError != null,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (nameError != null) Text(nameError!!, color = MaterialTheme.colorScheme.error)
                    }
                    1 -> { // Address
                        Text("Where is it located?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("(Optional) helps with location based features.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(24.dp))
                        FlockrTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = "Address",
                            placeholder = "123 Main St",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    2 -> { // Localization
                        Text("Regional Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(24.dp))
                        
                        // Currency
                        ExposedDropdownMenuBox(
                            expanded = currencyExpanded, 
                            onExpandedChange = { currencyExpanded = !currencyExpanded }
                        ) {
                            OutlinedTextField(
                                value = "$currency (${currencies.find { it.first == currency }?.second ?: ""})",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Currency") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                shape = MaterialTheme.shapes.medium
                            )
                            ExposedDropdownMenu(
                                expanded = currencyExpanded, 
                                onDismissRequest = { currencyExpanded = false }
                            ) {
                                currencies.forEach { (code, symbol) ->
                                    DropdownMenuItem(
                                        text = { Text("$symbol $code") }, 
                                        onClick = { currency = code; currencyExpanded = false }
                                    )
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        // Date Format
                        ExposedDropdownMenuBox(
                            expanded = dateFormatExpanded, 
                            onExpandedChange = { dateFormatExpanded = !dateFormatExpanded }
                        ) {
                            OutlinedTextField(
                                value = dateFormat,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Date Format") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dateFormatExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                shape = MaterialTheme.shapes.medium
                            )
                            ExposedDropdownMenu(
                                expanded = dateFormatExpanded, 
                                onDismissRequest = { dateFormatExpanded = false }
                            ) {
                                dateFormats.forEach { format ->
                                    DropdownMenuItem(
                                        text = { Text(format) }, 
                                        onClick = { dateFormat = format; dateFormatExpanded = false }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        
                        // Timezone
                        val timezones = listOf(
                            "UTC",
                            "America/New_York", "America/Chicago", "America/Denver", "America/Los_Angeles",
                            "Europe/London", "Europe/Paris", "Europe/Berlin",
                            "Asia/Tokyo", "Asia/Shanghai", "Asia/Kolkata", "Asia/Singapore",
                            "Australia/Sydney"
                        )
                        
                        ExposedDropdownMenuBox(
                            expanded = timezoneExpanded, 
                            onExpandedChange = { timezoneExpanded = !timezoneExpanded }
                        ) {
                            OutlinedTextField(
                                value = timezone,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Timezone") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timezoneExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                shape = MaterialTheme.shapes.medium
                            )
                            ExposedDropdownMenu(
                                expanded = timezoneExpanded, 
                                onDismissRequest = { timezoneExpanded = false }
                            ) {
                                timezones.forEach { tz ->
                                    DropdownMenuItem(
                                        text = { Text(tz) }, 
                                        onClick = { timezone = tz; timezoneExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    3 -> { // Review
                         Text("Review Details", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                         Spacer(Modifier.height(24.dp))
                         
                         ElevatedCard(
                             modifier = Modifier.fillMaxWidth(),
                             colors = CardDefaults.elevatedCardColors(),
                             elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                             shape = MaterialTheme.shapes.large
                         ) {
                             Column {
                                 // Header Image PREVIEW
                                 Box(
                                     modifier = Modifier
                                         .fillMaxWidth()
                                         .height(140.dp)
                                         .background(MaterialTheme.colorScheme.surfaceVariant)
                                 ) {
                                     if (selectedImageUri != null) {
                                         AsyncImage(
                                             model = selectedImageUri,
                                             contentDescription = null,
                                             modifier = Modifier.fillMaxSize(),
                                             contentScale = ContentScale.Crop
                                         )
                                         Box(
                                            modifier = Modifier.fillMaxSize().background(
                                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                                )
                                            ),
                                            contentAlignment = Alignment.BottomStart
                                         ) {
                                             Text(
                                                 text = houseName,
                                                 style = MaterialTheme.typography.headlineSmall,
                                                 color = Color.White,
                                                 fontWeight = FontWeight.Bold,
                                                 modifier = Modifier.padding(16.dp)
                                             )
                                         }
                                     } else {
                                         Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                 Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                 Text("No Header Image", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                             }
                                         }
                                     }
                                 }
                                 
                                 Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                     if (selectedImageUri == null) {
                                         Text("Name: $houseName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                     }
                                     
                                     Row(verticalAlignment = Alignment.CenterVertically) {
                                         Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                         Spacer(Modifier.width(12.dp))
                                         Text(address.ifBlank { "No address set" }, style = MaterialTheme.typography.bodyMedium)
                                     }
                                     
                                     HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                     
                                     Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                         Column {
                                             Text("Currency", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                             Text(currency, style = MaterialTheme.typography.bodyLarge)
                                         }
                                         Column {
                                             Text("DateFormat", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                             Text(dateFormat, style = MaterialTheme.typography.bodyLarge)
                                         }
                                     }
                                     
                                     Column {
                                         Text("Timezone", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                         Text(timezone, style = MaterialTheme.typography.bodyLarge)
                                     }
                                 }
                             }
                         }
                         Spacer(Modifier.height(24.dp))
                         // Aesthetic Info Board
                         Card(
                             colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                             shape = MaterialTheme.shapes.medium
                         ) {
                             Row(
                                 Modifier.padding(16.dp).fillMaxWidth(), 
                                 verticalAlignment = Alignment.CenterVertically
                             ) {
                                 Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.secondary)
                                 Spacer(Modifier.width(16.dp))
                                 Text(
                                     "You can invite members immediately after creation.",
                                     style = MaterialTheme.typography.bodySmall,
                                     color = MaterialTheme.colorScheme.onSecondaryContainer
                                 )
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
