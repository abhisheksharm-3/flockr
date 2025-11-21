package `in`.xroden.flockr.features.settings.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.features.settings.domain.ProfileViewModel
import `in`.xroden.flockr.ui.components.buttons.FlockrPrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profileUiState by viewModel.uiState.collectAsState()
    val updateState by viewModel.updateState.collectAsState()

    var editMode by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }

    // Extract profile from UI state
    val profile = when (val state = profileUiState) {
        is `in`.xroden.flockr.features.settings.domain.ProfileUiState.Success -> state.profile
        else -> null
    }

    // Update editedName when profile loads
    LaunchedEffect(profile) {
        profile?.let {
            editedName = it.fullName ?: ""
        }
    }

    // Show snackbar on success
    LaunchedEffect(updateState) {
        if (updateState is `in`.xroden.flockr.features.settings.domain.UpdateProfileUiState.Success) {
            viewModel.resetUpdateState()
            editMode = false
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Profile",
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
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = profileUiState) {
                is `in`.xroden.flockr.features.settings.domain.ProfileUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is `in`.xroden.flockr.features.settings.domain.ProfileUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        FlockrPrimaryButton(
                            onClick = { viewModel.loadProfile() },
                            text = "Retry"
                        )
                    }
                }
                is `in`.xroden.flockr.features.settings.domain.ProfileUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Profile Header Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Avatar
                                Surface(
                                    modifier = Modifier.size(80.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = profile?.fullName?.firstOrNull()?.toString() ?: "?",
                                            style = MaterialTheme.typography.headlineLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                // Name and Email
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = profile?.fullName ?: "No name",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = profile?.email ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Profile Details Section
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Personal Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(start = 4.dp)
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    if (editMode) {
                                        // Edit Mode
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = "Full Name",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            OutlinedTextField(
                                                value = editedName,
                                                onValueChange = { editedName = it },
                                                placeholder = { Text("Enter your name") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                shape = MaterialTheme.shapes.medium
                                            )
                                        }

                                        val errorMessage = when (val state = updateState) {
                                            is `in`.xroden.flockr.features.settings.domain.UpdateProfileUiState.Error -> state.message
                                            else -> null
                                        }

                                        if (errorMessage != null) {
                                            Text(
                                                text = errorMessage,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )
                                        }

                                        val isUpdating = updateState is `in`.xroden.flockr.features.settings.domain.UpdateProfileUiState.Loading

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    editMode = false
                                                    editedName = profile?.fullName ?: ""
                                                    viewModel.resetUpdateState()
                                                },
                                                modifier = Modifier.weight(1f),
                                                enabled = !isUpdating,
                                                shape = MaterialTheme.shapes.medium
                                            ) {
                                                Text("Cancel")
                                            }

                                            Button(
                                                onClick = {
                                                    viewModel.updateProfile(editedName)
                                                },
                                                modifier = Modifier.weight(1f),
                                                enabled = !isUpdating && editedName.isNotBlank(),
                                                shape = MaterialTheme.shapes.medium
                                            ) {
                                                if (isUpdating) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(16.dp),
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                        strokeWidth = 2.dp
                                                    )
                                                } else {
                                                    Text("Save Changes")
                                                }
                                            }
                                        }
                                    } else {
                                        // View Mode
                                        ProfileInfoItem(
                                            label = "Full Name",
                                            value = profile?.fullName ?: "Not set"
                                        )

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                        ProfileInfoItem(
                                            label = "Email",
                                            value = profile?.email ?: ""
                                        )

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                        ProfileInfoItem(
                                            label = "Member Since",
                                            value = profile?.createdAt?.toString()?.take(10) ?: "Unknown"
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        OutlinedButton(
                                            onClick = { editMode = true },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = MaterialTheme.shapes.medium
                                        ) {
                                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Edit Profile")
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
fun ProfileInfoItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

