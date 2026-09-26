/** The house's shared documents and the viewer's personal ones: listing, uploading, opening and deleting. */
package `in`.xroden.flockr.features.documents.presentation

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.xroden.flockr.core.domain.DomainError
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.core.presentation.Notice
import `in`.xroden.flockr.core.storage.StorageRepository
import `in`.xroden.flockr.core.validation.Validators
import `in`.xroden.flockr.features.house.model.HouseMemberRole
import `in`.xroden.flockr.features.documents.data.DocumentRepository
import `in`.xroden.flockr.features.documents.model.Document
import `in`.xroden.flockr.features.house.data.HouseRepository
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class DocumentViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val documentRepository: DocumentRepository,
    private val houseRepository: HouseRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<DocumentUiState>(DocumentUiState.Loading)
    val state: StateFlow<DocumentUiState> = _state.asStateFlow()

    private val _events = Channel<DocumentEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun load(houseId: String) {
        viewModelScope.launch {
            if (_state.value !is DocumentUiState.Ready) _state.value = DocumentUiState.Loading
            _state.value = fetch(houseId)
        }
    }

    private suspend fun fetch(houseId: String): DocumentUiState = coroutineScope {
        val house = async { documentRepository.getHouseDocuments(houseId) }
        val personal = async { documentRepository.getPersonalDocuments() }
        val members = async { houseRepository.getHouseMembers(houseId).getOrElse { emptyList() } }
        val houseResult = house.await()
        val personalResult = personal.await()
        val failure = houseResult.exceptionOrNull() ?: personalResult.exceptionOrNull()
        if (failure != null) return@coroutineScope DocumentUiState.Error(failure.userMessage())
        val viewerId = documentRepository.getCurrentUserId().orEmpty()
        val roster = members.await()
        DocumentUiState.Ready(
            house = houseResult.getOrThrow(),
            personal = personalResult.getOrThrow(),
            members = roster.associateBy { it.userId },
            viewerId = viewerId,
            isAdmin = roster.any { it.userId == viewerId && it.role in ADMIN_ROLES },
        )
    }

    /** Uploads the file at [uri] to the house when [toHouse], else to the viewer's personal documents. */
    fun upload(houseId: String, uri: Uri, toHouse: Boolean) {
        val ready = _state.value as? DocumentUiState.Ready ?: return
        if (ready.isUploading) return
        _state.value = ready.copy(isUploading = true)
        viewModelScope.launch {
            val uploaded = withContext(Dispatchers.IO) { runCatching { readPicked(uri) } }.fold(
                onSuccess = { documentRepository.uploadDocument(if (toHouse) houseId else null, it.name, it.bytes, it.mimeType) },
                onFailure = { Result.failure<Document>(it) },
            )
            uploaded.fold(
                onSuccess = { document ->
                    _state.update { current ->
                        if (current !is DocumentUiState.Ready) current
                        else if (toHouse) current.copy(house = listOf(document) + current.house, isUploading = false)
                        else current.copy(personal = listOf(document) + current.personal, isUploading = false)
                    }
                    _events.send(DocumentEvent.Show(Notice("Uploaded ${document.fileName}", isError = false)))
                },
                onFailure = { error ->
                    _state.update { (it as? DocumentUiState.Ready)?.copy(isUploading = false) ?: it }
                    _events.send(DocumentEvent.Show(Notice(error.userMessage(), isError = true)))
                },
            )
        }
    }

    fun delete(document: Document) {
        viewModelScope.launch {
            documentRepository.deleteDocument(document.id, document.storagePath, document.houseId).fold(
                onSuccess = {
                    _state.update { current ->
                        (current as? DocumentUiState.Ready)?.let { it.copy(house = it.house - document, personal = it.personal - document) } ?: current
                    }
                    _events.send(DocumentEvent.Show(Notice("Deleted ${document.fileName}", isError = false)))
                },
                onFailure = { _events.send(DocumentEvent.Show(Notice(it.userMessage(), isError = true))) },
            )
        }
    }

    fun open(document: Document) = withLink(document) { DocumentEvent.Open(it) }

    fun download(document: Document) = withLink(document) { DocumentEvent.Download(it, document.fileName, document.mimeType) }

    private fun withLink(document: Document, event: (String) -> DocumentEvent) {
        viewModelScope.launch {
            documentRepository.getDocumentUrl(document.storagePath, document.houseId).fold(
                onSuccess = { _events.send(event(it)) },
                onFailure = { _events.send(DocumentEvent.Show(Notice(it.userMessage(), isError = true))) },
            )
        }
    }

    /**
     * Reads the picked file, refusing a type the app does not store and a file over its size limit
     * before any of it is loaded into memory. A provider that misreports the size is caught by
     * reading at most one byte past the limit.
     */
    private fun readPicked(uri: Uri): PickedFile {
        val resolver = context.contentResolver
        val mimeType = Validators.validateMimeType(resolver.getType(uri).orEmpty()).getOrThrow()
        val maxSize = if (mimeType.startsWith("image/")) StorageRepository.MAX_IMAGE_SIZE_BYTES else StorageRepository.MAX_FILE_SIZE_BYTES
        var name: String? = null
        var size: Long? = null
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                name = cursor.getString(0)
                if (!cursor.isNull(1)) size = cursor.getLong(1)
            }
        }
        size?.let { if (it > maxSize) throw DomainError.StorageError.FileTooLarge(it, maxSize) }
        val bytes = resolver.openInputStream(uri)?.use { it.readNBytes(maxSize.toInt() + 1) }
            ?: throw DomainError.ValidationError.Rule("That file couldn't be read. Pick it again.")
        if (bytes.size > maxSize) throw DomainError.StorageError.FileTooLarge(bytes.size.toLong(), maxSize)
        return PickedFile(name?.takeIf { it.isNotBlank() } ?: FALLBACK_NAME, bytes, mimeType)
    }

    private class PickedFile(val name: String, val bytes: ByteArray, val mimeType: String)

    private companion object {
        val ADMIN_ROLES = setOf(HouseMemberRole.OWNER, HouseMemberRole.ADMIN)
        const val FALLBACK_NAME = "document"
    }
}
