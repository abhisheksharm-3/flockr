/** What the house chat shows, and the one-off events it reacts to. */
package `in`.xroden.flockr.features.chat.presentation

import `in`.xroden.flockr.features.chat.model.Message
import `in`.xroden.flockr.features.house.model.MemberWithProfile

sealed interface ChatUiState {
    data object Loading : ChatUiState
    data class Error(val message: String) : ChatUiState

    /** [messages] run oldest first, with the viewer's still-sending messages last and marked pending. */
    data class Ready(
        val messages: List<Message>,
        val members: Map<String, MemberWithProfile>,
        val viewerId: String,
    ) : ChatUiState
}

sealed interface ChatEvent {
    /** Sending [content] failed; the screen puts it back in the input so nothing typed is lost. */
    data class SendFailed(val content: String, val message: String) : ChatEvent
}
