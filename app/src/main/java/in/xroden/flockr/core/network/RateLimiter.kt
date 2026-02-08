package `in`.xroden.flockr.core.network

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Rate limiter using token bucket algorithm. */
@Singleton
class RateLimiter @Inject constructor() {

    private data class TokenBucket(private val maxTokens: Int) {
        private var tokens: Int = maxTokens
        private var lastRefill: Long = System.currentTimeMillis()
        private val refillIntervalMs = 60_000L
        private val mutex = Mutex()

        suspend fun acquire() {
            mutex.withLock {
                refillTokens()
                while (tokens <= 0) {
                    val timeUntilRefill = refillIntervalMs - (System.currentTimeMillis() - lastRefill)
                    if (timeUntilRefill > 0) kotlinx.coroutines.delay(timeUntilRefill)
                    refillTokens()
                }
                tokens--
            }
        }

        private fun refillTokens() {
            val now = System.currentTimeMillis()
            val timeSinceLastRefill = now - lastRefill
            if (timeSinceLastRefill >= refillIntervalMs) {
                val intervalsElapsed = timeSinceLastRefill / refillIntervalMs
                tokens = minOf(maxTokens, tokens + (maxTokens * intervalsElapsed).toInt())
                lastRefill = now
            }
        }
    }

    private val limiters = mutableMapOf<String, TokenBucket>()
    private val mutex = Mutex()

    /** Executes a block with rate limiting. Suspends if rate limit exceeded. */
    suspend fun <T> throttle(
        key: String,
        maxRequestsPerMinute: Int = 60,
        block: suspend () -> T
    ): T {
        val bucket = mutex.withLock {
            limiters.getOrPut(key) { TokenBucket(maxRequestsPerMinute) }
        }
        bucket.acquire()
        return block()
    }

    suspend fun reset(key: String) { mutex.withLock { limiters.remove(key) } }

    suspend fun resetAll() { mutex.withLock { limiters.clear() } }
}

suspend fun <T> withRateLimit(
    key: String,
    maxRequestsPerMinute: Int = 60,
    rateLimiter: RateLimiter,
    block: suspend () -> T
): T = rateLimiter.throttle(key, maxRequestsPerMinute, block)
