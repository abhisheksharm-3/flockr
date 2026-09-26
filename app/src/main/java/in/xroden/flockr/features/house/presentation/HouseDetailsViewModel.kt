/** A house's home: the house and its people, where the viewer stands, what is coming up, and who is sharing where they are. */
package `in`.xroden.flockr.features.house.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.features.house.data.HouseRepository
import `in`.xroden.flockr.features.location.data.LocationSharingRepository
import `in`.xroden.flockr.features.location.model.MemberLocation
import `in`.xroden.flockr.features.location.system.LocationSharingStatus
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class HouseDetailsViewModel @Inject constructor(
    private val houseRepository: HouseRepository,
    private val digestLoader: HouseDigestLoader,
    private val locationRepository: LocationSharingRepository,
    private val sharingStatus: LocationSharingStatus,
) : ViewModel() {

    private val _state = MutableStateFlow<HouseDetailUiState>(HouseDetailUiState.Loading)
    val state: StateFlow<HouseDetailUiState> = _state.asStateFlow()

    private val _locations = MutableStateFlow<List<MemberLocation>>(emptyList())

    /** Everyone sharing with this house right now, the viewer included, live. Empty when it can't be read. */
    val locations: StateFlow<List<MemberLocation>> = _locations.asStateFlow()

    /** This phone's own sharing: under way, running, or why it failed. */
    val sharing: StateFlow<LocationSharingStatus.State> = sharingStatus.state

    private var locationsJob: Job? = null

    /** Only the house itself is required; the digest degrades part by part. */
    fun load(houseId: String) {
        viewModelScope.launch {
            if (_state.value !is HouseDetailUiState.Ready) _state.value = HouseDetailUiState.Loading
            val viewerId = houseRepository.getCurrentUserId().orEmpty()
            val digest = async { digestLoader.load(houseId, viewerId, forHub = true) }
            _state.value = houseRepository.getHouseById(houseId).fold(
                onSuccess = { house -> HouseDetailUiState.Ready(house, viewerId, digest.await()) },
                onFailure = { HouseDetailUiState.Error(it.userMessage()) },
            )
        }
        locationsJob?.cancel()
        locationsJob = viewModelScope.launch {
            locationRepository.getLocationsFlow(houseId).collect { result -> result.onSuccess { _locations.value = it } }
        }
    }

    /** Clears a sharing failure once the screen has told the viewer about it. */
    fun acknowledgeSharingError() = sharingStatus.acknowledge()
}
