package `in`.xroden.flockr.core.network

import `in`.xroden.flockr.core.constants.AppConstants
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Retry policy with exponential backoff and jitter for network operations.
 * Provides configurable retry logic with increasing delays and randomization.
 */
class RetryPolicy(
    private val maxAttempts: Int = AppConstants.RETRY_MAX_ATTEMPTS,
    private val initialDelayMs: Long = AppConstants.RETRY_INITIAL_DELAY_MS,
    private val maxDelayMs: Long = AppConstants.RETRY_MAX_DELAY_MS,
    private val backoffMultiplier: Double = 2.0
) {
    /**
     * Executes a block with retry logic.
     * Retries on failure with exponential backoff and jitter.
     */
    suspend fun <T> execute(block: suspend () -> T): Result<T> {
        var currentAttempt = 0
        var lastException: Exception? = null

        while (currentAttempt < maxAttempts) {
            try {
                return Result.success(block())
            } catch (e: Exception) {
                lastException = e
                currentAttempt++

                if (currentAttempt >= maxAttempts) {
                    break
                }

                val delayMs = calculateDelayWithJitter(currentAttempt)
                delay(delayMs)
            }
        }

        return Result.failure(lastException ?: Exception("Max retry attempts reached"))
    }

    private fun calculateDelayWithJitter(attempt: Int): Long {
        val exponentialDelay = (initialDelayMs * backoffMultiplier.pow(attempt - 1)).toLong()
        val jitter = Random.nextLong(0, exponentialDelay / 2) // Add 0-50% random jitter
        return min(exponentialDelay + jitter, maxDelayMs)
    }
}

/**
 * Extension function for easy retry with default policy.
 */
suspend fun <T> retryWithExponentialBackoff(
    maxAttempts: Int = AppConstants.RETRY_MAX_ATTEMPTS,
    block: suspend () -> T
): Result<T> {
    return RetryPolicy(maxAttempts = maxAttempts).execute(block)
}
