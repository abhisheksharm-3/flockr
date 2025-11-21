package `in`.xroden.flockr.features.chat.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.chat.data.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _sendState = MutableStateFlow<SendMessageUiState>(SendMessageUiState.Idle)
    val sendState: StateFlow<SendMessageUiState> = _sendState.asStateFlow()

    private var messagesJob: kotlinx.coroutines.Job? = null

    fun loadMessages(houseId: String) {
        // Cancel any existing job to avoid multiple subscriptions
        messagesJob?.cancel()

        messagesJob = viewModelScope.launch {
            _uiState.value = ChatUiState.Loading
            
            chatRepository.getMessagesFlow(houseId).collect { result ->
                result.fold(
                    onSuccess = { messages ->
                        val currentUserId = chatRepository.getCurrentUserId()
                        // Merge with pending messages, avoiding duplicates if possible (though IDs differ)
                        // We just append pending messages at the end (since they are new)
                        val allMessages = messages + _pendingMessages.value
                        _uiState.value = ChatUiState.Success(allMessages, currentUserId)
                    },
                    onFailure = { error ->
                        _uiState.value = ChatUiState.Error(
                            message = error.message ?: "Failed to load messages",
                            cause = error
                        )
                    }
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        messagesJob?.cancel()
    }

    private val _pendingMessages = MutableStateFlow<List<`in`.xroden.flockr.features.chat.model.Message>>(emptyList())

    fun sendMessage(houseId: String, content: String) {
        val currentUserId = chatRepository.getCurrentUserId() ?: return
        val tempId = java.util.UUID.randomUUID().toString()
        val tempMessage = `in`.xroden.flockr.features.chat.model.Message(
            id = tempId,
            houseId = houseId,
            userId = currentUserId,
            content = content,
            createdAt = kotlinx.datetime.Clock.System.now(),
            senderName = "You",
            isPending = true
        )

        viewModelScope.launch {
            _pendingMessages.value = _pendingMessages.value + tempMessage
            _sendState.value = SendMessageUiState.Sending
            
            // Update UI immediately with pending message
            val currentMessages = (_uiState.value as? ChatUiState.Success)?.messages ?: emptyList()
            _uiState.value = ChatUiState.Success(currentMessages + tempMessage, currentUserId)

            chatRepository.sendMessage(houseId, content).fold(
                onSuccess = {
                    _sendState.value = SendMessageUiState.Success
                    // Remove pending message after a short delay to allow realtime to catch up
                    // or immediately if we trust realtime. 
                    // Better: keep it until real message arrives? 
                    // For now, just remove it after a delay to prevent duplication if realtime is fast.
                    // Actually, if we remove it, it might flicker. 
                    // Ideally we deduplicate in the UI state.
                    _pendingMessages.value = _pendingMessages.value.filter { it.id != tempId }
                    
                    kotlinx.coroutines.delay(500)
                    _sendState.value = SendMessageUiState.Idle
                },
                onFailure = { error ->
                    _sendState.value = SendMessageUiState.Error(
                        message = error.message ?: "Failed to send message"
                    )
                    // Keep pending message but maybe mark as error? 
                    // For now, remove it so user can retry.
                    _pendingMessages.value = _pendingMessages.value.filter { it.id != tempId }
                }
            )
        }
    }

    fun resetSendState() {
        _sendState.value = SendMessageUiState.Idle
    }

    fun getCurrentUserId(): String? = chatRepository.getCurrentUserId()
}
