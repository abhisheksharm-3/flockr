package `in`.xroden.flockr.features.settings.model

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val APP_LOCK_KEY = booleanPreferencesKey("app_lock_enabled")

    val appLockEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[APP_LOCK_KEY] ?: false
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[APP_LOCK_KEY] = enabled
        }
    }
}
