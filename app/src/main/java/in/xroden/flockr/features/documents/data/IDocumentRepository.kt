package `in`.xroden.flockr.features.documents.data

import `in`.xroden.flockr.features.documents.model.Document

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
