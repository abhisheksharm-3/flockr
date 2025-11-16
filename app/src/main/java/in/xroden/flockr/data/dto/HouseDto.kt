package `in`.xroden.flockr.data.dto

import `in`.xroden.flockr.data.enums.HouseMemberRole
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HouseInsert(
    val name: String,
    @SerialName("owner_id")
    val ownerId: String,
    @SerialName("invite_code")
    val inviteCode: String,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class HouseUpdate(
    val name: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("header_image_url")
    val headerImageUrl: String? = null
)

@Serializable
data class HouseMemberInsert(
    @SerialName("house_id")
    val houseId: String,
    @SerialName("user_id")
    val userId: String,
    val role: HouseMemberRole = HouseMemberRole.MEMBER
)

@Serializable
data class HouseMemberUpdate(
    val role: HouseMemberRole? = null,
    @SerialName("is_active")
    val isActive: Boolean? = null
)

@Serializable
data class HouseConfigUpdate(
    @SerialName("currency_code")
    val currencyCode: String? = null,
    @SerialName("date_format")
    val dateFormat: String? = null,
    @SerialName("first_day_of_week")
    val firstDayOfWeek: Int? = null,
    val timezone: String? = null
)

@Serializable
data class HouseInvitationInsert(
    @SerialName("house_id")
    val houseId: String,
    @SerialName("inviter_id")
    val inviterId: String,
    @SerialName("invitee_email")
    val inviteeEmail: String
)


