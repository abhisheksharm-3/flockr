package `in`.xroden.flockr.features.house.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import `in`.xroden.flockr.features.house.model.HouseAuditLog
import `in`.xroden.flockr.features.house.data.HouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HouseAuditLogViewModel @Inject constructor(
    private val houseRepository: HouseRepository
) : ViewModel() {

    private val _auditLogs = MutableStateFlow<List<HouseAuditLog>>(emptyList())
    val auditLogs: StateFlow<List<HouseAuditLog>> = _auditLogs.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadAuditLogs(houseId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _auditLogs.value = houseRepository.getHouseAuditLogs(houseId)
            } catch (e: Exception) {
                android.util.Log.e("HouseAuditLog", "Error loading audit logs", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseAuditLogScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: HouseAuditLogViewModel = hiltViewModel()
) {
    val auditLogs by viewModel.auditLogs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(houseId) {
        viewModel.loadAuditLogs(houseId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Activity Log",
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
        }
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (auditLogs.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "No activity yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "House activity will appear here",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(auditLogs) { log ->
                        AuditLogCard(log)
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditLogCard(log: HouseAuditLog) {
    val outputFormat = remember { SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()) }

    val formattedDate = remember(log.createdAt) {
        try {
            val instant = log.createdAt
            val date = Date(instant.toEpochMilliseconds())
            outputFormat.format(date)
        } catch (e: Exception) {
            android.util.Log.e("AuditLogCard", "Error parsing date: ${log.createdAt}", e)
            log.createdAt.toString()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon based on action type
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = getActionColor(log.action),
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        getActionIcon(log.action),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = getActionDescription(log.action, log.targetUserId),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Show details if available
                if (log.details.isNotEmpty()) {
                    Text(
                        log.details.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun getActionColor(action: String): androidx.compose.ui.graphics.Color {
    return when (action.lowercase()) {
        "member_added", "member_joined" -> MaterialTheme.colorScheme.primary
        "member_removed", "member_left" -> MaterialTheme.colorScheme.error
        "role_changed" -> MaterialTheme.colorScheme.tertiary
        "house_updated" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primaryContainer
    }
}

@Composable
private fun getActionIcon(action: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (action.lowercase()) {
        "member_added", "member_joined" -> Icons.Default.PersonAdd
        "member_removed", "member_left" -> Icons.Default.PersonRemove
        "role_changed" -> Icons.Default.AdminPanelSettings
        "house_updated" -> Icons.Default.Edit
        "expense_added" -> Icons.Default.AccountBalanceWallet
        "chore_created" -> Icons.Default.CheckCircle
        else -> Icons.Default.Info
    }
}

private fun getActionDescription(action: String, targetUserId: String?): String {
    return when (action.lowercase()) {
        "member_added" -> "New member added"
        "member_joined" -> "Member joined the house"
        "member_removed" -> "Member removed"
        "member_left" -> "Member left the house"
        "role_changed" -> "Member role changed"
        "house_updated" -> "House settings updated"
        "expense_added" -> "New expense added"
        "chore_created" -> "New chore created"
        else -> action.replace("_", " ").replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }
}
