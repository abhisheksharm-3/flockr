/** App-wide preferences: the theme, haptic feedback and the app lock. */
package `in`.xroden.flockr.features.settings.presentation

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.core.managers.BiometricAuthManager
import `in`.xroden.flockr.features.settings.data.SettingsRepository
import `in`.xroden.flockr.features.settings.model.ThemeMode
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MS = 5_000L
private const val NO_DEVICE_LOCK_MESSAGE = "Set up a fingerprint, face or screen lock on this phone first."
private const val NO_PROMPT_MESSAGE = "Couldn't ask you to confirm. Try again."

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val biometricAuthManager: BiometricAuthManager,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> =
        settingsRepository.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ThemeMode.SYSTEM)
    val appLockEnabled: StateFlow<Boolean> =
        settingsRepository.appLockEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)
    val hapticsEnabled: StateFlow<Boolean> =
        settingsRepository.hapticsEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), true)

    private val _appLockEvents = Channel<AppLockEvent>(Channel.BUFFERED)
    val appLockEvents: Flow<AppLockEvent> = _appLockEvents.receiveAsFlow()

    fun canUseAppLock(): Boolean = biometricAuthManager.canAuthenticate()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAppLockEnabled(enabled)
        }
    }

    /**
     * Turns the lock on only once the user has unlocked the prompt, so no one can lock themselves
     * out of the app with a lock they can't open. A cancelled prompt reports nothing.
     */
    fun enableAppLock(activity: FragmentActivity?) {
        when {
            !biometricAuthManager.canAuthenticate() -> _appLockEvents.trySend(AppLockEvent.Failed(NO_DEVICE_LOCK_MESSAGE))
            activity == null -> _appLockEvents.trySend(AppLockEvent.Failed(NO_PROMPT_MESSAGE))
            else -> biometricAuthManager.authenticate(
                activity = activity,
                title = "Turn on app lock",
                subtitle = "Confirm it's you",
                onSuccess = {
                    setAppLockEnabled(true)
                    _appLockEvents.trySend(AppLockEvent.Enabled)
                },
                onError = { message -> _appLockEvents.trySend(AppLockEvent.Failed(message)) },
            )
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHapticsEnabled(enabled)
        }
    }
}
