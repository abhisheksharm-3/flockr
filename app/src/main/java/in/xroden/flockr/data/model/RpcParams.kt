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
data class CreateNotificationWithTypeParams(
    @SerialName("p_house_id")
    val houseId: String,
    @SerialName("p_title")
    val title: String,
    @SerialName("p_message")
    val message: String,
    @SerialName("p_type")
    val type: String,
    @SerialName("p_data")
    val data: String = "{}",
    @SerialName("p_exclude_user_id")
    val excludeUserId: String? = null
)

@Serializable
data class ShoppingItemUpdateModel(
    @SerialName("item_name")
    val itemName: String,
    @SerialName("quantity")
    val quantity: String?
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
data class GetMonthlySummaryParams(
    @SerialName("p_house_id")
    val houseId: String,
    @SerialName("p_month")
    val month: String
)

@Serializable
data class GetSpendByCategoryParams(
    @SerialName("p_house_id")
    val houseId: String,
    @SerialName("p_month")
    val month: String
)

@Serializable
data class GetUserBalancesParams(
    @SerialName("p_house_id")
    val houseId: String
)

@Serializable
data class GetPerDiemEntriesWithDetailsParams(
    @SerialName("p_house_id")
    val houseId: String,
    @SerialName("p_month")
    val month: String? = null
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

@Serializable
data class RecurringExpenseInsert(
    @SerialName("house_id")
    val houseId: String,
    @SerialName("name")
    val name: String,
    @SerialName("amount")
    val amount: Double,
    @SerialName("due_day")
    val dueDay: Int,
    @SerialName("category")
    val category: String,
    @SerialName("created_by")
    val createdBy: String,
    @SerialName("frequency")
    val frequency: String = "monthly",
    @SerialName("custom_frequency_days")
    val customFrequencyDays: Int? = null,
    @SerialName("reminder_days_before")
    val reminderDaysBefore: Int = 3,
    @SerialName("reminder_enabled")
    val reminderEnabled: Boolean = true,
    @SerialName("notes")
    val notes: String? = null
)

@Serializable
data class RecurringExpenseUpdate(
    @SerialName("name")
    val name: String? = null,
    @SerialName("amount")
    val amount: Double? = null,
    @SerialName("due_day")
    val dueDay: Int? = null,
    @SerialName("category")
    val category: String? = null,
    @SerialName("is_active")
    val isActive: Boolean? = null,
    @SerialName("frequency")
    val frequency: String? = null,
    @SerialName("custom_frequency_days")
    val customFrequencyDays: Int? = null,
    @SerialName("reminder_days_before")
    val reminderDaysBefore: Int? = null,
    @SerialName("reminder_enabled")
    val reminderEnabled: Boolean? = null,
    @SerialName("notes")
    val notes: String? = null,
    @SerialName("last_paid_date")
    val lastPaidDate: String? = null
)

@Serializable
data class PaymentHistoryInsert(
    @SerialName("recurring_expense_id")
    val recurringExpenseId: String,
    @SerialName("paid_by")
    val paidBy: String,
    @SerialName("amount")
    val amount: Double,
    @SerialName("payment_date")
    val paymentDate: String
)

@Serializable
data class TransactionInsert(
    @SerialName("house_id")
    val houseId: String,
    @SerialName("payer_id")
    val payerId: String,
    @SerialName("payee_id")
    val payeeId: String,
    @SerialName("amount")
    val amount: Double,
    @SerialName("is_settlement")
    val isSettlement: Boolean = true,
    @SerialName("description")
    val description: String? = null
)

@Serializable
data class NotificationInsert(
    @SerialName("user_id")
    val userId: String,
    @SerialName("house_id")
    val houseId: String,
    @SerialName("title")
    val title: String,
    @SerialName("message")
    val message: String,
    @SerialName("type")
    val type: String = "general",
    @SerialName("is_read")
    val isRead: Boolean = false,
    @SerialName("data")
    val data: String? = null
)

@Serializable
data class NotificationUpdate(
    @SerialName("is_read")
    val isRead: Boolean
)

@Serializable
data class ShoppingItemInsert(
    @SerialName("house_id")
    val houseId: String,
    @SerialName("item_name")
    val itemName: String,
    @SerialName("quantity")
    val quantity: String? = null,
    @SerialName("added_by")
    val addedBy: String
)

@Serializable
data class ShoppingItemUpdate(
    @SerialName("is_purchased")
    val isPurchased: Boolean,
    @SerialName("purchased_by")
    val purchasedBy: String? = null,
    @SerialName("purchased_at")
    val purchasedAt: String? = null
)

@Serializable
data class ChoreInsert(
    @SerialName("house_id")
    val houseId: String,
    @SerialName("task_name")
    val taskName: String,
    @SerialName("description")
    val description: String? = null,
    @SerialName("due_date")
    val dueDate: String? = null,
    @SerialName("is_recurring")
    val isRecurring: Boolean = false,
    @SerialName("recurrence_pattern")
    val recurrencePattern: String? = null,
    @SerialName("assigned_to")
    val assignedTo: String? = null,
    @SerialName("created_by")
    val createdBy: String
)

@Serializable
data class ChoreUpdate(
    @SerialName("is_completed")
    val isCompleted: Boolean,
    @SerialName("completed_at")
    val completedAt: String? = null,
    @SerialName("completed_by")
    val completedBy: String? = null
)

@Serializable
data class MessageInsert(
    @SerialName("house_id")
    val houseId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("content")
    val content: String
)

@Serializable
data class ProfileUpdate(
    @SerialName("full_name")
    val fullName: String? = null,
    @SerialName("has_completed_onboarding")
    val hasCompletedOnboarding: Boolean? = null
)

