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
                        _uiState.value = ChatUiState.Success(messages, currentUserId)
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
        viewModelScope.launch {
            _sendState.value = SendMessageUiState.Sending
            
            chatRepository.sendMessage(houseId, content).fold(
                onSuccess = {
                    _sendState.value = SendMessageUiState.Success
                    kotlinx.coroutines.delay(500)
                    _sendState.value = SendMessageUiState.Idle
                },
                onFailure = { error ->
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
