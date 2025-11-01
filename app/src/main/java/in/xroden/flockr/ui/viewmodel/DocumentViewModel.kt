package `in`.xroden.flockr.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.data.model.Document
import `in`.xroden.flockr.data.repository.DocumentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DocumentViewModel @Inject constructor(
    private val documentRepository: DocumentRepository
) : ViewModel() {

    private val _personalDocuments = MutableStateFlow<DocumentUiState>(DocumentUiState.Loading)
    val personalDocuments: StateFlow<DocumentUiState> = _personalDocuments.asStateFlow()

    private val _houseDocuments = MutableStateFlow<DocumentUiState>(DocumentUiState.Loading)
    val houseDocuments: StateFlow<DocumentUiState> = _houseDocuments.asStateFlow()

    fun loadPersonalDocuments() {
        viewModelScope.launch {
            _personalDocuments.value = DocumentUiState.Loading
            try {
                val documents = documentRepository.getPersonalDocuments()
                _personalDocuments.value = DocumentUiState.Success(documents)
            } catch (e: Exception) {
                _personalDocuments.value = DocumentUiState.Error(e.message ?: "Failed to load documents")
            }
        }
    }

    fun loadHouseDocuments(houseId: String) {
        viewModelScope.launch {
            _houseDocuments.value = DocumentUiState.Loading
            try {
                val documents = documentRepository.getHouseDocuments(houseId)
                _houseDocuments.value = DocumentUiState.Success(documents)
            } catch (e: Exception) {
                _houseDocuments.value = DocumentUiState.Error(e.message ?: "Failed to load documents")
            }
        }
    }

    fun uploadPersonalDocument(uri: Uri, fileName: String, context: Context) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val fileData = inputStream?.readBytes()
                if (fileData == null) {
                    _personalDocuments.value = DocumentUiState.Error("Could not read file")
                    return@launch
                }
                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

                val result = documentRepository.uploadDocument(null, fileName, fileData, mimeType)
                result.fold(
                    onSuccess = {
                        loadPersonalDocuments()
                    },
                    onFailure = { error ->
                        _personalDocuments.value = DocumentUiState.Error(error.message ?: "Upload failed")
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("DocumentViewModel", "Upload error", e)
                _personalDocuments.value = DocumentUiState.Error(e.message ?: "Upload failed")
            }
        }
    }

    fun uploadHouseDocument(houseId: String, uri: Uri, fileName: String, context: Context) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val fileData = inputStream?.readBytes()
                if (fileData == null) {
                    _houseDocuments.value = DocumentUiState.Error("Could not read file")
                    return@launch
                }
                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

                val result = documentRepository.uploadDocument(houseId, fileName, fileData, mimeType)
                result.fold(
                    onSuccess = {
                        loadHouseDocuments(houseId)
                    },
                    onFailure = { error ->
                        _houseDocuments.value = DocumentUiState.Error(error.message ?: "Upload failed")
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("DocumentViewModel", "Upload error", e)
                _houseDocuments.value = DocumentUiState.Error(e.message ?: "Upload failed")
            }
        }
    }

    fun downloadDocument(document: Document, context: Context) {
        // TODO: Implement download functionality
        // For now, this is a placeholder
        viewModelScope.launch {
            try {
                // Download functionality to be implemented
                android.util.Log.d("DocumentViewModel", "Download requested for: ${document.fileName}")
            } catch (_: Exception) {
                // Handle error silently
            }
        }
    }

    fun deleteDocument(documentId: String, storagePath: String = "") {
        viewModelScope.launch {
            try {
                documentRepository.deleteDocument(documentId, storagePath)
            } catch (_: Exception) {
                // Handle error silently
            }
        }
    }
}

sealed class DocumentUiState {
    object Loading : DocumentUiState()
    data class Success(val documents: List<Document>) : DocumentUiState()
    data class Error(val message: String) : DocumentUiState()
}

