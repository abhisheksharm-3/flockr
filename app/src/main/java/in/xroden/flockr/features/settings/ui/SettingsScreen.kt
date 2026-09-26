/** The app's settings: your profile, how Flockr looks and feels, security, notifications and signing out. */
package `in`.xroden.flockr.features.settings.ui

import `in`.xroden.flockr.features.auth.presentation.AccountDeletionState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.BuildConfig
import `in`.xroden.flockr.features.settings.model.ThemeMode
import `in`.xroden.flockr.features.settings.presentation.ProfileUiState
import `in`.xroden.flockr.features.settings.presentation.ProfileViewModel
import `in`.xroden.flockr.features.settings.presentation.SettingsViewModel
import `in`.xroden.flockr.ui.components.AnimatedGlyph
import `in`.xroden.flockr.ui.components.BadgeTone
import `in`.xroden.flockr.ui.components.GlyphMotion
import `in`.xroden.flockr.ui.components.HeroActions
import `in`.xroden.flockr.ui.components.HeroCaption
import `in`.xroden.flockr.ui.components.HeroHeader
import `in`.xroden.flockr.ui.components.HeroSecondaryButton
import `in`.xroden.flockr.ui.components.HeroStatusBarScrim
import `in`.xroden.flockr.ui.components.IconBadge
import `in`.xroden.flockr.ui.components.ListRow
import `in`.xroden.flockr.ui.components.MemberAvatar
import `in`.xroden.flockr.ui.components.SectionTitle
import `in`.xroden.flockr.ui.components.dialogs.ConfirmDialog
import `in`.xroden.flockr.ui.components.inputs.PillSelector
import `in`.xroden.flockr.ui.components.isHeroScrolledAway
import `in`.xroden.flockr.ui.theme.ComponentHeight
import `in`.xroden.flockr.ui.theme.IconSize
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics

private const val SOURCE_CODE_URL = "https://github.com/abhisheksharm-3/flockr"

private val ThemeOptions = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)

private fun ThemeMode.label() = when (this) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

private fun ThemeMode.meaning() = when (this) {
    ThemeMode.SYSTEM -> "Follows your phone"
    ThemeMode.LIGHT -> "Always light"
    ThemeMode.DARK -> "Always dark"
}

private fun ThemeMode.icon() = when (this) {
    ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
    ThemeMode.LIGHT -> Icons.Rounded.LightMode
    ThemeMode.DARK -> Icons.Rounded.DarkMode
}

/** You lead, large, on the cobalt; below it the rest is grouped by what you'd come to change, with signing out last and apart. */
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToNotificationPreferences: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onSignOut: () -> Unit,
    accountDeletion: AccountDeletionState,
    onDeleteAccount: () -> Unit,
    onAccountDeletionErrorShown: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val haptics = rememberHaptics()
    val uriHandler = LocalUriHandler.current
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle()
    val lockEnabled by viewModel.appLockEnabled.collectAsStateWithLifecycle()
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
    var isConfirmingSignOut by remember { mutableStateOf(false) }
    var isConfirmingDelete by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val isDeleting = accountDeletion == AccountDeletionState.Deleting

    LaunchedEffect(accountDeletion) {
        val failure = accountDeletion as? AccountDeletionState.Failed ?: return@LaunchedEffect
        haptics.error()
        snackbarHostState.showSnackbar(failure.message)
        onAccountDeletionErrorShown()
    }

    LifecycleResumeEffect(Unit) {
        profileViewModel.loadProfile()
        onPauseOrDispose {}
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + Spacing.xxl),
            ) {
                item(key = "hero") { ProfileHero(state = profileState, onEditProfile = onNavigateToProfile) }

                item(key = "appearance") { SectionTitle("Appearance") }
                item(key = "theme") {
                    Column {
                        ListRow(
                            headline = "Theme",
                            supporting = themeMode.meaning(),
                            leading = { AnimatedBadge(themeMode.icon(), trigger = themeMode) },
                        )
                        PillSelector(
                            tabs = ThemeOptions.map { it.label() },
                            selectedIndex = ThemeOptions.indexOf(themeMode),
                            onTabSelected = { viewModel.setThemeMode(ThemeOptions[it]) },
                            modifier = Modifier.padding(horizontal = Spacing.lg).padding(bottom = Spacing.sm),
                        )
                    }
                }
                item(key = "haptics") {
                    SwitchListRow(
                        icon = Icons.Rounded.Vibration,
                        title = "Haptic feedback",
                        subtitle = "A small vibration when you tap, toggle or finish something",
                        checked = hapticsEnabled,
                        onCheckedChange = viewModel::setHapticsEnabled,
                    )
                }

                item(key = "privacy") { SectionTitle("Privacy and alerts") }
                item(key = "security") {
                    LinkRow(
                        icon = Icons.Rounded.Lock,
                        title = "Security",
                        subtitle = if (lockEnabled) "App lock is on" else "Lock the app with your fingerprint, face or screen lock",
                        onClick = onNavigateToSecurity,
                    )
                }
                item(key = "notifications") {
                    LinkRow(
                        icon = Icons.Rounded.Notifications,
                        title = "Notifications",
                        subtitle = "Choose what each house tells you about",
                        onClick = onNavigateToNotificationPreferences,
                    )
                }

                item(key = "about") { SectionTitle("About") }
                item(key = "version") {
                    ListRow(
                        headline = "Version",
                        supporting = BuildConfig.VERSION_NAME,
                        leading = { IconBadge(Icons.Rounded.Info, BadgeTone.SLATE) },
                    )
                }
                item(key = "source") {
                    ListRow(
                        headline = "Source code",
                        supporting = "See how Flockr is built on GitHub",
                        leading = { IconBadge(Icons.Rounded.Code, BadgeTone.SLATE) },
                        trailing = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(IconSize.sm)) },
                        onClick = { uriHandler.openUri(SOURCE_CODE_URL) },
                    )
                }

                item(key = "sign_out") {
                    ListRow(
                        headline = "Sign out",
                        supporting = "You can sign back in any time",
                        headlineColor = MaterialTheme.colorScheme.error,
                        leading = { IconBadge(Icons.AutoMirrored.Rounded.Logout, BadgeTone.ROSE) },
                        onClick = { haptics.tap(); isConfirmingSignOut = true },
                        modifier = Modifier.padding(top = Spacing.xxl),
                    )
                }
                item(key = "delete_account") {
                    ListRow(
                        headline = if (isDeleting) "Deleting your account…" else "Delete account",
                        supporting = "Removes your profile, photo and personal files for good",
                        headlineColor = MaterialTheme.colorScheme.error,
                        leading = { IconBadge(Icons.Rounded.DeleteForever, BadgeTone.ROSE) },
                        onClick = if (isDeleting) null else ({ haptics.tap(); isConfirmingDelete = true }),
                    )
                }
            }
            HeroStatusBarScrim(isHeroGone = listState.isHeroScrolledAway)
        }
    }

    if (isConfirmingSignOut) {
        ConfirmDialog(
            title = "Sign out?",
            message = "You'll need to sign in again to see your houses.",
            confirmText = "Sign out",
            onConfirm = {
                isConfirmingSignOut = false
                onSignOut()
            },
            onDismiss = { isConfirmingSignOut = false },
            isDestructive = true,
        )
    }
    if (isConfirmingDelete) {
        ConfirmDialog(
            title = "Delete your account?",
            message = "Your profile, photo and personal files are erased and you're signed out. Houses you're in keep what was spent, " +
                "shown as \"Deleted account\", so their balances still add up. A house only you are in is deleted too. This can't be undone.",
            confirmText = "Delete account",
            onConfirm = {
                isConfirmingDelete = false
                onDeleteAccount()
            },
            onDismiss = { isConfirmingDelete = false },
            isDestructive = true,
        )
    }
}

/** Who is signed in, large on the cobalt. Tapping it, or its button, opens the profile editor. */
@Composable
private fun ProfileHero(state: ProfileUiState, onEditProfile: () -> Unit) {
    val profile = (state as? ProfileUiState.Success)?.profile
    HeroHeader(title = "Settings") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.sm)
                .clickable(onClickLabel = "Edit profile", role = Role.Button, onClick = onEditProfile),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            MemberAvatar(name = profile?.fullName.orEmpty(), avatarUrl = profile?.avatarUrl, size = IconSize.xxl)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(
                    profile?.fullName?.takeIf { it.isNotBlank() } ?: "Your profile",
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                HeroCaption(
                    when (state) {
                        is ProfileUiState.Success -> state.profile.email
                        is ProfileUiState.Error -> state.message
                        ProfileUiState.Loading -> "Loading your profile"
                    },
                )
            }
        }
        HeroActions { HeroSecondaryButton("Edit profile", onClick = onEditProfile) }
    }
}

/** A circle badge whose glyph turns once when [trigger] changes, for a row whose icon follows its value. */
@Composable
private fun AnimatedBadge(icon: ImageVector, trigger: Any) {
    Surface(
        modifier = Modifier.size(ComponentHeight.avatar),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            AnimatedGlyph(icon, trigger = trigger, motion = GlyphMotion.SPIN, contentDescription = null, modifier = Modifier.size(IconSize.sm + Spacing.xxs))
        }
    }
}

/** A row that opens another screen. Opening is navigation, so it fires no haptic. */
@Composable
private fun LinkRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListRow(
        headline = title,
        supporting = subtitle,
        leading = { IconBadge(icon, BadgeTone.COBALT) },
        trailing = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        onClick = onClick,
    )
}

/**
 * A [ListRow] with a circle badge and a switch at its end, lined up with the settings rows around it.
 * The whole row is the touch target and toggling fires the direction-specific haptic.
 */
@Composable
internal fun SwitchListRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    val haptics = rememberHaptics()
    ListRow(
        headline = title,
        supporting = subtitle,
        leading = { IconBadge(icon, BadgeTone.COBALT) },
        trailing = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
        modifier = Modifier.toggleable(value = checked, enabled = enabled, role = Role.Switch) { haptics.toggle(it); onCheckedChange(it) },
    )
}
