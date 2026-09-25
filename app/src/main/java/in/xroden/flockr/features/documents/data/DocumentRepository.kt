package `in`.xroden.flockr.features.documents.data

import `in`.xroden.flockr.core.domain.DomainError
import `in`.xroden.flockr.core.domain.requireAuthenticated
import `in`.xroden.flockr.core.logging.Logger
import `in`.xroden.flockr.core.security.InputSanitizer
import `in`.xroden.flockr.core.storage.StorageRepository
import `in`.xroden.flockr.core.validation.Validators
import `in`.xroden.flockr.data.dto.DocumentInsert
import `in`.xroden.flockr.features.documents.model.Document
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.storage.storage
import javax.inject.Inject
import javax.inject.Singleton

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
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(count = 200)
            }
            .decodeList<Document>()
    }

    suspend fun getHouseDocuments(houseId: String): Result<List<Document>> = runCatching {
        Validators.validateUUID(houseId).getOrThrow()

        supabase.from("documents")
            .select(Columns.ALL) {
                filter { eq("house_id", houseId) }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(count = 200)
            }
            .decodeList<Document>()
    }

    suspend fun uploadDocument(
        houseId: String?,
        fileName: String,
        fileData: ByteArray,
        mimeType: String
    ): Result<Document> = runCatching {
            val currentUserId = requireAuthenticated(userId)
            val sanitizedFileName = InputSanitizer.sanitizeFileName(fileName)

            Validators.validateMimeType(mimeType).getOrThrow()
            Validators.validateFileSize(fileData.size.toLong(), StorageRepository.MAX_FILE_SIZE_BYTES).getOrThrow()
            if (houseId != null) Validators.validateUUID(houseId).getOrThrow()

            val bucket = if (houseId != null) "house-documents" else "personal-documents"
            val path = if (houseId != null) {
                "$houseId/$currentUserId/${System.currentTimeMillis()}_$sanitizedFileName"
            } else {
                "$currentUserId/${System.currentTimeMillis()}_$sanitizedFileName"
            }

            supabase.storage.from(bucket).upload(path, fileData) { upsert = false }

            val document = supabase.from("documents")
                .insert(DocumentInsert(
                    houseId = houseId,
                    userId = currentUserId,
                    storagePath = path,
                    fileName = sanitizedFileName,
                    fileSize = fileData.size.toLong(),
                    mimeType = mimeType
                )) { select() }
                .decodeSingle<Document>()

            document
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
        val bucket = if (houseId != null) "house-documents" else "personal-documents"
        runCatching { supabase.storage.from(bucket).delete(storagePath) }
            .onFailure { Logger.w("DocumentRepository", "Left the file in storage: $storagePath", it) }
    }

    suspend fun getDocumentUrl(storagePath: String, houseId: String?): Result<String> = runCatching {
        val bucket = if (houseId != null) "house-documents" else "personal-documents"
        supabase.storage.from(bucket).createSignedUrl(storagePath, kotlin.time.Duration.parse("PT1H"))
    }

    suspend fun downloadDocument(storagePath: String, houseId: String?): Result<ByteArray> = runCatching {
        val bucket = if (houseId != null) "house-documents" else "personal-documents"
        supabase.storage.from(bucket).downloadAuthenticated(storagePath)
    }
}
