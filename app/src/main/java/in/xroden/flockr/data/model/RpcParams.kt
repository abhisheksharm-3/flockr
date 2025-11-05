package `in`.xroden.flockr.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateHouseParams(
    @SerialName("p_name")
    val name: String,
    @SerialName("p_owner_id")
    val ownerId: String,
    @SerialName("p_invite_code")
    val inviteCode: String,
    @SerialName("p_address")
    val address: String? = null,
    @SerialName("p_latitude")
    val latitude: Double? = null,
    @SerialName("p_longitude")
    val longitude: Double? = null
)

@Serializable
data class CreateNotificationParams(
    @SerialName("p_house_id")
    val houseId: String,
    @SerialName("p_title")
    val title: String,
    @SerialName("p_message")
    val message: String,
    @SerialName("p_data")
    val data: String? = null,
    @SerialName("p_exclude_user_id")
    val excludeUserId: String? = null
)

@Serializable
data class HouseConfigUpdate(
    @SerialName("currency_code")
    val currencyCode: String? = null,
    @SerialName("currency_symbol")
    val currencySymbol: String? = null,
    @SerialName("date_format")
    val dateFormat: String? = null,
    @SerialName("first_day_of_week")
    val firstDayOfWeek: Int? = null,
    @SerialName("timezone")
    val timezone: String? = null
)

@Serializable
data class OneTimeExpenseInsert(
    @SerialName("house_id")
    val houseId: String,
    @SerialName("name")
    val name: String,
    @SerialName("amount")
    val amount: Double,
    @SerialName("date")
    val date: String,
    @SerialName("paid_by")
    val paidBy: String,
    @SerialName("category")
    val category: String,
    @SerialName("notes")
    val notes: String? = null
)

@Serializable
data class ExpenseSplitInsert(
    @SerialName("expense_id")
    val expenseId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("amount_owed")
    val amountOwed: Double
)

@Serializable
data class PerDiemConfigInsert(
    @SerialName("house_id")
    val houseId: String,
    @SerialName("item_name")
    val itemName: String,
    @SerialName("rate")
    val rate: Double,
    @SerialName("category")
    val category: String,
    @SerialName("unit")
    val unit: String,
    @SerialName("is_active")
    val isActive: Boolean = true
)

@Serializable
data class HouseMemberInsert(
    @SerialName("house_id")
    val houseId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("role")
    val role: String = "member"
)

@Serializable
data class HouseInvitationInsert(
    @SerialName("house_id")
    val houseId: String,
    @SerialName("inviter_id")
    val inviterId: String,
    @SerialName("invitee_email")
    val inviteeEmail: String,
    @SerialName("status")
    val status: String = "pending"
)

@Serializable
data class GetUserHouseIdsParams(
    @SerialName("p_user_id")
    val userId: String
)

@Serializable
data class GetHouseMembersParams(
    @SerialName("p_house_id")
    val houseId: String
)

@Serializable
data class HouseUpdate(
    @SerialName("name")
    val name: String? = null,
    @SerialName("address")
    val address: String? = null,
    @SerialName("latitude")
    val latitude: Double? = null,
    @SerialName("longitude")
    val longitude: Double? = null
)

@Serializable
data class PerDiemConfigUpdate(
    @SerialName("item_name")
    val itemName: String,
    @SerialName("rate")
    val rate: Double,
    @SerialName("category")
    val category: String,
    @SerialName("unit")
    val unit: String
)

@Serializable
data class PerDiemConfigActivation(
    @SerialName("is_active")
    val isActive: Boolean
)

@Serializable
data class PerDiemEntryInsert(
    @SerialName("config_id")
    val configId: String,
    @SerialName("quantity")
    val quantity: Double,
    @SerialName("date")
    val date: String,
    @SerialName("added_by")
    val addedBy: String,
    @SerialName("notes")
    val notes: String? = null
)

@Serializable
data class GetPerDiemBillParams(
    @SerialName("p_house_id")
    val houseId: String,
    @SerialName("p_start_date")
    val startDate: String,
    @SerialName("p_end_date")
    val endDate: String
)

@Serializable
data class GetPerDiemBillByMonthParams(
    @SerialName("p_house_id")
    val houseId: String,
    @SerialName("p_month")
    val month: String
)

@Serializable
data class CreateHouseResponse(
    @SerialName("out_house_id")
    val houseId: String,
    @SerialName("out_house_name")
    val houseName: String,
    @SerialName("out_invite_code")
    val inviteCode: String
)

