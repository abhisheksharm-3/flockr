package `in`.xroden.flockr.features.settings.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import `in`.xroden.flockr.features.settings.presentation.ProfileViewModel
import `in`.xroden.flockr.features.settings.presentation.ProfileUiState
import `in`.xroden.flockr.features.settings.presentation.UpdateProfileUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    
    val currentProfile = (uiState as? ProfileUiState.Success)?.profile
    
    LaunchedEffect(currentProfile) {
        if (currentProfile != null) {
            editedName = currentProfile.fullName ?: ""
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Transparent TopBar to float over profile
            CenterAlignedTopAppBar(
                title = { },
                navigationIcon = {
                    FilledIconButton(
                        onClick = onNavigateBack,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    val containerColor = if (isEditing) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    val contentColor = if (isEditing) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

                    FilledIconButton(
                        onClick = {
                            if (isEditing) {
                                viewModel.updateProfile(editedName)
                                isEditing = false
                            } else {
                                isEditing = true
                            }
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = containerColor,
                            contentColor = contentColor
                        ),
                        enabled = updateState !is UpdateProfileUiState.Loading && editedName.isNotBlank()
                    ) {
                        if (updateState is UpdateProfileUiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = if (isEditing) "Save" else "Edit"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Gradient Background Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    )
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(60.dp))

                when (val state = uiState) {
                    is ProfileUiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is ProfileUiState.Error -> {
                        Column(
                            modifier = Modifier.padding(top = 100.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(state.message, color = MaterialTheme.colorScheme.error)
                            Button(onClick = { viewModel.loadProfile() }) { Text("Retry") }
                        }
                    }
                    is ProfileUiState.Success -> {
                        val profile = state.profile

                        // Avatar
                        Box(contentAlignment = Alignment.BottomEnd) {
                            
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                                contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
                                onResult = { uri ->
                                    if (uri != null) {
                                        viewModel.uploadProfilePicture(uri, context)
                                    }
                                }
                            )

                            Surface(
                                modifier = Modifier
                                    .size(160.dp)
                                    .border(6.dp, MaterialTheme.colorScheme.background, CircleShape),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shadowElevation = 12.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (!profile.avatarUrl.isNullOrEmpty()) {
                                        coil.compose.AsyncImage(
                                            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                                .data(profile.avatarUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Profile Picture",
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(80.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    
                                    // Loading overlay
                                    if (updateState is UpdateProfileUiState.Loading) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                }
                            }
                            
                            // Edit Badge (If Editing)
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isEditing,
                                enter = fadeIn() + slideInVertically(initialOffsetY = { 20 }),
                                exit = fadeOut() + slideOutVertically(targetOffsetY = { 20 })
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(40.dp)
                                        .border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
                                        .clickable {
                                            photoPickerLauncher.launch(
                                                androidx.activity.result.PickVisualMediaRequest(
                                                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                                )
                                            )
                                        },
                                    shadowElevation = 4.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Edit, 
                                            null, 
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Name Display / Edit
                        AnimatedVisibility(visible = !isEditing) {
                            Text(
                                text = profile.fullName ?: "No Name Set",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        
                        AnimatedVisibility(visible = isEditing) {
                            OutlinedTextField(
                                value = editedName,
                                onValueChange = { editedName = it },
                                label = { Text("Full Name") },
                                textStyle = MaterialTheme.typography.headlineSmall,
                                singleLine = true,
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        SizedSpacer(32.dp)

                        // Details Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                ProfileDetailItem(
                                    icon = Icons.Default.Email,
                                    label = "Email Address",
                                    value = profile.email
                                )
                                
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                
                                ProfileDetailItem(
                                    icon = Icons.Default.Person, // Or badge icon
                                    label = "User ID",
                                    value = profile.id,
                                    isMonospace = true
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
fun ProfileDetailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isMonospace: Boolean = false
) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            shape = CircleShape, 
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = value,
                style = if (isMonospace) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = if (isMonospace) androidx.compose.ui.text.font.FontFamily.Monospace else null
            )
        }
    }
}

@Composable
fun SizedSpacer(height: androidx.compose.ui.unit.Dp) {
    Spacer(modifier = Modifier.height(height))
}
