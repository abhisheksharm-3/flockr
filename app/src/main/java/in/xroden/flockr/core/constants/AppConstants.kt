package `in`.xroden.flockr.core.constants

object AppConstants {
    const val APP_LOCK_TIMEOUT_MS = 60_000L
    const val REALTIME_DEBOUNCE_MS = 150L // Reduced for snappier updates
    const val NETWORK_TIMEOUT_MS = 30_000L
    const val RETRY_MAX_ATTEMPTS = 3
    const val RETRY_INITIAL_DELAY_MS = 500L // Reduced initial delay
    const val RETRY_MAX_DELAY_MS = 5_000L // Reduced max delay
}
