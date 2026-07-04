package `in`.xroden.flockr.features.documents.presentation

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.documents.data.IDocumentRepository
import `in`.xroden.flockr.features.documents.domain.usecase.UploadDocumentUseCase
import `in`.xroden.flockr.features.documents.model.Document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class DocumentViewModel @Inject constructor(
    private val documentRepository: IDocumentRepository,
    private val uploadDocumentUseCase: UploadDocumentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<DocumentUiState>(DocumentUiState.Loading)
    val uiState: StateFlow<DocumentUiState> = _uiState.asStateFlow()

    private val _uploadState = MutableStateFlow<UploadDocumentUiState>(UploadDocumentUiState.Idle)
    val uploadState: StateFlow<UploadDocumentUiState> = _uploadState.asStateFlow()

    private val _events = Channel<DocumentEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _viewDocumentEvent = MutableSharedFlow<String>()
    val viewDocumentEvent = _viewDocumentEvent.asSharedFlow()

    private val _downloadEvent = MutableSharedFlow<DownloadRequest>()
    val downloadEvent = _downloadEvent.asSharedFlow()

    private val _messageEvent = MutableSharedFlow<String>()
    val messageEvent = _messageEvent.asSharedFlow()

    private var currentHouseId: String? = null

    val personalDocuments: StateFlow<List<Document>> =
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

    val houseDocuments: StateFlow<List<Document>> =
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

            val readResult = withContext(Dispatchers.IO) {
                runCatching {
                    // Check the declared size BEFORE reading, so a huge pick can't OOM the app.
                    val size = queryFileSize(context, uri)
                    if (size != null && size > MAX_UPLOAD_SIZE_BYTES) {
                        throw IllegalArgumentException("File is too large (max 10 MB)")
                    }
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw java.io.IOException("Could not read file")
                    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                    bytes to mimeType
                }
            }

            val (fileData, mimeType) = readResult.getOrElse { error ->
                _uploadState.value = UploadDocumentUiState.Error(error.message ?: "Could not read file")
                return@launch
            }

            // Route through the use case so document-count and image-size limits are enforced.
            uploadDocumentUseCase(houseId, fileName, fileData, mimeType).fold(
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
        }
    }

    fun uploadPersonalDocument(uri: Uri, fileName: String, context: Context) {
        uploadDocument(uri, fileName, context, houseId = null)
    }

    fun uploadHouseDocument(houseId: String, uri: Uri, fileName: String, context: Context) {
        uploadDocument(uri, fileName, context, houseId = houseId)
    }

    fun deleteDocument(documentId: String, storagePath: String, houseId: String?) {
        viewModelScope.launch {
            documentRepository.deleteDocument(documentId, storagePath, houseId).fold(
                onSuccess = { loadDocuments(currentHouseId) },
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

    fun viewDocument(document: Document) {
        viewModelScope.launch {
            documentRepository.getDocumentUrl(document.storagePath, document.houseId).fold(
                onSuccess = { url -> _viewDocumentEvent.emit(url) },
                onFailure = { error ->
                    _uiState.value = DocumentUiState.Error(
                        message = error.message ?: "Failed to get document URL",
                        cause = error
                    )
                }
            )
        }
    }

    fun downloadDocument(document: Document) {
        viewModelScope.launch {
            documentRepository.getDocumentUrl(document.storagePath, document.houseId).fold(
                onSuccess = { url ->
                    _downloadEvent.emit(DownloadRequest(url, document.fileName, document.mimeType))
                },
                onFailure = { error ->
                    _messageEvent.emit(error.message ?: "Could not download file")
                }
            )
        }
    }

    private fun queryFileSize(context: Context, uri: Uri): Long? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index >= 0 && cursor.moveToFirst() && !cursor.isNull(index)) cursor.getLong(index) else null
        }

    companion object {
        // Keep in sync with the repository's authoritative size limit.
        private const val MAX_UPLOAD_SIZE_BYTES = 10L * 1024 * 1024
    }
}

sealed class DocumentEvent {
    data object DocumentUploaded : DocumentEvent()
}

data class DownloadRequest(val url: String, val fileName: String, val mimeType: String?)
