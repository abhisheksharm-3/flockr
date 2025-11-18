package `in`.xroden.flockr.features.chat.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.features.chat.model.Message
import `in`.xroden.flockr.features.chat.domain.ChatUiState
import `in`.xroden.flockr.features.chat.domain.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(houseId) {
        viewModel.loadMessages(houseId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Chat",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    ),
                tonalElevation = 3.dp,
                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        maxLines = 3,
                        shape = MaterialTheme.shapes.extraSmall,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(houseId, messageText)
                                messageText = ""
                            }
                        },
                        enabled = messageText.isNotBlank(),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (messageText.isNotBlank())
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                2.dp,
                                if (messageText.isNotBlank())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    "Send",
                                    tint = if (messageText.isNotBlank())
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is ChatUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                }
            }
            is ChatUiState.Success -> {
                if (state.messages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(80.dp).border(
                                    2.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium
                                ),
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                "NO MESSAGES",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                "Start a conversation with your household",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        reverseLayout = true
                    ) {
                        items(state.messages.reversed()) { message ->
                            MessageBubble(
                                message = message,
                                currentUserId = viewModel.getCurrentUserId()
                            )
                        }

                        // Security Warning Banner at the bottom (shows at top due to reverseLayout)
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "⚠️ This chat is not encrypted. Please do not share sensitive information like passwords or financial details. We do not access your messages, but they are stored in a way that could be viewed. Use this chat for household coordination only.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
            is ChatUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "ERROR",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    currentUserId: String? = null
) {
    val isCurrentUser = currentUserId != null && message.userId == currentUserId
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .border(
                    width = 2.dp,
                    color = if (isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isCurrentUser) 12.dp else 4.dp,
                        bottomEnd = if (isCurrentUser) 4.dp else 12.dp
                    )
                ),
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isCurrentUser) 12.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 12.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isCurrentUser) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Show sender name for all messages
                Text(
                    text = if (isCurrentUser) "You" else (message.senderName ?: "Unknown"),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrentUser)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.secondary,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                message.createdAt.let { timestamp ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatTimestamp(timestamp.toString()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: String): String {
    return try {
        val instant = java.time.Instant.parse(timestamp)
        val messageTime = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
        val now = java.time.LocalDateTime.now()
        
        val minutesAgo = java.time.Duration.between(messageTime, now).toMinutes()
        val hoursAgo = java.time.Duration.between(messageTime, now).toHours()
        val daysAgo = java.time.Duration.between(messageTime, now).toDays()
        
        when {
            minutesAgo < 1 -> "Now"
            minutesAgo < 60 -> "${minutesAgo}m ago"
            hoursAgo < 24 -> "${hoursAgo}h ago"
            daysAgo == 1L -> "Yesterday"
            daysAgo < 7 -> "${daysAgo}d ago"
            else -> {
                val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d")
                messageTime.format(formatter)
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("ChatScreen", "Error formatting timestamp: $timestamp", e)
        "Unknown"
    }
}
