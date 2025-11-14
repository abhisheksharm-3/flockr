package `in`.xroden.flockr.features.chat.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.chat.model.Message
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

    private var messagesJob: kotlinx.coroutines.Job? = null

    fun loadMessages(houseId: String, houseName: String) {
        // Cancel any existing job to avoid multiple subscriptions
        messagesJob?.cancel()

        messagesJob = viewModelScope.launch {
            _uiState.value = ChatUiState.Loading
            android.util.Log.d("ChatViewModel", "Loading messages for house: $houseId")
            try {
                chatRepository.getMessagesFlow(houseId).collect { messages ->
                    android.util.Log.d("ChatViewModel", "Received ${messages.size} messages from flow")
                    _uiState.value = ChatUiState.Success(messages, houseName)
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error loading messages", e)
                _uiState.value = ChatUiState.Error(e.message ?: "Failed to load messages")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        messagesJob?.cancel()
    }

    fun sendMessage(houseId: String, content: String, houseName: String) {
        viewModelScope.launch {
            android.util.Log.d("ChatViewModel", "Sending message: $content")
            val result = chatRepository.sendMessage(houseId, content, houseName)
            if (result.isSuccess) {
                android.util.Log.d("ChatViewModel", "Message sent successfully - realtime flow will update UI")
                // Don't manually update - let the realtime flow handle it
            } else {
                android.util.Log.e("ChatViewModel", "Failed to send message: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun getCurrentUserId(): String? = chatRepository.getCurrentUserId()
}

sealed class ChatUiState {
    object Loading : ChatUiState()
    data class Success(val messages: List<Message>, val houseName: String) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}

