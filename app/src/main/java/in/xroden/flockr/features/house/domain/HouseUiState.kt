package `in`.xroden.flockr.features.house.domain

import `in`.xroden.flockr.features.house.model.House
import `in`.xroden.flockr.features.house.model.HouseCardData
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.model.HouseInvitation
import `in`.xroden.flockr.features.house.model.MemberWithProfile

sealed interface HouseListUiState {
    data object Loading : HouseListUiState
    data class Success(val houses: List<HouseCardData>) : HouseListUiState
    data class Error(val message: String, val cause: Throwable? = null) : HouseListUiState
}

sealed interface HouseDetailUiState {
    data object Loading : HouseDetailUiState
    data class Success(
        val house: House,
        val config: HouseConfig?,
        val members: List<MemberWithProfile>
    ) : HouseDetailUiState
    data class Error(val message: String, val cause: Throwable? = null) : HouseDetailUiState
}

sealed interface CreateHouseUiState {
    data object Idle : CreateHouseUiState
    data object Loading : CreateHouseUiState
    data class Success(val house: House) : CreateHouseUiState
    data class Error(val message: String) : CreateHouseUiState
}

sealed interface JoinHouseUiState {
    data object Idle : JoinHouseUiState
    data object Loading : JoinHouseUiState
    data class Success(val house: House?) : JoinHouseUiState
    data class Error(val message: String) : JoinHouseUiState
}

sealed interface InvitationsUiState {
    data object Loading : InvitationsUiState
    data class Success(val invitations: List<HouseInvitation>) : InvitationsUiState
    data class Error(val message: String) : InvitationsUiState
}


