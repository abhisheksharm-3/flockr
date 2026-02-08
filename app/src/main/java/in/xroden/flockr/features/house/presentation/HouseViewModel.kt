package `in`.xroden.flockr.features.house.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.house.data.IHouseRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for house detail screen.
 * Fetches house data, config, and members in parallel.
 */
@HiltViewModel
class HouseViewModel @Inject constructor(
    private val houseRepository: IHouseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HouseDetailUiState>(HouseDetailUiState.Loading)
    val uiState: StateFlow<HouseDetailUiState> = _uiState.asStateFlow()

    /**
     * Loads house details with parallel data fetching for improved performance.
     * @param houseId The ID of the house to load.
     */
    fun loadHouseDetails(houseId: String) {
        viewModelScope.launch {
            _uiState.value = HouseDetailUiState.Loading
            
            // Fetch all data in parallel for better performance
            val houseDeferred = async { houseRepository.getHouseById(houseId) }
            val configDeferred = async { houseRepository.getHouseConfig(houseId) }
            val membersDeferred = async { houseRepository.getHouseMembers(houseId) }
            
            val houseResult = houseDeferred.await()
            val configResult = configDeferred.await()
            val membersResult = membersDeferred.await()
            
            if (houseResult.isSuccess) {
                val house = houseResult.getOrNull()
                if (house != null) {
                    _uiState.value = HouseDetailUiState.Success(
                        house = house,
                        config = configResult.getOrNull(),
                        members = membersResult.getOrElse { emptyList() }
                    )
                } else {
                    _uiState.value = HouseDetailUiState.Error("House not found", null)
                }
            } else {
                _uiState.value = HouseDetailUiState.Error(
                    message = houseResult.exceptionOrNull()?.message ?: "Failed to load house",
                    cause = houseResult.exceptionOrNull()
                )
            }
        }
    }
}
