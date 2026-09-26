package `in`.xroden.flockr.features.house.presentation

import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.core.validation.Validators
import java.math.BigDecimal
import `in`.xroden.flockr.data.enums.HouseMemberRole
import `in`.xroden.flockr.features.chores.model.Chore
import `in`.xroden.flockr.features.expenses.model.Expense
import `in`.xroden.flockr.features.expenses.model.RecurringExpense
import `in`.xroden.flockr.features.expenses.model.SettleUpPayment
import kotlinx.datetime.LocalDate

import `in`.xroden.flockr.features.house.model.House
import `in`.xroden.flockr.features.house.model.HouseCardData
import `in`.xroden.flockr.features.house.model.HousePreview
import `in`.xroden.flockr.features.house.model.MemberWithProfile

sealed interface HouseListUiState {
    data object Loading : HouseListUiState
    data class Success(val houses: List<HouseCardData>) : HouseListUiState
    data class Error(val message: String, val cause: Throwable? = null) : HouseListUiState
}

/** A house's home: the house, and its [digest] of people, balances and what is coming up. */
sealed interface HouseDetailUiState {
    data object Loading : HouseDetailUiState
    data class Ready(val house: House, val viewerId: String, val digest: HouseDigest) : HouseDetailUiState {
        val members: List<MemberWithProfile> get() = digest.members
        val activeMembers: List<MemberWithProfile> get() = members.filter { it.isActive }
        val today: LocalDate get() = digest.today
        val upcomingBills: List<RecurringExpense> get() = digest.upcomingBills
        val myChores: List<Chore> get() = digest.myChores
        val recent: List<Expense> get() = digest.recent
        val viewerNet: BigDecimal? get() = digest.standing?.netOf(viewerId)
        val viewerPayments: List<SettleUpPayment> get() = digest.standing?.paymentsOf(viewerId).orEmpty()
        val canManageHouse: Boolean
            get() = members.firstOrNull { it.userId == viewerId }?.role.let { it == HouseMemberRole.OWNER || it == HouseMemberRole.ADMIN }
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
        val latitude: Double? = null,
        val longitude: Double? = null,
        val currencyCode: String,
        val dateFormat: String,
        val firstDayOfWeek: Int,
        val timezone: String,
        val isCurrencyLocked: Boolean,
        val isSaving: Boolean = false,
        val isUploadingImage: Boolean = false,
        val saved: List<Any?> = emptyList(),
    ) : HouseSettingsUiState {
        val isOwner: Boolean get() = house.ownerId == viewerId
        val nameError: String? get() = Validators.validateHouseName(name).exceptionOrNull()?.userMessage()

        /** The editable values, compared against [saved] to know whether there is anything to save. */
        val values: List<Any?> get() = listOf(name.trim(), address.trim(), latitude, longitude, currencyCode, dateFormat, firstDayOfWeek, timezone)
        val hasChanges: Boolean get() = values != saved
        val canSave: Boolean get() = canEdit && hasChanges && nameError == null && !isSaving
    }
}
