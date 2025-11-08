package `in`.xroden.flockr.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class House(
    val id: String,
    val name: String,
    @SerialName("owner_id")
    val ownerId: String,
    @SerialName("invite_code")
    val inviteCode: String? = null,
    @SerialName("address")
    val address: String? = null,
    @SerialName("latitude")
    val latitude: Double? = null,
    @SerialName("longitude")
    val longitude: Double? = null,
    @SerialName("header_image_url")
    val headerImageUrl: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class HouseMember(
    val id: String,
    @SerialName("house_id")
    val houseId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("role")
    val role: String = "Member", // Owner, Admin, Member
    @SerialName("joined_at")
    val joinedAt: String
)

@Serializable
data class HouseWithMembers(
    val house: House,
    val members: List<Profile>
)

@Serializable
data class HouseInvitation(
    val id: String,
    @SerialName("house_id")
    val houseId: String,
    @SerialName("inviter_id")
    val inviterId: String,
    @SerialName("invitee_email")
    val inviteeEmail: String,
    val status: String = "pending", // pending, accepted, rejected
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class MemberWithProfile(
    val userId: String,
    val fullName: String?,
    val email: String,
    val role: String = "Member",
    val joinedAt: String
)

@Serializable
data class HouseAuditLog(
    val id: String,
    @SerialName("house_id")
    val houseId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("action")
    val action: String, // member_added, member_removed, role_changed, house_updated, etc.
    @SerialName("target_user_id")
    val targetUserId: String? = null,
    @SerialName("details")
    val details: Map<String, String> = emptyMap(),
    @SerialName("created_at")
    val createdAt: String
)


