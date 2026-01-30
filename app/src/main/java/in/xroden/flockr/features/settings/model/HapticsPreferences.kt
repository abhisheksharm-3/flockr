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
class HapticsPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val HAPTICS_ENABLED_KEY = booleanPreferencesKey("haptics_enabled")

    val hapticsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[HAPTICS_ENABLED_KEY] ?: true // Enabled by default
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[HAPTICS_ENABLED_KEY] = enabled
        }
    }
}

