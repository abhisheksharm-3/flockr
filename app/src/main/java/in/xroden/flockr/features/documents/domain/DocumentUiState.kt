package `in`.xroden.flockr.features.documents.domain

import androidx.compose.runtime.Immutable
import `in`.xroden.flockr.features.documents.model.Document

@Immutable
sealed interface DocumentUiState {
    data object Loading : DocumentUiState
    data class Success(
        val personalDocuments: List<Document>,
        val houseDocuments: List<Document>
    ) : DocumentUiState
    data class Error(val message: String, val cause: Throwable? = null) : DocumentUiState
}

@Immutable
sealed interface UploadDocumentUiState {
    data object Idle : UploadDocumentUiState
    data object Uploading : UploadDocumentUiState
    data object Success : UploadDocumentUiState
    data class Error(val message: String) : UploadDocumentUiState
}


