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
class HouseManagementViewModel @Inject constructor(
    private val houseRepository: HouseRepository
) : ViewModel() {

    private val _detailState = MutableStateFlow<HouseDetailUiState>(HouseDetailUiState.Loading)
    val detailState: StateFlow<HouseDetailUiState> = _detailState.asStateFlow()

    private val _invitationsState = MutableStateFlow<InvitationsUiState>(InvitationsUiState.Loading)
    val invitationsState: StateFlow<InvitationsUiState> = _invitationsState.asStateFlow()

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
            
            houseRepository.getPendingInvitations(houseId).fold(
                onSuccess = { invitations ->
                    _invitationsState.value = InvitationsUiState.Success(invitations)
                },
                onFailure = { error ->
                    _invitationsState.value = InvitationsUiState.Error(
                        message = error.message ?: "Failed to load invitations"
                    )
                }
            )
        }
    }

    fun removeMember(houseId: String, userId: String) {
        viewModelScope.launch {
            houseRepository.removeMemberFromHouse(houseId, userId).fold(
                onSuccess = {
                    loadHouseDetails(houseId)
                },
                onFailure = { error ->
                    _detailState.value = HouseDetailUiState.Error(
                        message = error.message ?: "Failed to remove member",
                        cause = error
                    )
                }
            )
        }
    }

    fun inviteMember(houseId: String, email: String) {
        viewModelScope.launch {
            houseRepository.inviteMember(houseId, email).fold(
                onSuccess = {
                    loadInvitations(houseId)
                },
                onFailure = { error ->
                    _invitationsState.value = InvitationsUiState.Error(
                        message = error.message ?: "Failed to invite member"
                    )
                }
            )
        }
    }

    fun cancelInvitation(houseId: String, invitationId: String) {
        viewModelScope.launch {
            houseRepository.cancelInvitation(invitationId).fold(
                onSuccess = {
                    loadInvitations(houseId)
                },
                onFailure = { error ->
                    _invitationsState.value = InvitationsUiState.Error(
                        message = error.message ?: "Failed to cancel invitation"
                    )
                }
            )
        }
    }

    fun resendInvitationNotification(invitationId: String) {
        viewModelScope.launch {
            houseRepository.resendInvitationNotification(invitationId).fold(
                onSuccess = {
                    // Success
                },
                onFailure = { error ->
                    _invitationsState.value = InvitationsUiState.Error(
                        message = error.message ?: "Failed to resend notification"
                    )
                }
            )
        }
    }
}
