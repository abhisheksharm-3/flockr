/** The last good result of each live query, kept on disk so a screen opens on it without a network. */
package `in`.xroden.flockr.core.realtime

import kotlinx.coroutines.CancellationException
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/** Where a live query keeps its result: a file named after [key], written and read with [serializer]. */
class CachedAs<T>(val key: String, val serializer: KSerializer<T>)

/** Caches a live query's result under [key], which must name everything the query depends on. */
inline fun <reified T> cachedAs(key: String): CachedAs<T> = CachedAs(key, serializer<T>())

/**
 * One file per query in the app's cache directory. It holds the signed-in user's data, so it is
 * cleared on sign-out and account deletion; Android may also clear it when storage runs low, which
 * only costs a slower first open.
 */
object OfflineCache {
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var dir: File

    /** Call once from `Application.onCreate`. */
    fun init(cacheDir: File) {
        dir = cacheDir.resolve("offline").apply { mkdirs() }
    }

    suspend fun <T> read(entry: CachedAs<T>): T? = withContext(Dispatchers.IO) {
        runCatching { json.decodeFromString(entry.serializer, fileOf(entry.key).readText()) }.getOrNull()
    }

    /** Writes beside the old file and renames over it, so a reader never sees half a result. */
    suspend fun <T> write(entry: CachedAs<T>, value: T) = withContext(Dispatchers.IO) {
        runCatching {
            val target = fileOf(entry.key)
            val partial = File(dir, "${target.name}.${Thread.currentThread().id}.tmp")
            partial.writeText(json.encodeToString(entry.serializer, value))
            partial.renameTo(target)
        }
    }

    /** [fetch]'s result, saved for next time; without a network, the one saved last time, if any. */
    suspend fun <T> fetchOrSaved(entry: CachedAs<T>, fetch: suspend () -> T): T =
        runCatching { fetch() }
            .onSuccess { write(entry, it) }
            .getOrElse { error -> if (error is CancellationException) throw error else read(entry) ?: throw error }

    suspend fun clear() = withContext(Dispatchers.IO) {
        dir.listFiles()?.forEach { it.delete() }
    }

    private fun fileOf(key: String) = File(dir, key.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".json")
}
