package `in`.xroden.flockr.features.settings.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message

import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.notifications.model.NotificationPreference
import `in`.xroden.flockr.features.notifications.data.INotificationRepository
import `in`.xroden.flockr.features.house.data.IHouseRepository
import `in`.xroden.flockr.ui.components.loading.ListScreenSkeleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@HiltViewModel
class NotificationPreferencesViewModel @Inject constructor(
    private val notificationRepository: INotificationRepository,
    private val houseRepository: IHouseRepository
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
            runCatching {
                val housesResult = houseRepository.getHouses()
                val houses = housesResult.getOrDefault(emptyList())
                val houseNameMap = mutableMapOf<String, String>()

                houses.forEach { house ->
                    houseNameMap[house.id] = house.name
                    notificationRepository.ensurePreferencesExist(house.id)
                }

                _houseNames.value = houseNameMap
                _preferences.value = notificationRepository.getNotificationPreferences()
            }.onFailure {
                _message.value = "Failed to load preferences"
            }
            _isLoading.value = false
        }
    }

    fun updatePreference(houseId: String, key: String, enabled: Boolean) {
        // Optimistic update
        val currentPrefs = _preferences.value.toMutableList()
        val idx = currentPrefs.indexOfFirst { it.houseId == houseId }
        if (idx >= 0) {
            val pref = currentPrefs[idx]
            currentPrefs[idx] = when (key) {
                "enable_member_joined" -> pref.copy(enableMemberJoined = enabled)
                "enable_expense_added" -> pref.copy(enableExpenseAdded = enabled)
                "enable_chore_assigned" -> pref.copy(enableChoreAssigned = enabled)
                "enable_message_sent" -> pref.copy(enableMessageSent = enabled)
                "enable_shopping_item_added" -> pref.copy(enableShoppingItemAdded = enabled)
                else -> pref
            }
            _preferences.value = currentPrefs
        }

        viewModelScope.launch {
            runCatching {
                notificationRepository.updateNotificationPreferences(houseId, key, enabled)
            }.onFailure {
                loadPreferences() // Revert on failure
                _message.value = "Failed to update"
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
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val houseNames by viewModel.houseNames.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
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
                        "Notifications",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            isLoading -> {
                ListScreenSkeleton(modifier = Modifier.padding(padding))
            }
            preferences.isEmpty() -> {
                EmptyNotificationPreferences(modifier = Modifier.padding(padding))
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Info header
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Notifications,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Choose which notifications you want to receive for each household",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    items(preferences, key = { it.id }) { pref ->
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
private fun EmptyNotificationPreferences(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Notifications,
                        null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                "No Preferences Yet",
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

@Composable
private fun NotificationPreferenceCard(
    preference: NotificationPreference,
    houseName: String,
    onToggle: (String, Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Home,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    houseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            NotificationToggleRow(
                "Member Joined",
                Icons.Outlined.PersonAdd,
                preference.enableMemberJoined
            ) { onToggle("enable_member_joined", it) }
            
            NotificationToggleRow(
                "Expense Added",
                Icons.Outlined.Receipt,
                preference.enableExpenseAdded
            ) { onToggle("enable_expense_added", it) }
            
            NotificationToggleRow(
                "Chore Assigned",
                Icons.Outlined.TaskAlt,
                preference.enableChoreAssigned
            ) { onToggle("enable_chore_assigned", it) }
            
            NotificationToggleRow(
                "Message Sent",
                Icons.AutoMirrored.Filled.Message,
                preference.enableMessageSent
            ) { onToggle("enable_message_sent", it) }
            
            NotificationToggleRow(
                "Shopping Item",
                Icons.Outlined.ShoppingCart,
                preference.enableShoppingItemAdded
            ) { onToggle("enable_shopping_item_added", it) }
        }
    }
}

@Composable
private fun NotificationToggleRow(
    title: String,
    icon: ImageVector,
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
                null,
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
