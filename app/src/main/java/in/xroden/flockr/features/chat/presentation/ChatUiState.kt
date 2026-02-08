package `in`.xroden.flockr.features.chat.presentation

import androidx.compose.runtime.Immutable
import `in`.xroden.flockr.features.chat.model.Message

@Immutable
sealed interface ChatUiState {
    data object Loading : ChatUiState
    data class Success(val messages: List<Message>, val currentUserId: String?) : ChatUiState
    data class Error(val message: String, val cause: Throwable? = null) : ChatUiState
}

@Immutable
sealed interface SendMessageUiState {
    data object Idle : SendMessageUiState
    data object Sending : SendMessageUiState
    data object Success : SendMessageUiState
    data class Error(val message: String) : SendMessageUiState
}


