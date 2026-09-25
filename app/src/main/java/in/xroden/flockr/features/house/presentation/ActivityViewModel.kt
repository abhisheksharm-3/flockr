/** A house's activity feed with the members it names. */
package `in`.xroden.flockr.features.house.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.features.house.data.HouseAuditRepository
import `in`.xroden.flockr.features.house.data.HouseRepository
import `in`.xroden.flockr.features.house.model.HouseAuditLog
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ActivityUiState {
    data object Loading : ActivityUiState
    data class Error(val message: String) : ActivityUiState
    data class Ready(val events: List<HouseAuditLog>, val members: Map<String, MemberWithProfile>, val viewerId: String) : ActivityUiState
}

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val auditRepository: HouseAuditRepository,
    private val houseRepository: HouseRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ActivityUiState>(ActivityUiState.Loading)
    val state: StateFlow<ActivityUiState> = _state.asStateFlow()

    fun load(houseId: String) {
        viewModelScope.launch {
            val members = async { houseRepository.getHouseMembers(houseId).getOrElse { emptyList() }.associateBy { it.userId } }
            _state.value = auditRepository.getActivity(houseId).fold(
                onSuccess = { ActivityUiState.Ready(it, members.await(), houseRepository.getCurrentUserId().orEmpty()) },
                onFailure = { ActivityUiState.Error(it.userMessage()) },
            )
        }
    }
}
