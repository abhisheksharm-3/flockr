package `in`.xroden.flockr.features.house.presentation

import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.core.validation.Validators
import java.math.BigDecimal

import `in`.xroden.flockr.features.house.model.House
import `in`.xroden.flockr.features.house.model.HouseCardData
import `in`.xroden.flockr.features.house.model.HousePreview
import `in`.xroden.flockr.features.house.model.MemberWithProfile

sealed interface HouseListUiState {
    data object Loading : HouseListUiState
    data class Success(val houses: List<HouseCardData>) : HouseListUiState
    data class Error(val message: String, val cause: Throwable? = null) : HouseListUiState
}

/** [viewerNet] is null when the balances could not be loaded, so the home can still open without them. */
sealed interface HouseDetailUiState {
    data object Loading : HouseDetailUiState
    data class Ready(
        val house: House,
        val members: List<MemberWithProfile>,
        val viewerId: String,
        val viewerNet: BigDecimal?,
    ) : HouseDetailUiState {
        val activeMembers: List<MemberWithProfile> get() = members.filter { it.isActive }
    }
    data class Error(val message: String) : HouseDetailUiState
}

/** [Created.photoUploaded] is false when the house was made but its header photo failed to upload. */
sealed interface CreateHouseUiState {
    data object Idle : CreateHouseUiState
    data object Creating : CreateHouseUiState
    data class Created(val house: House, val photoUploaded: Boolean) : CreateHouseUiState
}

sealed interface HousePreviewUiState {
    data object Idle : HousePreviewUiState
    data object Loading : HousePreviewUiState
    data class Success(val preview: HousePreview) : HousePreviewUiState
    data class Error(val message: String) : HousePreviewUiState
}

/**
 * The settings form. Only admins may change the house, so [canEdit] is false for a plain member,
 * who can still see the settings, open the activity log and leave.
 */
sealed interface HouseSettingsUiState {
    data object Loading : HouseSettingsUiState
    data class Error(val message: String) : HouseSettingsUiState
    data class Ready(
        val house: House,
        val viewerId: String,
        val canEdit: Boolean,
        val name: String,
        val address: String,
        val currencyCode: String,
        val dateFormat: String,
        val firstDayOfWeek: Int,
        val timezone: String,
        val isCurrencyLocked: Boolean,
        val isSaving: Boolean = false,
        val isUploadingImage: Boolean = false,
    ) : HouseSettingsUiState {
        val isOwner: Boolean get() = house.ownerId == viewerId
        val nameError: String? get() = Validators.validateHouseName(name).exceptionOrNull()?.userMessage()
        val canSave: Boolean get() = canEdit && nameError == null && !isSaving
    }
}
