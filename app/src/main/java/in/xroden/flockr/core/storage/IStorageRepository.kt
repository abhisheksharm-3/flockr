package `in`.xroden.flockr.core.storage

/** Interface for file storage operations. */
interface IStorageRepository {
    
    /** Uploads a file to the specified bucket. Returns the public URL. */
    suspend fun uploadFile(bucket: String, path: String, data: ByteArray): Result<String>
    
    /** Deletes a file from the specified bucket. */
    suspend fun deleteFile(bucket: String, path: String): Result<Unit>

    companion object {
        const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L // 10MB
        const val MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024L // 5MB
    }
}
