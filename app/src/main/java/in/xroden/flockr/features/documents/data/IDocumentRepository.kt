package `in`.xroden.flockr.features.documents.data

import `in`.xroden.flockr.features.documents.model.Document

/**
 * Repository interface for document operations.
 * Enables easy mocking for unit tests.
 */
interface IDocumentRepository {
    suspend fun getPersonalDocuments(): Result<List<Document>>
    suspend fun getHouseDocuments(houseId: String): Result<List<Document>>
    suspend fun uploadDocument(
        houseId: String?,
        fileName: String,
        fileData: ByteArray,
        mimeType: String
    ): Result<Document>
    suspend fun deleteDocument(documentId: String, storagePath: String, houseId: String?): Result<Unit>
    suspend fun getDocumentUrl(storagePath: String, houseId: String?): Result<String>
    suspend fun downloadDocument(storagePath: String, houseId: String?): Result<ByteArray>
    fun getCurrentUserId(): String?
}
