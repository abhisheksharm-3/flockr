/** The app's settings: your profile, how Flockr looks and feels, security, notifications and signing out. */
package `in`.xroden.flockr.features.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.BuildConfig
import `in`.xroden.flockr.features.auth.presentation.AuthViewModel
import `in`.xroden.flockr.features.settings.model.ThemeMode
import `in`.xroden.flockr.features.settings.presentation.ProfileUiState
import `in`.xroden.flockr.features.settings.presentation.ProfileViewModel
import `in`.xroden.flockr.features.settings.presentation.SettingsViewModel
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.MemberAvatar
import `in`.xroden.flockr.ui.components.cards.SectionCard
import `in`.xroden.flockr.ui.components.dialogs.ConfirmDialog
import `in`.xroden.flockr.ui.components.forms.ToggleRow
import `in`.xroden.flockr.ui.components.inputs.PillSelector
import `in`.xroden.flockr.ui.theme.ComponentHeight
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics

private const val SOURCE_CODE_URL = "https://github.com/abhisheksharm-3/flockr"

private val ThemeOptions = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)

private fun ThemeMode.label() = when (this) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

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
    val haptics = rememberHaptics()
    val uriHandler = LocalUriHandler.current
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle()
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    var isConfirmingSignOut by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LifecycleResumeEffect(Unit) {
        profileViewModel.loadProfile()
        onPauseOrDispose {}
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { FlockrTopAppBar(title = "Settings", onNavigateBack = onNavigateBack, scrollBehavior = scrollBehavior) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            ProfileHeader(state = profileState, onClick = onNavigateToProfile)

            SectionCard(title = "Appearance") {
                Text("Theme", style = MaterialTheme.typography.titleSmallEmphasized)
                PillSelector(
                    tabs = ThemeOptions.map { it.label() },
                    selectedIndex = ThemeOptions.indexOf(themeMode),
                    onTabSelected = { viewModel.setThemeMode(ThemeOptions[it]) },
                )
                ToggleRow(
                    title = "Haptic feedback",
                    subtitle = "A small vibration when you tap, toggle or finish something",
                    checked = hapticsEnabled,
                    onCheckedChange = viewModel::setHapticsEnabled,
                )
            }

            SectionCard(title = "Privacy and alerts") {
                LinkRow(icon = Icons.Rounded.Lock, title = "Security", subtitle = "Lock the app with your fingerprint, face or screen lock", onClick = onNavigateToSecurity)
                LinkRow(icon = Icons.Rounded.Notifications, title = "Notifications", subtitle = "Choose what each house tells you about", onClick = onNavigateToNotificationPreferences)
            }

            SectionCard(title = "About") {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                    Text("Version", style = MaterialTheme.typography.titleSmallEmphasized)
                    Text(BuildConfig.VERSION_NAME, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LinkRow(icon = Icons.Rounded.Code, title = "Source code", subtitle = "See how Flockr is built on GitHub", onClick = { uriHandler.openUri(SOURCE_CODE_URL) })
            }

            OutlinedButton(
                onClick = { haptics.tap(); isConfirmingSignOut = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.padding(end = Spacing.sm))
                Text("Sign out")
            }
        }
    }

    if (isConfirmingSignOut) {
        ConfirmDialog(
            title = "Sign out?",
            message = "You'll need to sign in again to see your houses.",
            confirmText = "Sign out",
            onConfirm = {
                isConfirmingSignOut = false
                authViewModel.signOut()
                onLogout()
            },
            onDismiss = { isConfirmingSignOut = false },
            isDestructive = true,
        )
    }
}

/** Who is signed in. Tapping it opens the profile editor, so it fires no haptic of its own. */
@Composable
private fun ProfileHeader(state: ProfileUiState, onClick: () -> Unit) {
    val profile = (state as? ProfileUiState.Success)?.profile
    val name = profile?.fullName?.takeIf { it.isNotBlank() } ?: "Your profile"
    val detail = when (state) {
        is ProfileUiState.Success -> state.profile.email
        is ProfileUiState.Error -> state.message
        ProfileUiState.Loading -> "Loading…"
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.xl),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            MemberAvatar(name = profile?.fullName.orEmpty(), avatarUrl = profile?.avatarUrl, size = ComponentHeight.avatarLarge)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(name, style = MaterialTheme.typography.titleLargeEmphasized)
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Edit profile", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** A row that opens another screen or page. Opening is navigation, so it fires no haptic. */
@Composable
private fun LinkRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(title, style = MaterialTheme.typography.titleSmallEmphasized)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
