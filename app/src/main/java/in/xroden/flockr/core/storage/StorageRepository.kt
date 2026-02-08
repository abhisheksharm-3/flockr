package `in`.xroden.flockr.core.storage

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Storage repository implementation using Supabase Storage. */
@Singleton
class StorageRepository @Inject constructor(
    private val supabase: SupabaseClient
) : IStorageRepository {

    override suspend fun uploadFile(bucket: String, path: String, data: ByteArray): Result<String> = 
        runCatching {
            val maxSize = if (bucket.contains("image") || bucket.contains("header")) {
                IStorageRepository.MAX_IMAGE_SIZE_BYTES
            } else {
                IStorageRepository.MAX_FILE_SIZE_BYTES
            }

            require(data.size <= maxSize) {
                "File size ${data.size} exceeds maximum allowed size of $maxSize bytes"
            }

            withContext(Dispatchers.IO) {
                val storageBucket = supabase.storage.from(bucket)
                storageBucket.upload(path, data) { upsert = true }
                storageBucket.publicUrl(path)
            }
        }

    override suspend fun deleteFile(bucket: String, path: String): Result<Unit> = 
        runCatching {
            withContext(Dispatchers.IO) {
                supabase.storage.from(bucket).delete(path)
            }
        }
}
