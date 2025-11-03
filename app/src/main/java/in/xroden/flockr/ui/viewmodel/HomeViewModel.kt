package `in`.xroden.flockr.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.data.model.House
import `in`.xroden.flockr.data.repository.HouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val houseRepository: HouseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHouses()
    }

    fun loadHouses() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            android.util.Log.d("HomeViewModel", "Loading houses...")
            try {
                houseRepository.getHousesFlow().collect { houses ->
                    android.util.Log.d("HomeViewModel", "Houses loaded: ${houses.size} houses")
                    _uiState.value = HomeUiState.Success(houses)
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading houses", e)
                _uiState.value = HomeUiState.Error(e.message ?: "Failed to load houses")
            }
        }
    }

    fun createHouse(name: String, address: String?, latitude: Double?, longitude: Double?, onSuccess: (House) -> Unit) {
        viewModelScope.launch {
            try {
                android.util.Log.d("HomeViewModel", "createHouse called - name='$name', address='$address', latitude=$latitude, longitude=$longitude")
                _uiState.value = HomeUiState.Loading
                houseRepository.createHouse(name, address, latitude, longitude).fold(
                    onSuccess = { house ->
                        android.util.Log.d("HomeViewModel", "createHouse succeeded - id=${house.id}, name=${house.name}, inviteCode=${house.inviteCode}")
                        loadHouses()
                        onSuccess(house)
                    },
                    onFailure = { error ->
                        android.util.Log.e("HomeViewModel", "createHouse failed", error)
                        _uiState.value = HomeUiState.Error(error.message ?: "Failed to create house")
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Exception in createHouse", e)
                _uiState.value = HomeUiState.Error(e.message ?: "Failed to create house")
            }
        }
    }

    fun joinHouseByInviteCode(inviteCode: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                android.util.Log.d("HomeViewModel", "joinHouseByInviteCode called - code='$inviteCode'")
                houseRepository.joinHouseByInviteCode(inviteCode).fold(
                    onSuccess = { house ->
                        android.util.Log.d("HomeViewModel", "joinHouseByInviteCode succeeded - joined house: ${house.name}")
                        loadHouses()
                        onSuccess(house.id)
                    },
                    onFailure = { error ->
                        android.util.Log.e("HomeViewModel", "joinHouseByInviteCode failed", error)
                        onError(error.message ?: "Failed to join household")
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Exception in joinHouseByInviteCode", e)
                onError(e.message ?: "Failed to join household")
            }
        }
    }

    suspend fun getHouseById(houseId: String): House? {
        android.util.Log.d("HomeViewModel", "getHouseById called - id='$houseId'")
        return try {
            val house = houseRepository.getHouseById(houseId)
            android.util.Log.d("HomeViewModel", "getHouseById succeeded - house=${house?.name}")
            house
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "getHouseById failed", e)
            null
        }
    }
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val houses: List<House>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
