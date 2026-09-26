/** A house's home: the house and its people, where the viewer stands, and what is coming up. */
package `in`.xroden.flockr.features.house.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.features.house.data.HouseRepository
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class HouseDetailsViewModel @Inject constructor(
    private val houseRepository: HouseRepository,
    private val digestLoader: HouseDigestLoader,
) : ViewModel() {

    private val _state = MutableStateFlow<HouseDetailUiState>(HouseDetailUiState.Loading)
    val state: StateFlow<HouseDetailUiState> = _state.asStateFlow()

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
    }
}
