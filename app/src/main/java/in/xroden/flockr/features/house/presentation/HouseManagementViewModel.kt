/** A house's members, past and present, its invite code and email invitations, and the admin actions on them. */
package `in`.xroden.flockr.features.house.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.core.presentation.Notice
import `in`.xroden.flockr.data.enums.HouseMemberRole
import `in`.xroden.flockr.features.house.data.HouseInvitationRepository
import `in`.xroden.flockr.features.house.data.HouseRepository
import `in`.xroden.flockr.features.house.data.MAX_SPLIT_WEIGHT_DECIMALS
import `in`.xroden.flockr.features.house.model.HouseInvitation
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.utils.parseDecimal
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

private val MAX_SPLIT_WEIGHT = BigDecimal("99999.999")

sealed interface ManageMembersUiState {
    data object Loading : ManageMembersUiState
    data class Error(val message: String) : ManageMembersUiState

    /** [invitations] are the pending ones the viewer may see: all of them for an admin, their own for anyone else. */
    data class Ready(
        val houseName: String,
        val inviteCode: String?,
        val members: List<MemberWithProfile>,
        val viewerId: String,
        val invitations: List<HouseInvitation>,
        val inviteEmail: String = "",
        val isInviting: Boolean = false,
        val isRegenerating: Boolean = false,
    ) : ManageMembersUiState {
        val viewerRole: HouseMemberRole? get() = members.firstOrNull { it.userId == viewerId && it.isActive }?.role
        val isOwner: Boolean get() = viewerRole == HouseMemberRole.OWNER
        val isAdmin: Boolean get() = viewerRole == HouseMemberRole.OWNER || viewerRole == HouseMemberRole.ADMIN
        val currentMembers: List<MemberWithProfile> get() = members.filter { it.isActive }
        val pastMembers: List<MemberWithProfile> get() = members.filterNot { it.isActive }
    }
}

/** Why [text] can't be a split weight, or null when it can: above zero, at most three decimals. */
fun splitWeightError(text: String): String? {
    val weight = parseDecimal(text) ?: return "Enter a number, like 1 or 1.5"
    return when {
        weight.signum() <= 0 -> "The share must be more than zero"
        weight.stripTrailingZeros().scale() > MAX_SPLIT_WEIGHT_DECIMALS -> "Use at most $MAX_SPLIT_WEIGHT_DECIMALS decimal places"
        weight > MAX_SPLIT_WEIGHT -> "That share is too large"
        else -> null
    }
}

@HiltViewModel
class HouseManagementViewModel @Inject constructor(
    private val houseRepository: HouseRepository,
    private val houseInvitationRepository: HouseInvitationRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ManageMembersUiState>(ManageMembersUiState.Loading)
    val state: StateFlow<ManageMembersUiState> = _state.asStateFlow()

    private val _events = Channel<Notice>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var houseId: String = ""

    fun load(houseId: String) {
        this.houseId = houseId
        viewModelScope.launch {
            if (_state.value !is ManageMembersUiState.Ready) _state.value = ManageMembersUiState.Loading
            refresh()
        }
    }

    private suspend fun refresh() {
        val house = viewModelScope.async { houseRepository.getHouseById(houseId) }
        val viewerId = houseRepository.getCurrentUserId().orEmpty()
        val membersResult = houseRepository.getHouseMembers(houseId)
        val houseResult = house.await()
        val members = membersResult.getOrElse {
            _state.value = ManageMembersUiState.Error(it.userMessage())
            return
        }
        val loadedHouse = houseResult.getOrElse {
            _state.value = ManageMembersUiState.Error(it.userMessage())
            return
        }
        val invitations = houseInvitationRepository.getSentInvitations(houseId).getOrElse { emptyList() }
        val previous = _state.value as? ManageMembersUiState.Ready
        _state.value = ManageMembersUiState.Ready(
            houseName = loadedHouse.name,
            inviteCode = loadedHouse.inviteCode,
            members = members,
            viewerId = viewerId,
            invitations = invitations,
            inviteEmail = previous?.inviteEmail.orEmpty(),
        )
    }

    fun updateInviteEmail(email: String) {
        update { it.copy(inviteEmail = email) }
    }

    fun invite() {
        val ready = _state.value as? ManageMembersUiState.Ready ?: return
        if (ready.isInviting || ready.inviteEmail.isBlank()) return
        update { it.copy(isInviting = true) }
        viewModelScope.launch {
            val email = ready.inviteEmail.trim()
            houseInvitationRepository.inviteMember(houseId, email).fold(
                onSuccess = {
                    update { it.copy(inviteEmail = "") }
                    refresh()
                    _events.send(Notice("Invitation sent to $email", isError = false))
                },
                onFailure = { _events.send(Notice(it.userMessage(), isError = true)) },
            )
            update { it.copy(isInviting = false) }
        }
    }

    fun cancelInvitation(invitation: HouseInvitation) = act("Invitation to ${invitation.inviteeEmail} cancelled") {
        houseInvitationRepository.cancelInvitation(invitation.id)
    }

    fun changeRole(member: MemberWithProfile, role: HouseMemberRole) = act(
        when (role) {
            HouseMemberRole.OWNER -> "${member.displayName} now owns the house"
            HouseMemberRole.ADMIN -> "${member.displayName} is now an admin"
            HouseMemberRole.MEMBER -> "${member.displayName} is now a member"
        }
    ) {
        houseRepository.updateMemberRole(houseId, member.userId, role)
    }

    fun remove(member: MemberWithProfile) = act("${member.displayName} removed") {
        houseRepository.removeMember(houseId, member.userId)
    }

    fun setSplitWeight(member: MemberWithProfile, text: String) {
        if (splitWeightError(text) != null) return
        val weight = parseDecimal(text) ?: return
        act("${member.displayName}'s share is now ${weight.stripTrailingZeros().toPlainString()}") {
            houseRepository.setDefaultSplitWeight(houseId, member.userId, weight)
        }
    }

    fun regenerateInviteCode() {
        val ready = _state.value as? ManageMembersUiState.Ready ?: return
        if (ready.isRegenerating) return
        update { it.copy(isRegenerating = true) }
        viewModelScope.launch {
            houseRepository.regenerateInviteCode(houseId).fold(
                onSuccess = { code ->
                    update { it.copy(inviteCode = code) }
                    _events.send(Notice("New invite code ready. The old one no longer works.", isError = false))
                },
                onFailure = { _events.send(Notice(it.userMessage(), isError = true)) },
            )
            update { it.copy(isRegenerating = false) }
        }
    }

    /** Runs [call], reloads the roster on success, and reports either outcome. */
    private fun act(successMessage: String, call: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            call().fold(
                onSuccess = {
                    refresh()
                    _events.send(Notice(successMessage, isError = false))
                },
                onFailure = { _events.send(Notice(it.userMessage(), isError = true)) },
            )
        }
    }

    private fun update(transform: (ManageMembersUiState.Ready) -> ManageMembersUiState.Ready) {
        (_state.value as? ManageMembersUiState.Ready)?.let { _state.value = transform(it) }
    }
}
