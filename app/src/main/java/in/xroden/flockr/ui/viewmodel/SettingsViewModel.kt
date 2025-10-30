package `in`.xroden.flockr.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.data.preferences.ThemeMode
import `in`.xroden.flockr.data.preferences.ThemePreferences
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

