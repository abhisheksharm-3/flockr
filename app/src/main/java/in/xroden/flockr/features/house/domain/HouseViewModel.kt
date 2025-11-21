package `in`.xroden.flockr.features.house.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.house.data.HouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HouseViewModel @Inject constructor(
    private val houseRepository: HouseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HouseDetailUiState>(HouseDetailUiState.Loading)
    val uiState: StateFlow<HouseDetailUiState> = _uiState.asStateFlow()

    fun loadHouseDetails(houseId: String) {
        viewModelScope.launch {
            _uiState.value = HouseDetailUiState.Loading
            
            val houseResult = houseRepository.getHouseById(houseId)
            val configResult = houseRepository.getHouseConfig(houseId)
            val membersResult = houseRepository.getHouseMembers(houseId)
            
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
