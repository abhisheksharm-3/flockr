package `in`.xroden.flockr

import `in`.xroden.flockr.features.notifications.system.PushTokens
import `in`.xroden.flockr.features.notifications.system.EXTRA_NOTIFICATION_ID
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import `in`.xroden.flockr.features.settings.model.ThemeMode
import `in`.xroden.flockr.ui.navigation.FlockrNavigation
import `in`.xroden.flockr.ui.theme.FlockrTheme
import `in`.xroden.flockr.features.settings.presentation.SettingsViewModel
import androidx.core.content.ContextCompat
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.fragment.app.FragmentActivity
import `in`.xroden.flockr.core.managers.AppLockManager
import `in`.xroden.flockr.core.validation.Validators
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import `in`.xroden.flockr.ui.components.LockScreenOverlay
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import android.content.Intent
import androidx.compose.animation.fadeOut
import androidx.compose.ui.Modifier
import javax.inject.Inject
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.CompositionLocalProvider
import `in`.xroden.flockr.utils.LocalHapticsEnabled

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    
    @Inject
    lateinit var appLockManager: AppLockManager

    @Inject
    lateinit var pushTokens: PushTokens

    private val settingsViewModel: SettingsViewModel by viewModels()
    private val askForNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    /** The latest intent, observable by Compose, so a link opened while the app is running is still read. */
    private val intentState = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (savedInstanceState == null) {
            appLockManager.initializeColdStartLock()
        }

        intentState.value = intent

        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val isAppLocked by appLockManager.isAppLocked.collectAsStateWithLifecycle()
            val hapticsEnabled = settingsViewModel.hapticsEnabled.collectAsState(initial = true)

            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            CompositionLocalProvider(LocalHapticsEnabled provides hapticsEnabled) {
                FlockrTheme(darkTheme = darkTheme) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val (inviteCode, setInviteCode) = remember { mutableStateOf<String?>(null) }
                        val (notificationId, setNotificationId) = remember { mutableStateOf<String?>(null) }

                        LaunchedEffect(intentState.value) {
                            intentState.value?.inviteCode()?.let { setInviteCode(it) }
                            intentState.value?.getStringExtra(EXTRA_NOTIFICATION_ID)?.let { setNotificationId(it) }
                        }

                        FlockrNavigation(
                            initialInviteCode = inviteCode,
                            onInviteConsumed = { setInviteCode(null) },
                            pendingNotificationId = notificationId,
                            onNotificationConsumed = { setNotificationId(null) },
                            onSignedIn = ::onSignedIn
                        )

                        AnimatedVisibility(
                            visible = isAppLocked,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            LockScreenOverlay(
                                onUnlockClick = { appLockManager.authenticate(this@MainActivity) }
                            )
                        }
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentState.value = intent
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            appLockManager.onAppBackgrounded()
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            appLockManager.onAppForegrounded {
                appLockManager.authenticate(this@MainActivity)
            }
        }
    }

    /** Runs once the user is signed in: links this phone for pushes and asks to show notifications. */
    private fun onSignedIn() {
        lifecycleScope.launch { pushTokens.register() }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            askForNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

/** The invite code in a `flockr://invite?code=…` or `flockr://invite/…` link, when it is a valid one. */
private fun Intent.inviteCode(): String? {
    val link = data?.takeIf { it.scheme == "flockr" && it.host == "invite" } ?: return null
    val code = link.getQueryParameter("code") ?: link.pathSegments.firstOrNull() ?: return null
    return Validators.validateInviteCode(code).getOrNull()
}
