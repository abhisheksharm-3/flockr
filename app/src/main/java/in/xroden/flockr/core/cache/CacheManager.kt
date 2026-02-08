package `in`.xroden.flockr.core.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** LRU cache with TTL support using LinkedHashMap for O(1) eviction. Thread-safe via mutex. */
@Singleton
class CacheManager @Inject constructor() {

    private data class CacheEntry<T>(
        val value: T,
        val expiresAt: Long
    )

    private val cache = object : LinkedHashMap<String, CacheEntry<*>>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry<*>>): Boolean {
            return size > MAX_ENTRIES
        }
    }

    private val mutex = Mutex()

    companion object {
        private const val MAX_ENTRIES = 100
    }

    /** Retrieves a cached value if it exists and hasn't expired. */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T> get(key: String): T? = mutex.withLock {
        val entry = cache[key] as? CacheEntry<T> ?: return@withLock null
        if (System.currentTimeMillis() > entry.expiresAt) {
            cache.remove(key)
            return@withLock null
        }
        entry.value
    }

    /** Stores a value in cache with specified TTL. */
    suspend fun <T> put(key: String, value: T, ttlMs: Long): Unit = mutex.withLock {
        evictExpired()
        cache[key] = CacheEntry(value, System.currentTimeMillis() + ttlMs)
    }

    /** Retrieves cached value or fetches and caches it if not present. */
    suspend fun <T> getOrPut(key: String, ttlMs: Long, fetch: suspend () -> T): T {
        get<T>(key)?.let { return it }
        val value = fetch()
        put(key, value, ttlMs)
        return value
    }

    /** Invalidates a specific cache entry. */
    suspend fun invalidate(key: String): Unit = mutex.withLock {
        cache.remove(key)
    }

    /** Invalidates all entries matching a prefix. */
    suspend fun invalidateByPrefix(prefix: String): Unit = mutex.withLock {
        val keysToRemove = cache.keys.filter { it.startsWith(prefix) }
        keysToRemove.forEach { cache.remove(it) }
    }

    /** Clears all cached entries. */
    suspend fun clear(): Unit = mutex.withLock {
        cache.clear()
    }

    private fun evictExpired() {
        val now = System.currentTimeMillis()
        val expiredKeys = cache.entries
            .filter { it.value.expiresAt < now }
            .map { it.key }
        expiredKeys.forEach { cache.remove(it) }
    }
}
