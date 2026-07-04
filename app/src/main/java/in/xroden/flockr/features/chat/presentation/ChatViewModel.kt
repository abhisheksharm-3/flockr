package `in`.xroden.flockr.features.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.chat.data.IChatRepository
import `in`.xroden.flockr.features.chat.model.Message
import `in`.xroden.flockr.features.house.data.IHouseRepository
import `in`.xroden.flockr.features.house.model.HouseConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Clock

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: IChatRepository,
    private val houseRepository: IHouseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _sendState = MutableStateFlow<SendMessageUiState>(SendMessageUiState.Idle)
    val sendState: StateFlow<SendMessageUiState> = _sendState.asStateFlow()

    private val _houseConfig = MutableStateFlow<HouseConfig?>(null)
    val houseConfig: StateFlow<HouseConfig?> = _houseConfig.asStateFlow()

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
                        // Reconcile optimistic messages by (sender, content): once the server row
                        // exists, drop the temp. Matching on tempId never worked (server id differs),
                        // which caused duplicate bubbles and permanently-"pending" messages.
                        val stillPending = _pendingMessages.updateAndGet { current ->
                            current.filter { pendingMessage ->
                                messages.none {
                                    it.userId == pendingMessage.userId && it.content == pendingMessage.content
                                }
                            }
                        }
                        _uiState.value = ChatUiState.Success(messages + stillPending, currentUserId)
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

    fun loadHouseConfig(houseId: String) {
        viewModelScope.launch {
            houseRepository.getHouseConfig(houseId).onSuccess { _houseConfig.value = it }
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
            _pendingMessages.update { current -> current + tempMessage }
            _sendState.value = SendMessageUiState.Sending

            _uiState.update { current ->
                val currentMessages = (current as? ChatUiState.Success)?.messages ?: emptyList()
                ChatUiState.Success(currentMessages + tempMessage, currentUserId)
            }

            chatRepository.sendMessage(houseId, content).fold(
                onSuccess = {
                    // Keep the optimistic message until the realtime echo arrives; the collector
                    // above reconciles it away by (sender, content), avoiding a flicker/disappear.
                    _sendState.value = SendMessageUiState.Success
                },
                onFailure = { error ->
                    _pendingMessages.update { current -> current.filter { it.id != tempId } }
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
