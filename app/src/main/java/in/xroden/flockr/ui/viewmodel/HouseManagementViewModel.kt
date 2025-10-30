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
class HouseManagementViewModel @Inject constructor(
    private val houseRepository: HouseRepository
) : ViewModel() {

    private val _currentHouse = MutableStateFlow<House?>(null)
    val currentHouse: StateFlow<House?> = _currentHouse.asStateFlow()

    fun loadHouse(houseId: String) {
        viewModelScope.launch {
            _currentHouse.value = houseRepository.getHouseById(houseId)
        }
    }
}

