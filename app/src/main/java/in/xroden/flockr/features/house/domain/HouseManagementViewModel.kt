package `in`.xroden.flockr.features.house.domain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.features.house.model.House
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

    private val _currentHouse = MutableStateFlow<House?>(null)
    val currentHouse: StateFlow<House?> = _currentHouse.asStateFlow()

    fun getCurrentUserId(): String? = houseRepository.getCurrentUserId()

    fun loadHouse(houseId: String) {
        viewModelScope.launch {
            android.util.Log.d("HouseManagementViewModel", "Loading house: $houseId")
            _currentHouse.value = houseRepository.getHouseById(houseId)
            android.util.Log.d("HouseManagementViewModel", "House loaded: ${_currentHouse.value?.name}")
        }
    }

    suspend fun getHouseMembers(houseId: String): List<`in`.xroden.flockr.data.model.MemberWithProfile> {
        android.util.Log.d("HouseManagementViewModel", "Fetching members for house: $houseId")
        val members = houseRepository.getHouseMembers(houseId)
        android.util.Log.d("HouseManagementViewModel", "Fetched ${members.size} members")
        return members
    }

    suspend fun removeMember(houseId: String, userId: String): Result<Unit> {
        android.util.Log.d("HouseManagementViewModel", "Removing member: userId=$userId from house=$houseId")
        val result = houseRepository.removeMemberFromHouse(houseId, userId)
        if (result.isSuccess) {
            android.util.Log.d("HouseManagementViewModel", "Member removed successfully")
        } else {
            android.util.Log.e("HouseManagementViewModel", "Failed to remove member: ${result.exceptionOrNull()?.message}")
        }
        return result
    }

    suspend fun inviteMember(houseId: String, email: String): Result<Unit> {
        android.util.Log.d("HouseManagementViewModel", "Inviting member: email=$email to house=$houseId")
        val result = houseRepository.inviteMember(houseId, email)
        if (result.isSuccess) {
            android.util.Log.d("HouseManagementViewModel", "Invitation sent successfully")
        } else {
            android.util.Log.e("HouseManagementViewModel", "Failed to invite member: ${result.exceptionOrNull()?.message}")
        }
        return result
    }

    suspend fun getPendingInvitations(houseId: String): List<`in`.xroden.flockr.data.model.HouseInvitation> {
        android.util.Log.d("HouseManagementViewModel", "Fetching pending invitations for house: $houseId")
        val invitations = houseRepository.getPendingInvitations(houseId)
        android.util.Log.d("HouseManagementViewModel", "Fetched ${invitations.size} pending invitations")
        return invitations
    }

    suspend fun cancelInvitation(invitationId: String): Result<Unit> {
        android.util.Log.d("HouseManagementViewModel", "Cancelling invitation: $invitationId")
        val result = houseRepository.cancelInvitation(invitationId)
        if (result.isSuccess) {
            android.util.Log.d("HouseManagementViewModel", "Invitation cancelled successfully")
        } else {
            android.util.Log.e("HouseManagementViewModel", "Failed to cancel invitation: ${result.exceptionOrNull()?.message}")
        }
        return result
    }

    suspend fun resendInvitationNotification(invitationId: String): Result<Unit> {
        android.util.Log.d("HouseManagementViewModel", "Resending invitation notification: $invitationId")
        val result = houseRepository.resendInvitationNotification(invitationId)
        if (result.isSuccess) {
            android.util.Log.d("HouseManagementViewModel", "Notification resent successfully")
        } else {
            android.util.Log.e("HouseManagementViewModel", "Failed to resend notification: ${result.exceptionOrNull()?.message}")
        }
        return result
    }
}
