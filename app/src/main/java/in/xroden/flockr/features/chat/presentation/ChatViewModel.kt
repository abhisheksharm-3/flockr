/** The house chat: the latest messages live, with the viewer's own shown at once while they send. */
package `in`.xroden.flockr.features.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.core.security.InputSanitizer
import `in`.xroden.flockr.features.chat.data.ChatRepository
import `in`.xroden.flockr.features.chat.model.Message
import `in`.xroden.flockr.features.house.data.HouseRepository
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val houseRepository: HouseRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private val _events = Channel<ChatEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val pending = MutableStateFlow<List<PendingMessage>>(emptyList())
    private var newestConfirmed: Instant? = null
    private var loadedHouseId: String? = null
    private var loadJob: Job? = null

    fun load(houseId: String) {
        if (loadedHouseId == houseId && loadJob?.isActive == true) return
        loadedHouseId = houseId
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = ChatUiState.Loading
            val members = async { houseRepository.getHouseMembers(houseId).getOrElse { emptyList() }.associateBy { it.userId } }
            val viewerId = chatRepository.getCurrentUserId().orEmpty()
            combine(chatRepository.getMessagesFlow(houseId), pending, ::Pair).collect { (result, waiting) ->
                result.fold(
                    onSuccess = { messages ->
                        newestConfirmed = messages.lastOrNull()?.createdAt
                        val arrived = waiting.filter { it.arrivedIn(messages) }.toSet()
                        if (arrived.isNotEmpty()) pending.update { it - arrived }
                        _state.value = ChatUiState.Ready(
                            messages = messages + (waiting - arrived).map { it.message },
                            members = members.await(),
                            viewerId = viewerId,
                        )
                    },
                    onFailure = { _state.value = ChatUiState.Error(it.userMessage()) },
                )
            }
        }
    }

    fun send(houseId: String, content: String) {
        val viewerId = chatRepository.getCurrentUserId() ?: return
        val text = InputSanitizer.sanitizeText(content)
        if (text.isEmpty()) return
        val waiting = PendingMessage(
            message = Message(
                id = UUID.randomUUID().toString(),
                houseId = houseId,
                userId = viewerId,
                content = text,
                createdAt = Clock.System.now(),
                isPending = true,
            ),
            after = newestConfirmed,
        )
        pending.update { it + waiting }
        viewModelScope.launch {
            chatRepository.sendMessage(houseId, text).onFailure { error ->
                pending.update { it - waiting }
                _events.send(ChatEvent.SendFailed(content, error.userMessage()))
            }
        }
    }
}

/**
 * A message the viewer sent that the server has not echoed back yet. [after] is the newest server
 * time the viewer had seen when sending, so an earlier message with the same text is not mistaken
 * for this one, and no device clock is compared with the server's.
 */
private data class PendingMessage(val message: Message, val after: Instant?) {
    fun arrivedIn(messages: List<Message>): Boolean = messages.any {
        it.userId == message.userId && it.content == message.content && (after == null || it.createdAt > after)
    }
}
