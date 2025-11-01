package `in`.xroden.flockr.ui.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.data.model.Message
import `in`.xroden.flockr.ui.viewmodel.ChatUiState
import `in`.xroden.flockr.ui.viewmodel.ChatViewModel

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
        android.util.Log.d("ChatScreen", "Screen launched for house: $houseId")
        viewModel.loadMessages(houseId, "House")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState is ChatUiState.Success) {
                            (uiState as ChatUiState.Success).houseName
                        } else {
                            "Chat"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        maxLines = 3
                    )
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                android.util.Log.d("ChatScreen", "Send button clicked, message: $messageText")
                                val houseName = if (uiState is ChatUiState.Success) {
                                    (uiState as ChatUiState.Success).houseName
                                } else {
                                    "House"
                                }
                                viewModel.sendMessage(houseId, messageText, houseName)
                                android.util.Log.d("ChatScreen", "Message sent to ViewModel, clearing text field")
                                messageText = ""
                            }
                        },
                        enabled = messageText.isNotBlank()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send")
                    }
                }
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is ChatUiState.Loading -> {
                android.util.Log.d("ChatScreen", "UI State: Loading")
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is ChatUiState.Success -> {
                android.util.Log.d("ChatScreen", "UI State: Success with ${state.messages.size} messages")
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.messages) { message ->
                        MessageBubble(message = message)
                    }
                }
            }
            is ChatUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    `in`.xroden.flockr.ui.components.cards.FlockrCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Show sender name
        Text(
            text = message.senderName ?: "Unknown",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
        )
        // Message content
        Text(
            text = message.content,
            style = MaterialTheme.typography.bodyMedium
        )
        // Timestamp
        Text(
            text = formatTimestamp(message.createdAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun formatTimestamp(timestamp: String): String {
    return try {
        // Format: 2025-10-31T09:44:23.228343+00:00
        val instant = java.time.Instant.parse(timestamp)
        val localTime = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
        val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
        localTime.format(formatter)
    } catch (e: Exception) {
        android.util.Log.e("ChatScreen", "Error formatting timestamp: $timestamp", e)
        timestamp
    }
}

