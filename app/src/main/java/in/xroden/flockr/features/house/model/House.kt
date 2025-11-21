package `in`.xroden.flockr.features.house.model

import `in`.xroden.flockr.data.enums.HouseMemberRole
import `in`.xroden.flockr.data.enums.InvitationStatus
import `in`.xroden.flockr.data.serialization.InstantSerializer
import `in`.xroden.flockr.features.auth.model.Profile
import kotlinx.datetime.Instant
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
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant? = null,
    @SerialName("updated_at")
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant? = null
)

@Serializable
data class HouseMember(
    val id: String,
    @SerialName("house_id")
    val houseId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("role")
    val role: HouseMemberRole = HouseMemberRole.MEMBER,
    @SerialName("joined_at")
    @Serializable(with = InstantSerializer::class)
    val joinedAt: Instant
)

@Serializable
data class HouseWithMembers(
    val house: House,
    val members: List<Profile>
)

@Serializable
data class MemberWithProfile(
    @SerialName("user_id")
    val userId: String,
    @SerialName("full_name")
    val fullName: String?,
    val email: String,
    val role: HouseMemberRole = HouseMemberRole.MEMBER,
    @SerialName("joined_at")
    @Serializable(with = InstantSerializer::class)
    val joinedAt: Instant,
    @SerialName("avatar_url")
    val avatarUrl: String? = null
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
    val status: InvitationStatus = InvitationStatus.PENDING,
    @SerialName("created_at")
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant
)

@Serializable
data class HouseAuditLog(
    val id: String,
    @SerialName("house_id")
    val houseId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("action")
    val action: String,
    @SerialName("target_user_id")
    val targetUserId: String? = null,
    @SerialName("details")
    val details: Map<String, String> = emptyMap(),
    @SerialName("created_at")
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant
)
