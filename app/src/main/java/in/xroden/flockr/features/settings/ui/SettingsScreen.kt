package `in`.xroden.flockr.features.settings.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.features.settings.model.ThemeMode
import `in`.xroden.flockr.features.auth.domain.AuthViewModel
import `in`.xroden.flockr.features.settings.domain.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit,

    onNavigateToNotificationPreferences: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    profileViewModel: `in`.xroden.flockr.features.settings.domain.ProfileViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val currentTheme by viewModel.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val profileUiState by profileViewModel.uiState.collectAsState()
    val updateState by profileViewModel.updateState.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var editMode by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }

    // Extract profile from UI state
    val profile = when (val state = profileUiState) {
        is `in`.xroden.flockr.features.settings.domain.ProfileUiState.Success -> state.profile
        else -> null
    }

    val profileError = when (val state = profileUiState) {
        is `in`.xroden.flockr.features.settings.domain.ProfileUiState.Error -> state.message
        else -> null
    }

    LaunchedEffect(profile) {
        profile?.let {
            editedName = it.fullName ?: ""
        }
    }

    // Handle update state
    LaunchedEffect(updateState) {
        when (updateState) {
            is `in`.xroden.flockr.features.settings.domain.UpdateProfileUiState.Success -> {
                editMode = false
            }
            else -> {}
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Settings",
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
                    // Profile Avatar
                    Surface(
                        modifier = Modifier.size(100.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = (profile?.fullName?.firstOrNull()?.uppercase() ?: "U"),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    if (editMode) {
                        // Edit Mode
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = editedName,
                                onValueChange = { editedName = it },
                                label = { Text("Full Name") },
                                placeholder = { Text("Enter your name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                            )

                            if (profileError != null || updateState is `in`.xroden.flockr.features.settings.domain.UpdateProfileUiState.Error) {
                                Text(
                                    text = profileError ?: (updateState as? `in`.xroden.flockr.features.settings.domain.UpdateProfileUiState.Error)?.message ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        editMode = false
                                        editedName = profile?.fullName ?: ""
                                        profileViewModel.resetUpdateState()
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = !(updateState is `in`.xroden.flockr.features.settings.domain.UpdateProfileUiState.Loading),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    onClick = {
                                        profileViewModel.updateProfile(editedName)
                                        editMode = false
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = !(updateState is `in`.xroden.flockr.features.settings.domain.UpdateProfileUiState.Loading) && editedName.isNotBlank(),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    if (updateState is `in`.xroden.flockr.features.settings.domain.UpdateProfileUiState.Loading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Text("Save", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    } else {
                        // View Mode
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = profile?.fullName ?: "Loading...",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = profile?.email ?: "",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { editMode = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Edit Profile", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Settings Sections
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Appearance Section
                SettingsSection(title = "Appearance") {
                    SettingsItem(
                        icon = if (currentTheme == ThemeMode.DARK) Icons.Default.DarkMode else Icons.Default.LightMode,
                        title = "Theme",
                        subtitle = when (currentTheme) {
                            ThemeMode.LIGHT -> "Light Mode"
                            ThemeMode.DARK -> "Dark Mode"
                            ThemeMode.SYSTEM -> "System Default"
                        },
                        onClick = { showThemeDialog = true }
                    )
                }

                // Notifications Section
                SettingsSection(title = "Notifications") {
                    SettingsItem(
                        icon = Icons.Default.Notifications,
                        title = "Notification Preferences",
                        subtitle = "Manage notification settings for each household",
                        onClick = onNavigateToNotificationPreferences
                    )
                }
                
                // Security Section
                SettingsSection(title = "Security") {
                    SettingsItem(
                        icon = Icons.Default.Settings,
                        title = "App Lock",
                        subtitle = "Secure your app with biometrics",
                        onClick = onNavigateToSecurity
                    )
                }

                // About Section
                SettingsSection(title = "About") {
                    SettingsItem(
                        icon = Icons.Default.Settings,
                        title = "Flockr",
                        subtitle = "Version ${`in`.xroden.flockr.BuildConfig.VERSION_NAME} (${`in`.xroden.flockr.BuildConfig.VERSION_CODE})",
                        showChevron = false
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    SettingsItem(
                        icon = Icons.Default.Person,
                        title = "Developer",
                        subtitle = "Abhishek Sharma",
                        showChevron = false
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    val context = androidx.compose.ui.platform.LocalContext.current
                    SettingsItem(
                        icon = Icons.Default.Person,
                        title = "Website",
                        subtitle = "abhisheksan.com",
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                data = android.net.Uri.parse("https://abhisheksan.com")
                            }
                            context.startActivity(intent)
                        }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    SettingsItem(
                        icon = Icons.Default.Settings,
                        title = "GitHub Repository",
                        subtitle = "View source code & contribute",
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                data = android.net.Uri.parse("https://github.com/abhisheksharm-3/flockr")
                            }
                            context.startActivity(intent)
                        }
                    )
                }

                // Account Section
                SettingsSection(title = "Account") {
                    SettingsItem(
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        title = "Sign Out",
                        subtitle = "Sign out of your account",
                        onClick = { showLogoutDialog = true },
                        showChevron = false,
                        iconTint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // Theme Selector Full Screen
    if (showThemeDialog) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Choose Theme",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { showThemeDialog = false }) {
                            Icon(Icons.Default.Clear, "Close")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Choose how Flockr looks to you. Select a single theme or sync with your system settings.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                ThemeMode.entries.forEach { mode ->
                    val selected = currentTheme == mode

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            scope.launch {
                                viewModel.setThemeMode(mode)
                                showThemeDialog = false
                            }
                        },
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = when (mode) {
                                    ThemeMode.LIGHT -> Icons.Default.LightMode
                                    ThemeMode.DARK -> Icons.Default.DarkMode
                                    ThemeMode.SYSTEM -> Icons.Default.Settings
                                },
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = if (selected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = when (mode) {
                                        ThemeMode.LIGHT -> "Light"
                                        ThemeMode.DARK -> "Dark"
                                        ThemeMode.SYSTEM -> "System Default"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = when (mode) {
                                        ThemeMode.LIGHT -> "Always use light theme"
                                        ThemeMode.DARK -> "Always use dark theme"
                                        ThemeMode.SYSTEM -> "Follow system settings"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (selected) {
                                Icon(
                                    Icons.Default.Done,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    "Sign Out?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    "Are you sure you want to sign out of your account?",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            showLogoutDialog = false
                            authViewModel.signOut()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Sign Out", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            },
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

// Helper Composables for Settings
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = onClick != null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        onClick = { onClick?.invoke() },
        modifier = Modifier.fillMaxWidth(),
        enabled = onClick != null,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = iconTint
            )

            // Text Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (iconTint == MaterialTheme.colorScheme.error)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Chevron
            if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}
