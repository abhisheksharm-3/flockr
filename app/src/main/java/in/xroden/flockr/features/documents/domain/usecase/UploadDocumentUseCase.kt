/** Uploads a document once it passes the size, type and per-house or per-person count limits. */
package `in`.xroden.flockr.features.documents.domain.usecase

import `in`.xroden.flockr.core.domain.DomainError
import `in`.xroden.flockr.core.storage.StorageRepository
import `in`.xroden.flockr.features.documents.data.DocumentRepository
import `in`.xroden.flockr.features.documents.model.Document
import javax.inject.Inject

class UploadDocumentUseCase @Inject constructor(
    private val documentRepository: DocumentRepository
) {
    companion object {
        const val MAX_HOUSE_DOCUMENTS = 3
        const val MAX_PERSONAL_DOCUMENTS = 2
    }

    suspend operator fun invoke(
        houseId: String?,
        fileName: String,
        fileData: ByteArray,
        mimeType: String
    ): Result<Document> {
        if (fileName.isBlank()) {
            return Result.failure(DomainError.ValidationError.EmptyField("File name"))
        }

        if (fileData.isEmpty()) {
            return Result.failure(DomainError.ValidationError.Rule("That file is empty"))
        }

        val maxSize = if (mimeType.startsWith("image/")) {
            StorageRepository.MAX_IMAGE_SIZE_BYTES
        } else {
            StorageRepository.MAX_FILE_SIZE_BYTES
        }

        if (fileData.size > maxSize) {
            return Result.failure(
                DomainError.StorageError.FileTooLarge(fileData.size.toLong(), maxSize)
            )
        }

        if (houseId != null) {
            val houseDocs = documentRepository.getHouseDocuments(houseId).getOrDefault(emptyList())
            if (houseDocs.size >= MAX_HOUSE_DOCUMENTS) {
                return Result.failure(
                    DomainError.StorageError.LimitReached("House", MAX_HOUSE_DOCUMENTS)
                )
            }
        } else {
            val personalDocs = documentRepository.getPersonalDocuments().getOrDefault(emptyList())
            if (personalDocs.size >= MAX_PERSONAL_DOCUMENTS) {
                return Result.failure(
                    DomainError.StorageError.LimitReached("Personal", MAX_PERSONAL_DOCUMENTS)
                )
            }
        }

        return documentRepository.uploadDocument(houseId, fileName, fileData, mimeType)
    }
}
