/** Uploads public pictures (avatars and house photos) to Supabase Storage and hands back their URLs. */
package `in`.xroden.flockr.core.storage

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository @Inject constructor(
    private val supabase: SupabaseClient
) {

    /** Stores [data] at [path] in [bucket], replacing what is there, and returns its public URL. */
    suspend fun uploadFile(bucket: String, path: String, data: ByteArray): Result<String> = runCatching {
        require(data.size <= MAX_IMAGE_SIZE_BYTES) { "That picture is over ${MAX_IMAGE_SIZE_BYTES / (1024 * 1024)} MB" }
        val storageBucket = supabase.storage.from(bucket)
        storageBucket.upload(path, data) { upsert = true }
        storageBucket.publicUrl(path)
    }

    companion object {
        const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024L
        const val MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024L
    }
}
