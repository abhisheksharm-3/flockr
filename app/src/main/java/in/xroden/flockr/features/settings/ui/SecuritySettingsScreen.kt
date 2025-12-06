package `in`.xroden.flockr.features.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.features.settings.domain.SettingsViewModel
import `in`.xroden.flockr.utils.BiometricAuthManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val lockEnabled by viewModel.appLockEnabled.collectAsState(initial = false)
    val context = LocalContext.current
    var showBiometricError by remember { mutableStateOf<String?>(null) }
    
    val biometricManager = remember { BiometricAuthManager(context) }
    val canAuthenticate = remember { biometricManager.canAuthenticate() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Security", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // App Lock Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                "App Lock",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Require biometrics to open",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = lockEnabled,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                // Verify before enabling
                                if (canAuthenticate) {
                                    val activity = context as? FragmentActivity
                                    if (activity != null) {
                                        biometricManager.authenticate(
                                            activity = activity,
                                            onSuccess = {
                                                viewModel.setAppLockEnabled(true)
                                            },
                                            onError = {
                                                showBiometricError = it
                                            }
                                        )
                                    } else {
                                        showBiometricError = "Activity context required"
                                    }
                                } else {
                                    showBiometricError = "Biometrics not available"
                                }
                            } else {
                                viewModel.setAppLockEnabled(false)
                            }
                        },
                        enabled = canAuthenticate
                    )
                }
            }

            if (!canAuthenticate) {
                Text(
                    "Biometric authentication is not set up on this device.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            showBiometricError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
