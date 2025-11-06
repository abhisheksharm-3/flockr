package `in`.xroden.flockr.ui.screens.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.data.preferences.ThemeMode
import `in`.xroden.flockr.ui.components.ModernButton
import `in`.xroden.flockr.ui.components.ModernDivider
import `in`.xroden.flockr.ui.components.ModernSectionHeader
import `in`.xroden.flockr.ui.viewmodel.AuthViewModel
import `in`.xroden.flockr.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    profileViewModel: `in`.xroden.flockr.ui.viewmodel.ProfileViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val currentTheme by viewModel.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val profile by profileViewModel.profile.collectAsState()
    val isProfileLoading by profileViewModel.isLoading.collectAsState()
    val profileError by profileViewModel.error.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var editMode by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        profileViewModel.loadProfile()
    }

    LaunchedEffect(profile) {
        profile?.let {
            editedName = it.fullName ?: ""
        }
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets.systemBars,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Settings",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Profile Section
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Profile Picture Placeholder
                        Surface(
                            modifier = Modifier
                                .size(80.dp)
                                .align(Alignment.CenterHorizontally),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        if (editMode) {
                            // Edit Mode
                            OutlinedTextField(
                                value = editedName,
                                onValueChange = { editedName = it },
                                label = { Text("Full Name") },
                                placeholder = { Text("Enter your name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            if (profileError != null) {
                                Text(
                                    text = profileError ?: "",
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
                                        profileViewModel.clearError()
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isProfileLoading,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Cancel")
                                }

                                ModernButton(
                                    onClick = {
                                        profileViewModel.updateProfile(editedName)
                                        editMode = false
                                    },
                                    text = if (isProfileLoading) "Saving..." else "Save",
                                    modifier = Modifier.weight(1f),
                                    enabled = !isProfileLoading && editedName.isNotBlank()
                                )
                            }
                        } else {
                            // View Mode
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = profile?.fullName ?: "Loading...",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = profile?.email ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            ModernButton(
                                onClick = { editMode = true },
                                text = "Edit Profile",
                                icon = Icons.Default.Edit,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Appearance Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )

                `in`.xroden.flockr.ui.components.lists.ModernListItem(
                    title = "Theme",
                    subtitle = when (currentTheme) {
                        ThemeMode.LIGHT -> "Light Mode"
                        ThemeMode.DARK -> "Dark Mode"
                        ThemeMode.SYSTEM -> "System Default"
                    },
                    icon = if (currentTheme == ThemeMode.DARK) Icons.Default.DarkMode else Icons.Default.LightMode,
                    onClick = { showThemeDialog = true },
                    showChevron = true
                )
            }

            // About Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )

                // App Info
                `in`.xroden.flockr.ui.components.lists.ModernListItem(
                    title = "Flockr",
                    subtitle = "Version 1.0.0",
                    icon = Icons.Default.Settings,
                    showChevron = false
                )

                // Developer
                `in`.xroden.flockr.ui.components.lists.ModernListItem(
                    title = "Developer",
                    subtitle = "Abhishek Sharma",
                    icon = Icons.Default.Person,
                    showChevron = false
                )

                // Website Link
                val context = androidx.compose.ui.platform.LocalContext.current
                `in`.xroden.flockr.ui.components.lists.ModernListItem(
                    title = "Website",
                    subtitle = "abhisheksan.com",
                    icon = Icons.Default.Person,
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            data = android.net.Uri.parse("https://abhisheksan.com")
                        }
                        context.startActivity(intent)
                    },
                    showChevron = true
                )

                // GitHub Link
                `in`.xroden.flockr.ui.components.lists.ModernListItem(
                    title = "GitHub Repository",
                    subtitle = "View source code & contribute",
                    icon = Icons.Default.Settings,
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            data = android.net.Uri.parse("https://github.com/abhisheksharm-3/flockr")
                        }
                        context.startActivity(intent)
                    },
                    showChevron = true
                )
            }

            // Account Actions
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Account",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )

                `in`.xroden.flockr.ui.components.lists.ModernListItem(
                    title = "Sign Out",
                    subtitle = "Sign out of your account",
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    iconBackgroundColor = MaterialTheme.colorScheme.errorContainer,
                    onClick = { showLogoutDialog = true },
                    showChevron = false
                )
            }
        }
    }

    // Theme Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = {
                Text(
                    "Choose Theme",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        val selected = currentTheme == mode
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        
                        val scale by animateFloatAsState(
                            targetValue = if (isPressed) 0.97f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "theme_option_scale"
                        )
                        
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(scale)
                                .border(
                                    width = 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null
                                ) {
                                    scope.launch {
                                        viewModel.setThemeMode(mode)
                                        showThemeDialog = false
                                    }
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                   else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = null
                                )
                                Text(
                                    text = when (mode) {
                                        ThemeMode.LIGHT -> "Light"
                                        ThemeMode.DARK -> "Dark"
                                        ThemeMode.SYSTEM -> "System Default"
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Done", fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
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
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Sign Out", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun IndustrialSettingsCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isDanger: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "settings_card_scale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .border(
                width = 2.dp,
                color = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDanger) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .border(
                        width = 2.dp,
                        color = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(6.dp)
                    ),
                shape = RoundedCornerShape(6.dp),
                color = if (isDanger) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
