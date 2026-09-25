/** Houses, their members, invitations and activity, in the shapes the house RPCs and tables return. */
package `in`.xroden.flockr.features.house.model

import androidx.compose.runtime.Immutable
import `in`.xroden.flockr.data.enums.HouseMemberRole
import `in`.xroden.flockr.data.serialization.BigDecimalSerializer
import `in`.xroden.flockr.data.serialization.InstantSerializer
import java.math.BigDecimal
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Immutable
@Serializable
data class House(
    val id: String,
    val name: String,
    @SerialName("owner_id")
    val ownerId: String,
    @SerialName("invite_code")
    val inviteCode: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("header_image_url")
    val headerImageUrl: String? = null
)

/**
 * A member of a house, current or past. Past members stay on the roster so the expenses they were
 * part of still name them; [isActive] tells the two apart.
 *
 * [defaultSplitWeight] is the member's share when an expense is split by shares, such as 2 for a
 * couple sharing a room.
 */
@Immutable
@Serializable
data class MemberWithProfile(
    @SerialName("user_id")
    val userId: String,
    @SerialName("full_name")
    val fullName: String = "",
    val email: String,
    val role: HouseMemberRole = HouseMemberRole.MEMBER,
    @SerialName("joined_at")
    @Serializable(with = InstantSerializer::class)
    val joinedAt: Instant,
    @SerialName("left_at")
    @Serializable(with = InstantSerializer::class)
    val leftAt: Instant? = null,
    @SerialName("default_split_weight")
    @Serializable(with = BigDecimalSerializer::class)
    val defaultSplitWeight: BigDecimal = BigDecimal.ONE,
    @SerialName("avatar_url")
    val avatarUrl: String? = null
) {
    val isActive: Boolean get() = leftAt == null

    /** The member's name, or their email when they have not set one. */
    val displayName: String get() = fullName.ifBlank { email }

    /** The first word of [displayName], for lists where a full name would crowd the row. */
    val shortName: String get() = displayName.trim().substringBefore(' ')
}

/** "You" for the viewer, the member's first name otherwise, and "A former housemate" for someone no longer on the roster. */
fun Map<String, MemberWithProfile>.nameOf(userId: String?, viewerId: String): String = when (userId) {
    viewerId -> "You"
    null -> "Someone"
    else -> get(userId)?.shortName ?: "A former housemate"
}

/** As [nameOf], but "you" in lower case, for the middle of a sentence. */
fun Map<String, MemberWithProfile>.nameInSentence(userId: String?, viewerId: String): String =
    if (userId == viewerId) "you" else nameOf(userId, viewerId)

/** An invitation the house has sent and the invitee has not yet answered. */
@Immutable
@Serializable
data class HouseInvitation(
    val id: String,
    @SerialName("house_id")
    val houseId: String,
    @SerialName("invitee_email")
    val inviteeEmail: String,
    @SerialName("created_at")
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant
)

/** An invitation the signed-in user has received, with enough of the house to decide on it. */
@Immutable
@Serializable
data class InvitationWithHouse(
    val id: String,
    @SerialName("house_id")
    val houseId: String,
    @SerialName("house_name")
    val houseName: String,
    @SerialName("header_image_url")
    val headerImageUrl: String? = null,
    @SerialName("inviter_name")
    val inviterName: String,
    @SerialName("created_at")
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant
)

/** One entry in a house's activity log. [userId] is null once the member who acted has deleted their account. */
@Serializable
data class HouseAuditLog(
    val id: String,
    @SerialName("house_id")
    val houseId: String,
    @SerialName("user_id")
    val userId: String? = null,
    val action: String,
    @SerialName("target_user_id")
    val targetUserId: String? = null,
    val details: JsonObject = JsonObject(emptyMap()),
    @SerialName("created_at")
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant
)
