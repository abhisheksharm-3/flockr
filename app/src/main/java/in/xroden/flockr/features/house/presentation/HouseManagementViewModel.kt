package `in`.xroden.flockr.features.house.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.house.data.IHouseRepository
import `in`.xroden.flockr.features.house.data.IHouseInvitationRepository
import `in`.xroden.flockr.features.house.model.InvitationWithHouse
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.data.enums.HouseMemberRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HouseManagementUiState {
    data object Idle : HouseManagementUiState
    data object Loading : HouseManagementUiState
    data object Success : HouseManagementUiState
    data class Error(val message: String) : HouseManagementUiState
}

@HiltViewModel
class HouseManagementViewModel @Inject constructor(
    private val houseRepository: IHouseRepository,
    private val houseInvitationRepository: IHouseInvitationRepository
) : ViewModel() {

    private val _detailState = MutableStateFlow<HouseDetailUiState>(HouseDetailUiState.Loading)
    val detailState: StateFlow<HouseDetailUiState> = _detailState.asStateFlow()

    private val _invitationsState = MutableStateFlow<InvitationsUiState>(InvitationsUiState.Loading)
    val invitationsState: StateFlow<InvitationsUiState> = _invitationsState.asStateFlow()

    private val _uiState = MutableStateFlow<HouseManagementUiState>(HouseManagementUiState.Idle)
    val uiState: StateFlow<HouseManagementUiState> = _uiState.asStateFlow()

    fun getCurrentUserId(): String? = houseRepository.getCurrentUserId()

    fun loadHouseDetails(houseId: String) {
        viewModelScope.launch {
            _detailState.value = HouseDetailUiState.Loading
            
            val houseResult = houseRepository.getHouseById(houseId)
            val configResult = houseRepository.getHouseConfig(houseId)
            val membersResult = houseRepository.getHouseMembers(houseId)
            
            if (houseResult.isSuccess) {
                val house = houseResult.getOrNull()
                if (house != null) {
                    _detailState.value = HouseDetailUiState.Success(
                        house = house,
                        config = configResult.getOrNull(),
                        members = membersResult.getOrElse { emptyList() }
                    )
                } else {
                    _detailState.value = HouseDetailUiState.Error("House not found", null)
                }
            } else {
                _detailState.value = HouseDetailUiState.Error(
                    message = houseResult.exceptionOrNull()?.message ?: "Failed to load house",
                    cause = houseResult.exceptionOrNull()
                )
            }
        }
    }

    fun loadInvitations(houseId: String) {
        viewModelScope.launch {
            _invitationsState.value = InvitationsUiState.Loading
            
            houseInvitationRepository.getPendingInvitations().fold(
                onSuccess = { invitations ->
                    // Filter by houseId since API returns all invitations
                    val filtered = invitations.filter { invitation -> invitation.houseId == houseId }
                    _invitationsState.value = InvitationsUiState.Success(filtered)
                },
                onFailure = { error ->
                    _invitationsState.value = InvitationsUiState.Error(
                        message = error.message ?: "Failed to load invitations"
                    )
                }
            )
        }
    }

    suspend fun getPendingInvitations(houseId: String): List<InvitationWithHouse> {
        return houseInvitationRepository.getPendingInvitations().getOrElse { emptyList() }.filter { it.houseId == houseId }
    }

    suspend fun removeMember(houseId: String, userId: String): Result<Unit> {
        return houseRepository.removeMember(houseId, userId).onSuccess {
            loadHouseDetails(houseId)
        }
    }

    suspend fun inviteMember(houseId: String, email: String): Result<Unit> {
        return houseInvitationRepository.inviteMember(houseId, email).onSuccess {
            loadInvitations(houseId)
        }
    }

    suspend fun cancelInvitation(houseId: String, email: String): Result<Unit> {
        return houseInvitationRepository.cancelInvitation(houseId, email).onSuccess {
            loadInvitations(houseId)
        }
    }

    suspend fun resendInvitationNotification(houseId: String, email: String): Result<Unit> {
        return houseInvitationRepository.resendInvitationNotification(houseId, email)
    }

    suspend fun updateMemberRole(
        houseId: String,
        userId: String,
        role: `in`.xroden.flockr.data.enums.HouseMemberRole
    ): Result<Unit> {
        return houseRepository.updateMemberRole(houseId, userId, role).onSuccess {
            loadHouseDetails(houseId)
        }
    }

    suspend fun leaveHouse(houseId: String): Result<Unit> = houseRepository.leaveHouse(houseId)

    suspend fun getHouseMembers(houseId: String) = houseRepository.getHouseMembers(houseId).getOrElse { emptyList() }
}
