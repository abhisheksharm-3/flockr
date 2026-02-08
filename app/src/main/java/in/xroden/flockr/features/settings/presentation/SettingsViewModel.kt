package `in`.xroden.flockr.features.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.settings.data.ISettingsRepository
import `in`.xroden.flockr.features.settings.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ViewModel for managing app settings. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: ISettingsRepository
) : ViewModel() {

    val themeMode: Flow<ThemeMode> = settingsRepository.themeMode
    val appLockEnabled: Flow<Boolean> = settingsRepository.appLockEnabled
    val hapticsEnabled: Flow<Boolean> = settingsRepository.hapticsEnabled

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

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHapticsEnabled(enabled)
        }
    }
}
