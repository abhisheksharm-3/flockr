package `in`.xroden.flockr.features.documents.domain

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.documents.data.DocumentRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
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

    private val _events = Channel<DocumentEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var currentHouseId: String? = null

    // Convenience properties for UI screens
    val personalDocuments: StateFlow<List<`in`.xroden.flockr.features.documents.model.Document>> =
        _uiState.map { state ->
            when (state) {
                is DocumentUiState.Success -> state.personalDocuments
                else -> emptyList()
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val houseDocuments: StateFlow<List<`in`.xroden.flockr.features.documents.model.Document>> =
        _uiState.map { state ->
            when (state) {
                is DocumentUiState.Success -> state.houseDocuments
                else -> emptyList()
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

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
                        _events.send(DocumentEvent.DocumentUploaded)
                        loadDocuments(currentHouseId)
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

    // Wrapper methods for clarity in UI
    fun uploadPersonalDocument(uri: Uri, fileName: String, context: Context) {
        uploadDocument(uri, fileName, context, houseId = null)
    }

    fun uploadHouseDocument(houseId: String, uri: Uri, fileName: String, context: Context) {
        uploadDocument(uri, fileName, context, houseId = houseId)
    }

    fun deleteDocument(documentId: String, storagePath: String, houseId: String?) {
        viewModelScope.launch {
            documentRepository.deleteDocument(documentId, storagePath, houseId).fold(
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

    // Convenience methods for UI screens
    fun loadPersonalDocuments() {
        viewModelScope.launch {
            val result = documentRepository.getPersonalDocuments()
            if (result.isSuccess) {
                val current = _uiState.value
                if (current is DocumentUiState.Success) {
                    _uiState.value = current.copy(personalDocuments = result.getOrElse { emptyList() })
                } else {
                    _uiState.value = DocumentUiState.Success(
                        personalDocuments = result.getOrElse { emptyList() },
                        houseDocuments = emptyList()
                    )
                }
            }
        }
    }

    fun loadHouseDocuments(houseId: String) {
        currentHouseId = houseId
        viewModelScope.launch {
            val result = documentRepository.getHouseDocuments(houseId)
            if (result.isSuccess) {
                val current = _uiState.value
                if (current is DocumentUiState.Success) {
                    _uiState.value = current.copy(houseDocuments = result.getOrElse { emptyList() })
                } else {
                    _uiState.value = DocumentUiState.Success(
                        personalDocuments = emptyList(),
                        houseDocuments = result.getOrElse { emptyList() }
                    )
                }
            }
        }
    }
    

    private val _viewDocumentEvent = kotlinx.coroutines.flow.MutableSharedFlow<String>()
    val viewDocumentEvent = _viewDocumentEvent.asSharedFlow()

    fun viewDocument(document: `in`.xroden.flockr.features.documents.model.Document) {
        viewModelScope.launch {
            documentRepository.getDocumentUrl(document.storagePath, document.houseId).fold(
                onSuccess = { url ->
                    _viewDocumentEvent.emit(url)
                },
                onFailure = { error ->
                    _uiState.value = DocumentUiState.Error(
                        message = error.message ?: "Failed to get document URL",
                        cause = error
                    )
                }
            )
        }
    }

    fun downloadDocument(document: `in`.xroden.flockr.features.documents.model.Document) {
        // For now, we'll just treat download as view, as Android handles downloads via browser/intent best
        viewDocument(document)
    }
}

sealed class DocumentEvent {
    object DocumentUploaded : DocumentEvent()
}

