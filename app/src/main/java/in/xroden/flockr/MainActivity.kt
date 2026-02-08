package `in`.xroden.flockr

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
import `in`.xroden.flockr.utils.PermissionManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.fragment.app.FragmentActivity
import `in`.xroden.flockr.core.managers.AppLockManager
import `in`.xroden.flockr.core.managers.IntentHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import `in`.xroden.flockr.ui.components.LockScreenOverlay
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import android.content.Intent
import androidx.compose.animation.fadeOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    
    @Inject
    lateinit var appLockManager: AppLockManager

    private lateinit var permissionManager: PermissionManager
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        permissionManager = PermissionManager(this)
        
        if (savedInstanceState == null) {
            appLockManager.initializeColdStartLock()
        }
        
        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val isAppLocked by appLockManager.isAppLocked.collectAsState()

            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            LaunchedEffect(Unit) {
                requestNotificationPermissionIfNeeded()
            }

            FlockrTheme(darkTheme = darkTheme) {
                Box(modifier = Modifier.fillMaxSize()) {
                    FlockrNavigation()
                    
                    val (inviteCode, setInviteCode) = remember { mutableStateOf<String?>(null) }
                    
                    LaunchedEffect(intent) {
                        IntentHandler.extractInviteCode(intent)?.let { setInviteCode(it) }
                    }

                    if (inviteCode != null) {
                        AlertDialog(
                            onDismissRequest = { setInviteCode(null) },
                            title = { Text("Invite Code Received") },
                            text = { Text("Open the app and use code: $inviteCode to join the household.") },
                            confirmButton = {
                                TextButton(onClick = { setInviteCode(null) }) {
                                    Text("Got it")
                                }
                            }
                        )
                    }

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
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
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

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                permissionManager.requestNotificationPermission { }
            }
        }
    }
}
