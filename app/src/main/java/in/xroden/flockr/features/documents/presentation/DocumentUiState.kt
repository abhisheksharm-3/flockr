/** What the documents screen shows, and the one-off actions it hands to the system. */
package `in`.xroden.flockr.features.documents.presentation

import `in`.xroden.flockr.core.presentation.Notice
import `in`.xroden.flockr.features.documents.model.Document
import `in`.xroden.flockr.features.house.model.MemberWithProfile

sealed interface DocumentUiState {
    data object Loading : DocumentUiState
    data class Error(val message: String) : DocumentUiState

    /** Both lists are newest first. [isAdmin] is true when the viewer owns or administers the house. */
    data class Ready(
        val house: List<Document>,
        val personal: List<Document>,
        val members: Map<String, MemberWithProfile>,
        val viewerId: String,
        val isAdmin: Boolean,
        val isUploading: Boolean = false,
    ) : DocumentUiState {
        /** Whoever added a document can delete it, and in a house so can an owner or admin. */
        fun canDelete(document: Document): Boolean = document.userId == viewerId || (document.houseId != null && isAdmin)
    }
}

sealed interface DocumentEvent {
    data class Show(val notice: Notice) : DocumentEvent
    data class Open(val url: String) : DocumentEvent
    data class Download(val url: String, val fileName: String, val mimeType: String?) : DocumentEvent
}
