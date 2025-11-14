package `in`.xroden.flockr.features.settings.domain

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.settings.model.ThemeMode
import `in`.xroden.flockr.features.settings.model.ThemePreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themePreferences: ThemePreferences
) : ViewModel() {

    val themeMode: Flow<ThemeMode> = themePreferences.themeMode

    suspend fun setThemeMode(mode: ThemeMode) {
        themePreferences.setThemeMode(mode)
    }
}

