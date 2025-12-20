package `in`.xroden.flockr.features.common.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class StorageRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    companion object {
        private const val BUCKET_AVATARS = "avatars"
    }

    suspend fun uploadFile(path: String, data: ByteArray): String = withContext(Dispatchers.IO) {
        val bucket = supabase.storage.from(BUCKET_AVATARS)
        bucket.upload(path, data) { upsert = true }
        bucket.publicUrl(path)
    }

    suspend fun deleteFile(path: String) = withContext(Dispatchers.IO) {
        val bucket = supabase.storage.from(BUCKET_AVATARS)
        bucket.delete(path)
    }
}
