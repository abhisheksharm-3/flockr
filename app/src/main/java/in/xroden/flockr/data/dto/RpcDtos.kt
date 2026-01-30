package `in`.xroden.flockr.data.dto

import `in`.xroden.flockr.data.serialization.BigDecimalSerializer
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * RPC parameter and response DTOs for Supabase remote procedure calls.
 * These classes are used for serializing/deserializing data sent to and received from PostgreSQL RPC functions.
 */

// ============================================================================
// House RPC DTOs
// ============================================================================

@Serializable
data class HouseNotificationParams(
    @SerialName("p_house_id") val houseId: String,
    @SerialName("p_title") val title: String,
    @SerialName("p_message") val message: String,
    @SerialName("p_type") val type: String,
    @SerialName("p_data") val data: String,
    @SerialName("p_exclude_user_id") val excludeUserId: String?
)

@Serializable
data class NotificationParams(
    @SerialName("user_id") val userId: String,
    @SerialName("house_id") val houseId: String,
    val title: String,
    val message: String,
    val type: String,
    val data: String
)

@Serializable
data class DeleteNotificationParams(
    @SerialName("p_notification_id") val notificationId: String
)

@Serializable
data class DeleteAllNotificationsParams(
    @SerialName("p_user_id") val userId: String
)

@Serializable
data class GetUserHouseIdsParams(
    @SerialName("p_user_id") val userId: String
)

@Serializable
data class HouseIdResult(
    @SerialName("house_id") val houseId: String
)

@Serializable
data class CreateHouseParams(
    @SerialName("p_name") val name: String,
    @SerialName("p_owner_id") val ownerId: String,
    @SerialName("p_invite_code") val inviteCode: String,
    @SerialName("p_address") val address: String?,
    @SerialName("p_latitude") val latitude: Double?,
    @SerialName("p_longitude") val longitude: Double?
)

@Serializable
data class CreateHouseResponse(
    @SerialName("out_house_id") val houseId: String,
    @SerialName("out_house_name") val houseName: String,
    @SerialName("out_invite_code") val inviteCode: String
)

@Serializable
data class LeaveHouseParams(
    @SerialName("p_house_id") val houseId: String,
    @SerialName("p_user_id") val userId: String
)

@Serializable
data class InviteCodeParam(
    val code: String
)

@Serializable
data class JoinHouseResult(
    val success: Boolean,
    val error: String? = null,
    @SerialName("house_id") val houseId: String? = null
)

@Serializable
data class GetHouseMembersParams(
    @SerialName("p_house_id") val houseId: String
)

@Serializable
data class CancelInvitationParams(
    @SerialName("p_house_id") val houseId: String,
    @SerialName("p_email") val email: String
)

@Serializable
data class ResendInvitationParams(
    @SerialName("p_house_id") val houseId: String,
    @SerialName("p_email") val email: String
)

@Serializable
data class DeleteHouseParams(
    @SerialName("p_house_id") val houseId: String
)

@Serializable
data class NotificationInsertParams(
    @SerialName("user_id") val userId: String,
    @SerialName("house_id") val houseId: String,
    val title: String,
    val message: String,
    val type: String,
    @SerialName("is_read") val isRead: Boolean = false
)

// ============================================================================
// Expense RPC DTOs
// ============================================================================

@Serializable
data class CreateExpenseParams(
    @SerialName("p_house_id") val houseId: String,
    @SerialName("p_paid_by") val paidBy: String,
    @SerialName("p_name") val name: String,
    @SerialName("p_amount")
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    @SerialName("p_category") val category: String,
    @SerialName("p_date") val date: LocalDate,
    @SerialName("p_notes") val notes: String?,
    @SerialName("p_splits") val splits: kotlinx.serialization.json.JsonElement
)

@Serializable
data class GetRecurringExpensesParams(
    @SerialName("p_house_id") val houseId: String
)

@Serializable
data class SettleBalanceParams(
    @SerialName("p_house_id") val houseId: String,
    @SerialName("p_payer_id") val payerId: String,
    @SerialName("p_payee_id") val payeeId: String,
    @SerialName("p_amount")
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    @SerialName("p_description") val description: String?
)

@Serializable
data class GetUserBalancesParams(
    @SerialName("p_house_id") val houseId: String
)

@Serializable
data class GetDebtBreakdownParams(
    @SerialName("p_house_id") val houseId: String,
    @SerialName("p_payer_id") val payerId: String,
    @SerialName("p_payee_id") val payeeId: String
)

@Serializable
data class GetMonthlySummaryParams(
    @SerialName("p_house_id") val houseId: String,
    @SerialName("p_month") val month: String
)

@Serializable
data class GetSpendByMemberParams(
    @SerialName("p_house_id") val houseId: String,
    @SerialName("p_month") val month: String
)

@Serializable
data class GetSpendByCategoryParams(
    @SerialName("p_house_id") val houseId: String,
    @SerialName("p_month") val month: String
)

/**
 * Response DTO for debt breakdown RPC call.
 */
@Serializable
data class DebtBreakdownItem(
    @SerialName("expense_id") val expenseId: String,
    @SerialName("expense_name") val expenseName: String,
    @SerialName("date") val date: LocalDate,
    @SerialName("amount_owed")
    @Serializable(with = BigDecimalSerializer::class)
    val amountOwed: BigDecimal,
    @SerialName("total_amount")
    @Serializable(with = BigDecimalSerializer::class)
    val totalAmount: BigDecimal
)

// ============================================================================
// Per Diem RPC DTOs
// ============================================================================

@Serializable
data class GetPerDiemBillParams(
    @SerialName("p_house_id") val houseId: String,
    @SerialName("p_month") val month: String
)

@Serializable
data class PerDiemBillByMemberParams(
    @SerialName("p_house_id") val houseId: String,
    @SerialName("p_month") val month: String
)

@Serializable
data class PerDiemEntriesParams(
    @SerialName("p_house_id") val houseId: String,
    @SerialName("p_month")
    @Serializable(with = `in`.xroden.flockr.data.serialization.LocalDateSerializer::class)
    val month: LocalDate? = null
)

@Serializable
data class PerDiemConfigParams(
    @SerialName("p_house_id") val houseId: String
)


// ============================================================================
// Invitation Update DTO
// ============================================================================

@Serializable
data class InvitationStatusUpdate(
    val status: String
)
