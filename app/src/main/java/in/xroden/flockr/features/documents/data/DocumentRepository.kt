/** The documents vault: house and personal files in Supabase Storage, with a row each in `documents`. */
package `in`.xroden.flockr.features.documents.data

import `in`.xroden.flockr.core.domain.DomainError
import `in`.xroden.flockr.core.domain.requireAuthenticated
import android.util.Log
import `in`.xroden.flockr.core.security.InputSanitizer
import `in`.xroden.flockr.core.storage.StorageRepository
import `in`.xroden.flockr.core.validation.Validators
import `in`.xroden.flockr.features.documents.data.DocumentInsert
import `in`.xroden.flockr.features.documents.model.Document
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.storage.storage
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours

@Singleton
class DocumentRepository @Inject constructor(
    private val supabase: SupabaseClient,
) {

    private val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    fun getCurrentUserId(): String? = userId

    suspend fun getPersonalDocuments(): Result<List<Document>> = runCatching {
        val currentUserId = userId ?: return@runCatching emptyList()

        supabase.from("documents")
            .select(Columns.ALL) {
                filter {
                    eq("user_id", currentUserId)
                    filter("house_id", FilterOperator.IS, null)
                }
                order("created_at", Order.DESCENDING)
                limit(count = 200)
            }
            .decodeList<Document>()
    }

    suspend fun getHouseDocuments(houseId: String): Result<List<Document>> = runCatching {
        Validators.validateUUID(houseId).getOrThrow()

        supabase.from("documents")
            .select(Columns.ALL) {
                filter { eq("house_id", houseId) }
                order("created_at", Order.DESCENDING)
                limit(count = 200)
            }
            .decodeList<Document>()
    }

    /**
     * Stores [fileData] and records it, for [houseId] or as a personal document when it is null. Refuses
     * an empty file, an unsupported type, one over the size limit, or one past the house's or the
     * person's document count.
     */
    suspend fun uploadDocument(houseId: String?, fileName: String, fileData: ByteArray, mimeType: String): Result<Document> = runCatching {
        val currentUserId = requireAuthenticated(userId)
        val name = InputSanitizer.sanitizeFileName(fileName)
        if (name.isBlank()) throw DomainError.ValidationError.EmptyField("File name")
        if (fileData.isEmpty()) throw DomainError.ValidationError.Rule("That file is empty")
        Validators.validateMimeType(mimeType).getOrThrow()
        val maxSize = if (mimeType.startsWith("image/")) StorageRepository.MAX_IMAGE_SIZE_BYTES else StorageRepository.MAX_FILE_SIZE_BYTES
        Validators.validateFileSize(fileData.size.toLong(), maxSize).getOrThrow()
        if (houseId != null) {
            if (getHouseDocuments(houseId).getOrThrow().size >= MAX_HOUSE_DOCUMENTS) throw DomainError.StorageError.LimitReached("House", MAX_HOUSE_DOCUMENTS)
        } else if (getPersonalDocuments().getOrThrow().size >= MAX_PERSONAL_DOCUMENTS) {
            throw DomainError.StorageError.LimitReached("Personal", MAX_PERSONAL_DOCUMENTS)
        }

        val path = listOfNotNull(houseId, currentUserId, "${System.currentTimeMillis()}_$name").joinToString("/")
        supabase.storage.from(bucketFor(houseId)).upload(path, fileData) { upsert = false }
        supabase.from("documents")
            .insert(DocumentInsert(houseId, currentUserId, path, name, fileData.size.toLong(), mimeType)) { select() }
            .decodeSingle<Document>()
    }

    /**
     * Deletes the record first and the file second, so a refused delete leaves both in place. A file
     * the caller may not remove, such as another member's that an admin deleted, is left in storage.
     */
    suspend fun deleteDocument(documentId: String, storagePath: String, houseId: String?): Result<Unit> = runCatching {
        val deleted = supabase.from("documents").delete {
            filter { eq("id", documentId) }
            select()
        }.decodeList<Document>()
        if (deleted.isEmpty()) throw DomainError.ValidationError.Rule("Only whoever added it, or an admin, can delete this document")
        runCatching { supabase.storage.from(bucketFor(houseId)).delete(storagePath) }
            .onFailure { Log.w("Flockr:DocumentRepository", "Left the file in storage: $storagePath", it) }
    }

    suspend fun getDocumentUrl(storagePath: String, houseId: String?): Result<String> = runCatching {
        supabase.storage.from(bucketFor(houseId)).createSignedUrl(storagePath, 1.hours)
    }

    private fun bucketFor(houseId: String?) = if (houseId != null) "house-documents" else "personal-documents"

    companion object {
        const val MAX_HOUSE_DOCUMENTS = 3
        const val MAX_PERSONAL_DOCUMENTS = 2
    }
}
