package `in`.xroden.flockr.features.settings.ui

import androidx.compose.foundation.BorderStroke
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

import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.xroden.flockr.BuildConfig
import `in`.xroden.flockr.features.settings.model.ThemeMode
import `in`.xroden.flockr.features.auth.model.Profile
import `in`.xroden.flockr.features.auth.presentation.AuthViewModel
import `in`.xroden.flockr.features.settings.presentation.SettingsViewModel
import `in`.xroden.flockr.features.settings.presentation.ProfileViewModel
import `in`.xroden.flockr.features.settings.presentation.ProfileUiState
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.utils.Haptics
import `in`.xroden.flockr.utils.rememberHaptics
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val currentTheme by viewModel.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState(initial = true)
    val profileUiState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val profile = (profileUiState as? ProfileUiState.Success)?.profile

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
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
            ProfileHeaderCard(
                profile = profile,
                haptics = haptics,
                onEditProfile = onNavigateToProfile
            )

            SettingsSectionsList(
                currentTheme = currentTheme,
                hapticsEnabled = hapticsEnabled,
                haptics = haptics,
                onThemeClick = { showThemeDialog = true },
                onHapticsToggle = { viewModel.setHapticsEnabled(it) },
                onNavigateToNotificationPreferences = onNavigateToNotificationPreferences,
                onNavigateToSecurity = onNavigateToSecurity,
                onSignOutClick = { showLogoutDialog = true }
            )
        }
    }

    if (showThemeDialog) {
        ThemeDialog(
            currentTheme = currentTheme,
            onThemeSelected = { mode ->
                scope.launch {
                    viewModel.setThemeMode(mode)
                    showThemeDialog = false
                }
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showLogoutDialog) {
        LogoutDialog(
            onConfirm = {
                scope.launch {
                    showLogoutDialog = false
                    authViewModel.signOut()
                    onLogout()
                }
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}

@Composable
private fun ProfileHeaderCard(
    profile: Profile?,
    haptics: Haptics,
    onEditProfile: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val avatarUrl = profile?.avatarUrl
                    if (avatarUrl != null) {
                        androidx.compose.foundation.Image(
                            painter = coil.compose.rememberAsyncImagePainter(avatarUrl),
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = (profile?.fullName?.firstOrNull()?.uppercase() ?: "U"),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = profile?.fullName ?: "Loading...",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(profile?.email ?: "", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { haptics.tap(); onEditProfile() }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                    Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Edit Profile", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionsList(
    currentTheme: ThemeMode,
    hapticsEnabled: Boolean,
    haptics: Haptics,
    onThemeClick: () -> Unit,
    onHapticsToggle: (Boolean) -> Unit,
    onNavigateToNotificationPreferences: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onSignOutClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AppearanceSettingsSection(
            currentTheme = currentTheme,
            hapticsEnabled = hapticsEnabled,
            haptics = haptics,
            onThemeClick = onThemeClick,
            onHapticsToggle = onHapticsToggle
        )

        NotificationsSettingsSection(
            haptics = haptics,
            onNavigateToNotificationPreferences = onNavigateToNotificationPreferences
        )

        SecuritySettingsSection(
            haptics = haptics,
            onNavigateToSecurity = onNavigateToSecurity
        )

        AboutSettingsSection()

        AccountSettingsSection(
            haptics = haptics,
            onSignOutClick = onSignOutClick
        )
    }
}

@Composable
private fun AppearanceSettingsSection(
    currentTheme: ThemeMode,
    hapticsEnabled: Boolean,
    haptics: Haptics,
    onThemeClick: () -> Unit,
    onHapticsToggle: (Boolean) -> Unit
) {
    SettingsSection(title = "Appearance") {
        SettingsItem(
            icon = if (currentTheme == ThemeMode.DARK) Icons.Default.DarkMode else Icons.Default.LightMode,
            title = "Theme",
            subtitle = when (currentTheme) {
                ThemeMode.LIGHT -> "Light Mode"
                ThemeMode.DARK -> "Dark Mode"
                ThemeMode.SYSTEM -> "System Default"
            },
            onClick = onThemeClick
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        SettingsToggleItem(
            icon = Icons.Default.Vibration,
            title = "Haptic Feedback",
            subtitle = "Vibration feedback for interactions",
            checked = hapticsEnabled,
            onCheckedChange = onHapticsToggle
        )
    }
}

@Composable
private fun NotificationsSettingsSection(
    haptics: Haptics,
    onNavigateToNotificationPreferences: () -> Unit
) {
    SettingsSection(title = "Notifications") {
        SettingsItem(
            icon = Icons.Default.Notifications,
            title = "Notification Preferences",
            subtitle = "Manage notification settings for each household",
            onClick = onNavigateToNotificationPreferences
        )
    }
}

@Composable
private fun SecuritySettingsSection(
    haptics: Haptics,
    onNavigateToSecurity: () -> Unit
) {
    SettingsSection(title = "Security") {
        SettingsItem(
            icon = Icons.Default.Lock,
            title = "App Lock",
            subtitle = "Secure your app with biometrics",
            onClick = onNavigateToSecurity
        )
    }
}

@Composable
private fun AboutSettingsSection() {
    SettingsSection(title = "About") {
        SettingsItem(
            icon = Icons.Default.Info,
            title = "Flockr",
            subtitle = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            showChevron = false
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        SettingsItem(Icons.Default.Code, "Developer", "Abhishek Sharma", showChevron = false)
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        val context = LocalContext.current
        SettingsItem(Icons.Default.Language, "Website", "abhisheksan.com", onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://abhisheksan.com")))
        })
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        SettingsItem(Icons.Default.Star, "GitHub Repository", "View source code & contribute", onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/abhisheksharm-3/flockr")))
        })
    }
}

@Composable
private fun AccountSettingsSection(
    haptics: Haptics,
    onSignOutClick: () -> Unit
) {
    SettingsSection(title = "Account") {
        SettingsItem(
            icon = Icons.AutoMirrored.Filled.ExitToApp,
            title = "Sign Out",
            subtitle = "Sign out of your account",
            onClick = { haptics.error(); onSignOutClick() },
            showChevron = false,
            iconTint = MaterialTheme.colorScheme.error
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeDialog(
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Choose Theme", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Clear, "Close") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Appearance", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Choose how Flockr looks to you. Select a single theme or sync with your system settings.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))

            ThemeMode.entries.forEach { mode ->
                val selected = currentTheme == mode
                Card(
                    onClick = { onThemeSelected(mode) },
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
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
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = when (mode) {
                                    ThemeMode.LIGHT -> "Light"
                                    ThemeMode.DARK -> "Dark"
                                    ThemeMode.SYSTEM -> "System Default"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
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
                        if (selected) Icon(Icons.Default.Done, "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LogoutDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val haptics = rememberHaptics()
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.error) },
        title = { Text("Sign Out?", fontWeight = FontWeight.SemiBold) },
        text = { Text("Are you sure you want to sign out of your account?") },
        confirmButton = {
            Button(onClick = { haptics.error(); onConfirm() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Sign Out", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(Modifier.fillMaxWidth()) { content() }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = onClick != null,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        onClick = { onClick?.invoke() },
        modifier = Modifier.fillMaxWidth(),
        enabled = onClick != null,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.medium,
                color = iconTint.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(22.dp), tint = iconTint)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = if (iconTint == MaterialTheme.colorScheme.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (showChevron) Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    val haptics = rememberHaptics()
    Surface(
        onClick = { haptics.toggle(!checked); onCheckedChange(!checked) },
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.medium,
                color = iconTint.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(22.dp), tint = iconTint)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = { haptics.toggle(it); onCheckedChange(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

