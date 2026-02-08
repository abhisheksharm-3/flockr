package `in`.xroden.flockr.core.managers

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.core.DataStore
import androidx.fragment.app.FragmentActivity
import `in`.xroden.flockr.core.constants.AppConstants
import `in`.xroden.flockr.core.managers.BiometricAuthManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages app lock state and biometric authentication.
 * Handles lock timeout, authentication flow, and lock state persistence.
 */
@Singleton
class AppLockManager @Inject constructor(
    private val biometricAuthManager: BiometricAuthManager,
    private val dataStore: DataStore<Preferences>
) {
    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private var lastBackgroundTimestamp: Long = 0L

    val appLockEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[booleanPreferencesKey("app_lock_enabled")] ?: false
    }

    /**
     * Called when app goes to background (onStop).
     * Records timestamp for lock timeout calculation.
     */
    fun onAppBackgrounded() {
        lastBackgroundTimestamp = System.currentTimeMillis()
    }

    /**
     * Called when app comes to foreground (onResume).
     * Checks if lock timeout has expired and triggers authentication if needed.
     *
     * @param onAuthRequired Called when authentication is required
     */
    suspend fun onAppForegrounded(onAuthRequired: () -> Unit) {
        if (lastBackgroundTimestamp == 0L) return

        val diff = System.currentTimeMillis() - lastBackgroundTimestamp
        if (diff < AppConstants.APP_LOCK_TIMEOUT_MS) return

        val enabled = appLockEnabled.firstOrNull() ?: false
        if (enabled) {
            _isAppLocked.value = true
            onAuthRequired()
        } else {
            lastBackgroundTimestamp = 0
        }
    }

    /**
     * Triggers biometric authentication.
     *
     * @param activity The activity to show the biometric prompt
     * @param onSuccess Called when authentication succeeds
     * @param onError Called when authentication fails
     */
    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        biometricAuthManager.authenticate(
            activity = activity,
            title = "Flockr Locked",
            subtitle = "Verify your identity to access Flockr",
            onSuccess = {
                _isAppLocked.value = false
                lastBackgroundTimestamp = 0
                onSuccess()
            },
            onError = onError
        )
    }

    /**
     * Forces lock check on cold start.
     * Should be called when savedInstanceState is null in onCreate.
     */
    fun initializeColdStartLock() {
        lastBackgroundTimestamp = 1L
    }

    fun canAuthenticate(): Boolean = biometricAuthManager.canAuthenticate()
}
