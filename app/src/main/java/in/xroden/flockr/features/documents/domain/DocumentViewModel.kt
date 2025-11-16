package `in`.xroden.flockr.features.documents.domain

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.documents.data.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DocumentViewModel @Inject constructor(
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DocumentUiState>(DocumentUiState.Loading)
    val uiState: StateFlow<DocumentUiState> = _uiState.asStateFlow()

    private val _uploadState = MutableStateFlow<UploadDocumentUiState>(UploadDocumentUiState.Idle)
    val uploadState: StateFlow<UploadDocumentUiState> = _uploadState.asStateFlow()

    private var currentHouseId: String? = null

    fun loadDocuments(houseId: String? = null) {
        viewModelScope.launch {
            _uiState.value = DocumentUiState.Loading
            currentHouseId = houseId
            
            val personalResult = documentRepository.getPersonalDocuments()
            val houseResult = if (houseId != null) {
                documentRepository.getHouseDocuments(houseId)
            } else {
                Result.success(emptyList())
            }
            
            val personal = personalResult.getOrElse { emptyList() }
            val house = houseResult.getOrElse { emptyList() }
            
            if (personalResult.isFailure && houseResult.isFailure) {
                _uiState.value = DocumentUiState.Error(
                    message = personalResult.exceptionOrNull()?.message ?: "Failed to load documents",
                    cause = personalResult.exceptionOrNull()
                )
            } else {
                _uiState.value = DocumentUiState.Success(
                    personalDocuments = personal,
                    houseDocuments = house
                )
            }
        }
    }

    fun uploadDocument(uri: Uri, fileName: String, context: Context, houseId: String? = null) {
        viewModelScope.launch {
            _uploadState.value = UploadDocumentUiState.Uploading
            
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val fileData = inputStream?.readBytes()
                if (fileData == null) {
                    _uploadState.value = UploadDocumentUiState.Error("Could not read file")
                    return@launch
                }
                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

                documentRepository.uploadDocument(houseId, fileName, fileData, mimeType).fold(
                    onSuccess = {
                        _uploadState.value = UploadDocumentUiState.Success
                        loadDocuments(currentHouseId)
                        kotlinx.coroutines.delay(1000)
                        _uploadState.value = UploadDocumentUiState.Idle
                    },
                    onFailure = { error ->
                        _uploadState.value = UploadDocumentUiState.Error(
                            message = error.message ?: "Upload failed"
                        )
                    }
                )
            } catch (e: Exception) {
                _uploadState.value = UploadDocumentUiState.Error(e.message ?: "Upload failed")
            }
        }
    }

    fun deleteDocument(documentId: String, storagePath: String) {
        viewModelScope.launch {
            documentRepository.deleteDocument(documentId, storagePath).fold(
                onSuccess = {
                    loadDocuments(currentHouseId)
                },
                onFailure = { error ->
                    _uiState.value = DocumentUiState.Error(
                        message = error.message ?: "Failed to delete document",
                        cause = error
                    )
                }
            )
        }
    }

    fun resetUploadState() {
        _uploadState.value = UploadDocumentUiState.Idle
    }
}
