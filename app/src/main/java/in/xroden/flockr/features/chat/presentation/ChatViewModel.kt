package `in`.xroden.flockr.features.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.chat.data.IChatRepository
import `in`.xroden.flockr.features.chat.model.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Clock

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: IChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _sendState = MutableStateFlow<SendMessageUiState>(SendMessageUiState.Idle)
    val sendState: StateFlow<SendMessageUiState> = _sendState.asStateFlow()

    private val _pendingMessages = MutableStateFlow<List<Message>>(emptyList())

    private var messagesJob: kotlinx.coroutines.Job? = null
    private var currentHouseId: String? = null

    fun loadMessages(houseId: String) {
        if (currentHouseId == houseId && messagesJob?.isActive == true) return

        messagesJob?.cancel()
        currentHouseId = houseId

        messagesJob = viewModelScope.launch {
            if (_uiState.value !is ChatUiState.Success) {
                _uiState.value = ChatUiState.Loading
            }

            chatRepository.getMessagesFlow(houseId).collect { result ->
                result.fold(
                    onSuccess = { messages ->
                        val currentUserId = chatRepository.getCurrentUserId()
                        val pending = _pendingMessages.value
                        val pendingIds = pending.map { it.id }.toSet()
                        val deduplicated = messages.filterNot { it.id in pendingIds }
                        _uiState.value = ChatUiState.Success(deduplicated + pending, currentUserId)
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

    fun sendMessage(houseId: String, content: String) {
        val currentUserId = chatRepository.getCurrentUserId() ?: return
        val tempId = UUID.randomUUID().toString()
        val tempMessage = Message(
            id = tempId,
            houseId = houseId,
            userId = currentUserId,
            content = content,
            createdAt = Clock.System.now(),
            senderName = "You",
            isPending = true
        )

        viewModelScope.launch {
            _pendingMessages.value = _pendingMessages.value + tempMessage
            _sendState.value = SendMessageUiState.Sending

            val currentMessages = (_uiState.value as? ChatUiState.Success)?.messages ?: emptyList()
            _uiState.value = ChatUiState.Success(currentMessages + tempMessage, currentUserId)

            chatRepository.sendMessage(houseId, content).fold(
                onSuccess = {
                    _pendingMessages.value = _pendingMessages.value.filter { it.id != tempId }
                    _sendState.value = SendMessageUiState.Success
                },
                onFailure = { error ->
                    _pendingMessages.value = _pendingMessages.value.filter { it.id != tempId }
                    _sendState.value = SendMessageUiState.Error(
                        message = error.message ?: "Failed to send message"
                    )
                }
            )
        }
    }

    fun resetSendState() {
        _sendState.value = SendMessageUiState.Idle
    }

    fun getCurrentUserId(): String? = chatRepository.getCurrentUserId()
}
