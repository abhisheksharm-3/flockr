package `in`.xroden.flockr.features.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.notifications.model.NotificationPreference
import `in`.xroden.flockr.features.notifications.data.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationPreferencesViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val houseRepository: `in`.xroden.flockr.features.house.data.HouseRepository
) : ViewModel() {

    private val _preferences = MutableStateFlow<List<NotificationPreference>>(emptyList())
    val preferences: StateFlow<List<NotificationPreference>> = _preferences.asStateFlow()

    private val _houseNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val houseNames: StateFlow<Map<String, String>> = _houseNames.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get user's houses first
                val housesResult = houseRepository.getHouses()
                val houses = housesResult.getOrDefault(emptyList())
                val houseNameMap = mutableMapOf<String, String>()

                houses.forEach { house ->
                    houseNameMap[house.id] = house.name
                    // Ensure preferences exist for each house
                    notificationRepository.ensurePreferencesExist(house.id)
                }

                _houseNames.value = houseNameMap

                // Load notification preferences
                _preferences.value = notificationRepository.getNotificationPreferences()
            } catch (e: Exception) {
                android.util.Log.e("NotificationPrefs", "Error loading preferences", e)
                _message.value = "Failed to load preferences"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updatePreference(houseId: String, key: String, enabled: Boolean) {
        viewModelScope.launch {
            try {
                notificationRepository.updateNotificationPreferences(houseId, key, enabled)
                loadPreferences() // Reload to get updated data
                _message.value = "Preference updated"
            } catch (e: Exception) {
                android.util.Log.e("NotificationPrefs", "Error updating preference", e)
                _message.value = "Failed to update preference"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPreferencesScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationPreferencesViewModel = hiltViewModel()
) {
    val preferences by viewModel.preferences.collectAsState()
    val houseNames by viewModel.houseNames.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Notification Preferences",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Global Notification Settings
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Global Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "Manage notification preferences for all your households",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Per-House Notification Preferences
                if (preferences.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Notifications,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "No notification preferences yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Join a household to customize notifications",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(preferences) { pref ->
                        NotificationPreferenceCard(
                            preference = pref,
                            houseName = houseNames[pref.houseId] ?: "Unknown House",
                            onToggle = { key, enabled ->
                                viewModel.updatePreference(pref.houseId, key, enabled)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationPreferenceCard(
    preference: NotificationPreference,
    houseName: String,
    onToggle: (String, Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // House name header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Filled.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    houseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Notification toggles
            NotificationToggleRow(
                title = "Member Joined",
                icon = Icons.Filled.PersonAdd,
                enabled = preference.enableMemberJoined,
                onToggle = { onToggle("enable_member_joined", it) }
            )

            NotificationToggleRow(
                title = "Expense Added",
                icon = Icons.Filled.AccountBalanceWallet,
                enabled = preference.enableExpenseAdded,
                onToggle = { onToggle("enable_expense_added", it) }
            )

            NotificationToggleRow(
                title = "Chore Assigned",
                icon = Icons.Filled.CheckCircle,
                enabled = preference.enableChoreAssigned,
                onToggle = { onToggle("enable_chore_assigned", it) }
            )

            NotificationToggleRow(
                title = "Message Sent",
                icon = Icons.AutoMirrored.Filled.Message,
                enabled = preference.enableMessageSent,
                onToggle = { onToggle("enable_message_sent", it) }
            )

            NotificationToggleRow(
                title = "Shopping Item Added",
                icon = Icons.Filled.ShoppingCart,
                enabled = preference.enableShoppingItemAdded,
                onToggle = { onToggle("enable_shopping_item_added", it) }
            )
        }
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

