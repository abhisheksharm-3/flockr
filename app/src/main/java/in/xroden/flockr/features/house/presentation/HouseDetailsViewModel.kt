/** A house's home: the house, who lives there, and where the viewer stands with them. */
package `in`.xroden.flockr.features.house.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.features.expenses.data.ExpenseRepository
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
    private val expenseRepository: ExpenseRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<HouseDetailUiState>(HouseDetailUiState.Loading)
    val state: StateFlow<HouseDetailUiState> = _state.asStateFlow()

    fun load(houseId: String) {
        viewModelScope.launch {
            if (_state.value !is HouseDetailUiState.Ready) _state.value = HouseDetailUiState.Loading
            val viewerId = houseRepository.getCurrentUserId().orEmpty()
            val members = async { houseRepository.getHouseMembers(houseId).getOrElse { emptyList() } }
            val standing = async { expenseRepository.getStanding(houseId).getOrNull() }
            _state.value = houseRepository.getHouseById(houseId).fold(
                onSuccess = { house -> HouseDetailUiState.Ready(house, members.await(), viewerId, standing.await()?.netOf(viewerId)) },
                onFailure = { HouseDetailUiState.Error(it.userMessage()) },
            )
        }
    }
}
