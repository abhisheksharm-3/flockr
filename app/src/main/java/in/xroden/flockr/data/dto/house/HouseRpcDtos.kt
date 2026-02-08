package `in`.xroden.flockr.data.dto.house

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Parameters for getting user's house IDs via RPC. */
@Serializable
data class GetUserHouseIdsParams(
    @SerialName("p_user_id") val userId: String
)

/** Result from get_user_house_ids RPC. */
@Serializable
data class HouseIdResult(
    @SerialName("house_id") val houseId: String
)

/** Parameters for creating a house via RPC. */
@Serializable
data class CreateHouseParams(
    @SerialName("p_name") val name: String,
    @SerialName("p_owner_id") val ownerId: String,
    @SerialName("p_invite_code") val inviteCode: String,
    @SerialName("p_address") val address: String?,
    @SerialName("p_latitude") val latitude: Double?,
    @SerialName("p_longitude") val longitude: Double?
)

/** Response from create_house_with_owner RPC. */
@Serializable
data class CreateHouseResponse(
    @SerialName("out_house_id") val houseId: String,
    @SerialName("out_house_name") val houseName: String,
    @SerialName("out_invite_code") val inviteCode: String
)

/** Parameters for leaving/removing from a house. */
@Serializable
data class LeaveHouseParams(
    @SerialName("p_house_id") val houseId: String,
    @SerialName("p_user_id") val userId: String
)

/** Parameters for joining house by invite code. */
@Serializable
data class InviteCodeParam(
    val code: String
)

/** Result from join house RPC. */
@Serializable
data class JoinHouseResult(
    val success: Boolean,
    val error: String? = null,
    @SerialName("house_id") val houseId: String? = null
)

/** Parameters for getting house members. */
@Serializable
data class GetHouseMembersParams(
    @SerialName("p_house_id") val houseId: String
)

/** Parameters for canceling an invitation. */
@Serializable
data class CancelInvitationParams(
    @SerialName("p_house_id") val houseId: String,
    @SerialName("p_email") val email: String
)

/** Parameters for resending an invitation. */
@Serializable
data class ResendInvitationParams(
    @SerialName("p_house_id") val houseId: String,
    @SerialName("p_email") val email: String
)

/** Parameters for deleting a house. */
@Serializable
data class DeleteHouseParams(
    @SerialName("p_house_id") val houseId: String
)

/** DTO for updating invitation status. */
@Serializable
data class InvitationStatusUpdate(
    val status: String
)
