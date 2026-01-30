package `in`.xroden.flockr.core.network

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rate limiter for API requests to prevent abuse and excessive usage.
 * Implements token bucket algorithm for smooth rate limiting.
 */
@Singleton
class RateLimiter @Inject constructor() {

    private val limiters = mutableMapOf<String, TokenBucket>()
    private val mutex = Mutex()

    /**
     * Executes a block with rate limiting.
     * Suspends if rate limit is exceeded, resumes when tokens available.
     *
     * @param key Unique identifier for the rate limit (e.g., "create_expense", "upload_file")
     * @param maxRequestsPerMinute Maximum requests allowed per minute
     * @param block The operation to execute
     */
    suspend fun <T> throttle(
        key: String,
        maxRequestsPerMinute: Int = 60,
        block: suspend () -> T
    ): T {
        val bucket = mutex.withLock {
            limiters.getOrPut(key) {
                TokenBucket(maxRequestsPerMinute)
            }
        }

        bucket.acquire()
        return block()
    }

    /**
     * Resets rate limits for a specific key.
     * Useful for testing or manual reset scenarios.
     */
    suspend fun reset(key: String) {
        mutex.withLock {
            limiters.remove(key)
        }
    }

    /**
     * Resets all rate limits.
     */
    suspend fun resetAll() {
        mutex.withLock {
            limiters.clear()
        }
    }
}

/**
 * Token bucket implementation for rate limiting.
 */
private class TokenBucket(
    private val maxTokens: Int
) {
    private var tokens: Int = maxTokens
    private var lastRefill: Long = System.currentTimeMillis()
    private val refillIntervalMs = 60_000L // 1 minute
    private val mutex = Mutex()

    suspend fun acquire() {
        mutex.withLock {
            refillTokens()

            while (tokens <= 0) {
                // Wait until next refill
                val now = System.currentTimeMillis()
                val timeSinceLastRefill = now - lastRefill
                val timeUntilRefill = refillIntervalMs - timeSinceLastRefill

                if (timeUntilRefill > 0) {
                    kotlinx.coroutines.delay(timeUntilRefill)
                }

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

/**
 * Extension function for easy rate-limited execution.
 */
suspend fun <T> withRateLimit(
    key: String,
    maxRequestsPerMinute: Int = 60,
    rateLimiter: RateLimiter,
    block: suspend () -> T
): T {
    return rateLimiter.throttle(key, maxRequestsPerMinute, block)
}
