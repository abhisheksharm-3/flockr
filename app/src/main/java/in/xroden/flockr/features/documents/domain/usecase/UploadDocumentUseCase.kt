package `in`.xroden.flockr.features.documents.domain.usecase

import `in`.xroden.flockr.core.domain.DomainError
import `in`.xroden.flockr.core.storage.IStorageRepository
import `in`.xroden.flockr.features.documents.data.IDocumentRepository
import `in`.xroden.flockr.features.documents.model.Document
import javax.inject.Inject

/** Use case for uploading documents with validation and limit enforcement. */
class UploadDocumentUseCase @Inject constructor(
    private val documentRepository: IDocumentRepository
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
            return Result.failure(IllegalArgumentException("File name cannot be empty"))
        }

        if (fileData.isEmpty()) {
            return Result.failure(IllegalArgumentException("File data cannot be empty"))
        }

        val maxSize = if (mimeType.startsWith("image/")) {
            IStorageRepository.MAX_IMAGE_SIZE_BYTES
        } else {
            IStorageRepository.MAX_FILE_SIZE_BYTES
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
                    DomainError.StorageError.LimitReached("House document", MAX_HOUSE_DOCUMENTS)
                )
            }
        } else {
            val personalDocs = documentRepository.getPersonalDocuments().getOrDefault(emptyList())
            if (personalDocs.size >= MAX_PERSONAL_DOCUMENTS) {
                return Result.failure(
                    DomainError.StorageError.LimitReached("Personal document", MAX_PERSONAL_DOCUMENTS)
                )
            }
        }

        return documentRepository.uploadDocument(houseId, fileName, fileData, mimeType)
    }
}
