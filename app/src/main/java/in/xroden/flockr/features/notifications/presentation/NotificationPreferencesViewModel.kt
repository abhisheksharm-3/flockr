/** Which kinds of notification the member wants from each of their houses. */
package `in`.xroden.flockr.features.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.features.house.data.HouseRepository
import `in`.xroden.flockr.features.house.model.HouseCardData
import `in`.xroden.flockr.features.notifications.data.NotificationRepository
import `in`.xroden.flockr.features.notifications.model.NotificationType
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One house with each kind's switch. */
data class HousePreferences(val house: HouseCardData, val enabled: Map<NotificationType, Boolean>)

sealed interface NotificationPreferencesUiState {
    data object Loading : NotificationPreferencesUiState
    data class Error(val message: String) : NotificationPreferencesUiState
    data class Ready(val houses: List<HousePreferences>, val error: String? = null) : NotificationPreferencesUiState
}

@HiltViewModel
class NotificationPreferencesViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val houseRepository: HouseRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<NotificationPreferencesUiState>(NotificationPreferencesUiState.Loading)
    val state: StateFlow<NotificationPreferencesUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = runCatching {
                coroutineScope {
                    houseRepository.getHouses().getOrThrow()
                        .map { house -> async { HousePreferences(house, repository.getPreferences(house.id).getOrThrow()) } }
                        .awaitAll()
                }
            }.fold(
                onSuccess = { NotificationPreferencesUiState.Ready(it) },
                onFailure = { NotificationPreferencesUiState.Error(it.userMessage()) },
            )
        }
    }

    /** Flips the switch at once and puts it back if the save fails. */
    fun set(houseId: String, type: NotificationType, isEnabled: Boolean) {
        setLocally(houseId, type, isEnabled)
        viewModelScope.launch {
            repository.setPreference(houseId, type, isEnabled).onFailure { error ->
                setLocally(houseId, type, !isEnabled)
                _state.update { (it as? NotificationPreferencesUiState.Ready)?.copy(error = error.userMessage()) ?: it }
            }
        }
    }

    fun dismissError() = _state.update { (it as? NotificationPreferencesUiState.Ready)?.copy(error = null) ?: it }

    private fun setLocally(houseId: String, type: NotificationType, isEnabled: Boolean) = _state.update { state ->
        val ready = state as? NotificationPreferencesUiState.Ready ?: return@update state
        ready.copy(houses = ready.houses.map { if (it.house.id == houseId) it.copy(enabled = it.enabled + (type to isEnabled)) else it })
    }
}
