package `in`.xroden.flockr.features.settings.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.settings.model.HapticsPreferences
import `in`.xroden.flockr.features.settings.model.SecurityPreferences
import `in`.xroden.flockr.features.settings.model.ThemeMode
import `in`.xroden.flockr.features.settings.model.ThemePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themePreferences: ThemePreferences,
    private val securityPreferences: SecurityPreferences,
    private val hapticsPreferences: HapticsPreferences
) : ViewModel() {

    val themeMode: Flow<ThemeMode> = themePreferences.themeMode
    val appLockEnabled: Flow<Boolean> = securityPreferences.appLockEnabled
    val hapticsEnabled: Flow<Boolean> = hapticsPreferences.hapticsEnabled

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePreferences.setThemeMode(mode)
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            securityPreferences.setAppLockEnabled(enabled)
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            hapticsPreferences.setHapticsEnabled(enabled)
        }
    }
}
