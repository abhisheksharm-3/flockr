package `in`.xroden.flockr.features.settings.data

import `in`.xroden.flockr.features.settings.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/** Repository interface for app settings. */
interface ISettingsRepository {
    val themeMode: Flow<ThemeMode>
    val appLockEnabled: Flow<Boolean>
    val hapticsEnabled: Flow<Boolean>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setAppLockEnabled(enabled: Boolean)
    suspend fun setHapticsEnabled(enabled: Boolean)
}
