package `in`.xroden.flockr.features.house.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.data.HouseAuditRepository
import `in`.xroden.flockr.features.house.data.IHouseRepository
import `in`.xroden.flockr.utils.formatWithHouseConfig
import `in`.xroden.flockr.utils.getTimezone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime
import java.util.*
import javax.inject.Inject
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.json.JsonPrimitive
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@HiltViewModel
class HouseAuditLogViewModel @Inject constructor(
    private val houseAuditRepository: HouseAuditRepository,
    private val houseRepository: IHouseRepository
) : ViewModel() {

    private val _auditLogs = MutableStateFlow<List<HouseAuditLog>>(emptyList())
    val auditLogs: StateFlow<List<HouseAuditLog>> = _auditLogs.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _houseConfig = MutableStateFlow<HouseConfig?>(null)
    val houseConfig: StateFlow<HouseConfig?> = _houseConfig.asStateFlow()

    fun loadHouseConfig(houseId: String) {
        viewModelScope.launch {
            houseRepository.getHouseConfig(houseId).onSuccess { _houseConfig.value = it }
        }
    }

    fun loadAuditLogs(houseId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val logs = houseAuditRepository.getHouseAuditLogs(houseId)
                _auditLogs.value = logs
            } catch (_: Exception) {
                _auditLogs.value = emptyList()
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
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val houseConfig by viewModel.houseConfig.collectAsStateWithLifecycle()

    LaunchedEffect(houseId) {
        viewModel.loadAuditLogs(houseId)
        viewModel.loadHouseConfig(houseId)
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
                colors = TopAppBarDefaults.topAppBarColors(
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
                        EmptyAuditLogState()
                    }
                } else {
                    // Group logs by date (in the house's timezone + date format)
                    val groupedLogs = auditLogs.groupBy {
                       it.createdAt.toLocalDateTime(houseConfig.getTimezone()).date
                           .formatWithHouseConfig(houseConfig)
                    }

                    groupedLogs.forEach { (dateHeader, logs) ->
                         item {
                             Text(
                                 text = dateHeader,
                                 style = MaterialTheme.typography.labelLarge,
                                 color = MaterialTheme.colorScheme.primary,
                                 fontWeight = FontWeight.Bold,
                                 modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)
                             )
                         }
                         items(logs, key = { it.id }) { log ->
                             AuditLogCard(log, houseConfig)
                         }
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
fun EmptyAuditLogState() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Text(
                "No activity yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "House activity will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AuditLogCard(log: HouseAuditLog, houseConfig: HouseConfig?) {
    val time = remember(log.createdAt, houseConfig) {
        val dateTime = log.createdAt.toLocalDateTime(houseConfig.getTimezone())
        val hour12 = when {
            dateTime.hour == 0 -> 12
            dateTime.hour > 12 -> dateTime.hour - 12
            else -> dateTime.hour
        }
        val amPm = if (dateTime.hour < 12) "AM" else "PM"
        "${hour12.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')} $amPm"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon based on action type
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = getActionColor(log.action).copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        getActionIcon(log.action),
                        contentDescription = null,
                        tint = getActionColor(log.action),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getActionDescription(log.action, log.targetUserId),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Details map formatting
                if (log.details.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                                MaterialTheme.shapes.small
                            )
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                         log.details.forEach { (key, valueElement) ->
                             if (key != "id" && key != "house_id") {
                             val value = if (valueElement is JsonPrimitive && valueElement.isString) {
                                     valueElement.content
                                 } else {
                                     valueElement.toString()
                                 }
                                 Row(
                                     verticalAlignment = Alignment.Top
                                 ) {
                                     Text(
                                         text = "${formatKey(key)}: ",
                                         style = MaterialTheme.typography.bodySmall,
                                         fontWeight = FontWeight.SemiBold,
                                         color = MaterialTheme.colorScheme.onSurfaceVariant
                                     )
                                     Text(
                                         text = value,
                                         style = MaterialTheme.typography.bodySmall,
                                         color = MaterialTheme.colorScheme.onSurface
                                     )
                                 }
                             }
                         }
                    }
                }
            }
        }
    }
}

private fun formatKey(key: String): String {
    return key.replace("_", " ")
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

@Composable
private fun getActionColor(action: String): Color {
    return when (action.lowercase()) {
        "member_added", "member_joined" -> Color(0xFF4CAF50) // Green
        "member_removed", "member_left" -> Color(0xFFF44336) // Red
        "role_changed" -> Color(0xFFFF9800) // Orange
        "house_updated" -> Color(0xFF2196F3) // Blue
        "expense_added" -> Color(0xFFE91E63) // Pink
        "chore_created" -> Color(0xFF9C27B0) // Purple
        else -> MaterialTheme.colorScheme.secondary
    }
}

@Composable
private fun getActionIcon(action: String): ImageVector {
    return when (action.lowercase()) {
        "member_added", "member_joined" -> Icons.Default.PersonAdd
        "member_removed", "member_left" -> Icons.Default.PersonRemove
        "role_changed" -> Icons.Default.AdminPanelSettings
        "house_updated" -> Icons.Default.Edit
        "expense_added" -> Icons.AutoMirrored.Filled.ReceiptLong
        "chore_created" -> Icons.Default.CleaningServices
        else -> Icons.Default.Info
    }
}

private fun getActionDescription(action: String, targetUserId: String?): String {
    return when (action.lowercase()) {
        "member_added" -> "New member added"
        "member_joined" -> "Member joined"
        "member_removed" -> "Member removed"
        "member_left" -> "Member left"
        "role_changed" -> "Role updated"
        "house_updated" -> "House settings updated"
        "expense_added" -> "Expense added"
        "chore_created" -> "Chore created"
        else -> action.replace("_", " ").replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }
}
