package `in`.xroden.flockr.core.network

import `in`.xroden.flockr.core.constants.AppConstants
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/** Retry policy with exponential backoff and jitter for network operations. */
class RetryPolicy(
    private val maxAttempts: Int = AppConstants.RETRY_MAX_ATTEMPTS,
    private val initialDelayMs: Long = AppConstants.RETRY_INITIAL_DELAY_MS,
    private val maxDelayMs: Long = AppConstants.RETRY_MAX_DELAY_MS,
    private val backoffMultiplier: Double = 2.0,
    private val isRetryable: (Throwable) -> Boolean = ::defaultIsRetryable
) {
    /** Executes a block with retry logic using exponential backoff. */
    suspend fun <T> execute(block: suspend () -> T): Result<T> {
        var currentAttempt = 0
        var lastException: Throwable? = null

        while (currentAttempt < maxAttempts) {
            try {
                return Result.success(block())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                lastException = e
                currentAttempt++

                if (!isRetryable(e) || currentAttempt >= maxAttempts) break

                val delayMs = calculateDelayWithJitter(currentAttempt)
                delay(delayMs)
            }
        }

        return Result.failure(lastException ?: Exception("Max retry attempts reached"))
    }

    private fun calculateDelayWithJitter(attempt: Int): Long {
        val exponentialDelay = min(
            initialDelayMs * backoffMultiplier.pow(attempt - 1),
            maxDelayMs.toDouble()
        ).toLong()
        val jitter = if (exponentialDelay > 1) Random.nextLong(0, exponentialDelay / 2) else 0L
        return min(exponentialDelay + jitter, maxDelayMs)
    }

    companion object {
        /** Retries transient failures but not client errors (HTTP 4xx) or cancellation. */
        fun defaultIsRetryable(error: Throwable): Boolean = when {
            error is CancellationException -> false
            error is RestException && error.statusCode in 400..499 -> false
            else -> true
        }
    }
}

suspend fun <T> retryWithExponentialBackoff(
    maxAttempts: Int = AppConstants.RETRY_MAX_ATTEMPTS,
    block: suspend () -> T
): Result<T> = RetryPolicy(maxAttempts = maxAttempts).execute(block)
