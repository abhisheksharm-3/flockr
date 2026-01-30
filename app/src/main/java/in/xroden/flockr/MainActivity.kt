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
import `in`.xroden.flockr.features.settings.domain.SettingsViewModel
import `in`.xroden.flockr.utils.PermissionManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

import androidx.fragment.app.FragmentActivity
import `in`.xroden.flockr.utils.BiometricAuthManager
import `in`.xroden.flockr.core.validation.Validators
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable

import android.content.Intent
import androidx.compose.animation.fadeOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton


@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    
    private val _isAppLocked = MutableStateFlow(false)
    private lateinit var permissionManager: PermissionManager
    private var lastBackgroundTimestamp: Long = 0L
    private val settingsViewModel: SettingsViewModel by viewModels()

    companion object {
        private const val APP_LOCK_TIMEOUT_MS = 60000L // 1 minute
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        permissionManager = PermissionManager(this)
        
        // Trigger lock check on cold start (app was killed)
        if (savedInstanceState == null) {
            lastBackgroundTimestamp = 1L  // Force lock check on resume
        }
        
        setContent {
            // Observe theme from the injected viewModel
            val themeMode by settingsViewModel.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            // Observe lock state
            val isAppLocked by _isAppLocked.collectAsState()

            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            // Request notification permission on first launch
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                    if (!hasPermission) {
                        permissionManager.requestNotificationPermission { _ ->
                            // Permission result handled
                        }
                    }
                }
            }


            FlockrTheme(darkTheme = darkTheme) {
                Box(modifier = Modifier.fillMaxSize()) {
                    FlockrNavigation()
                    
                    // Global Dialogs
                    val (inviteCode, setInviteCode) = remember { mutableStateOf<String?>(null) }
                    
                    // Handle Intent
                    LaunchedEffect(intent) {
                        handleIntent(intent) { code ->
                             setInviteCode(code)
                        }
                    }

                    if (inviteCode != null) {
                        // Show alert for deep link - user can navigate to join screen manually
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

                    // App Lock Overlay
                    AnimatedVisibility(
                        visible = isAppLocked,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        LockScreenOverlay(
                            onUnlockClick = { authenticate() }
                        )
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update intent for Recomposition/LaunchedEffect
    }

    private fun handleIntent(intent: Intent?, onInviteFound: (String) -> Unit) {
        intent?.let {
            // Helper to validate and pass invite code
            fun processInviteCode(code: String) {
                Validators.validateInviteCode(code).onSuccess { validCode ->
                    onInviteFound(validCode)
                }
            }

            // 1. Check Data (Deep Link)
            // Format: flockr://invite/{code} or https://flockr.com/invite/{code}
            val data = it.data
            if (data != null && (data.scheme == "flockr" || data.host == "flockr.app")) {
                val pathSegments = data.pathSegments
                // Path examples: /invite/ABC123
                if (pathSegments.size >= 2 && pathSegments[0] == "invite") {
                    processInviteCode(pathSegments[1])
                    return
                }
            }

            // 2. Check Extras (Notification)
            val type = it.getStringExtra("notification_type") ?: it.getStringExtra("type")
            
            if (type != null) {
                if (type == "house_invitation" || type == "HOUSE_INVITE") {
                    val code = it.getStringExtra("invite_code") ?: it.getStringExtra("code")
                    if (code != null) {
                        processInviteCode(code)
                    }
                } else if (type.startsWith("house_invitation:")) {
                    val code = type.substringAfter("house_invitation:")
                    if (code.isNotEmpty()) {
                        processInviteCode(code)
                    }
                }
            }
        }
    }


    override fun onStop() {
        super.onStop()
        // Don't treat configuration changes (rotation) as backgrounding
        if (!isChangingConfigurations) {
            lastBackgroundTimestamp = System.currentTimeMillis()
        }
    }

    override fun onResume() {
        super.onResume()
        checkAppLock()
    }

    private fun checkAppLock() {
        // If 0, it means we just authenticated or disabled lock temporarily
        if (lastBackgroundTimestamp == 0L) return
        
        val diff = System.currentTimeMillis() - lastBackgroundTimestamp
        
        // Logical check: If cold start (timestamp=1), diff is huge -> Lock
        // If backgrounded recently (< 1 min), diff is small -> No Lock
        // If backgrounded long ago (> 1 min), diff is large -> Lock
        if (diff < APP_LOCK_TIMEOUT_MS) return

        lifecycleScope.launch {
            val enabled = settingsViewModel.appLockEnabled.firstOrNull() ?: false
            if (enabled) {
                _isAppLocked.value = true
                authenticate()
            } else {
                // Feature disabled, just reset timestamp
                lastBackgroundTimestamp = 0
            }
        }
    }

    private fun authenticate() {
        BiometricAuthManager(this@MainActivity).authenticate(
            activity = this@MainActivity,
            title = "Flockr Locked",
            subtitle = "Verify your identity to access Flockr",
            onSuccess = { 
                // Unlock
                _isAppLocked.value = false
                lastBackgroundTimestamp = 0 
            },
            onError = { 
                // Keep locked, user can retry via button
            }
        )
    }
}

@Composable
fun LockScreenOverlay(onUnlockClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Flockr is Locked",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Unlock to access your finances",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onUnlockClick) {
                Text("Unlock")
            }
        }
    }
}
