package `in`.xroden.flockr.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.data.model.Message
import `in`.xroden.flockr.data.repository.ChatRepository
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

    fun loadMessages(houseId: String, houseName: String) {
        viewModelScope.launch {
            try {
                chatRepository.getMessagesFlow(houseId).collect { messages ->
                    _uiState.value = ChatUiState.Success(messages, houseName)
                }
            } catch (e: Exception) {
                _uiState.value = ChatUiState.Error(e.message ?: "Failed to load messages")
            }
        }
    }

    fun sendMessage(houseId: String, content: String, houseName: String) {
        viewModelScope.launch {
            chatRepository.sendMessage(houseId, content, houseName)
        }
    }
}

sealed class ChatUiState {
    object Loading : ChatUiState()
    data class Success(val messages: List<Message>, val houseName: String) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}

