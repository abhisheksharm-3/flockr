package `in`.xroden.flockr.features.house.data

import `in`.xroden.flockr.core.domain.requireAuthenticated
import `in`.xroden.flockr.data.dto.HouseInvitationInsert
import `in`.xroden.flockr.data.dto.house.CancelInvitationParams
import `in`.xroden.flockr.data.dto.house.InvitationStatusUpdate
import `in`.xroden.flockr.data.dto.house.InviteCodeParam
import `in`.xroden.flockr.data.dto.house.JoinHouseResult
import `in`.xroden.flockr.data.dto.house.ResendInvitationParams
import `in`.xroden.flockr.data.dto.notification.NotificationInsertParams
import `in`.xroden.flockr.features.house.model.House
import `in`.xroden.flockr.features.house.model.HousePreview
import `in`.xroden.flockr.features.house.model.InvitationWithHouse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HouseInvitationRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val houseRepository: IHouseRepository
) : IHouseInvitationRepository {

    private val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    override suspend fun inviteMember(houseId: String, email: String): Result<Unit> = runCatching {
        val currentUserId = requireAuthenticated(userId)
        val house = houseRepository.getHouseById(houseId).getOrNull()
            ?: throw IllegalStateException("House not found")

        val inviteeProfile = supabase.from("profiles")
            .select(Columns.raw("id")) { filter { eq("email", email) } }
            .decodeSingleOrNull<kotlinx.serialization.json.JsonObject>()

        val inviteeUserId = inviteeProfile?.get("id")?.jsonPrimitive?.content

        supabase.from("house_invitations")
            .insert(HouseInvitationInsert(houseId = houseId, inviterId = currentUserId, inviteeEmail = email))

        if (inviteeUserId != null) {
            runCatching {
                supabase.from("notifications")
                    .insert(NotificationInsertParams(
                        userId = inviteeUserId,
                        houseId = houseId,
                        title = "House Invitation",
                        message = "You've been invited to join ${house.name}",
                        type = "house_invitation:${house.inviteCode ?: ""}"
                    ))
            }
        }
    }

    override suspend fun getHouseByInviteCode(inviteCode: String): Result<HousePreview?> = runCatching {
        supabase.postgrest.rpc(
            "get_house_by_invite_code_v2",
            parameters = InviteCodeParam(inviteCode.trim().uppercase())
        ).decodeSingleOrNull<HousePreview>()
    }

    override suspend fun joinHouseByInviteCode(inviteCode: String): Result<House> = runCatching {
        val joinResult = supabase.postgrest.rpc(
            "join_house_with_invite_code",
            parameters = InviteCodeParam(inviteCode.trim().uppercase())
        ).decodeAs<JoinHouseResult>()

        if (!joinResult.success) throw Exception(joinResult.error ?: "Unknown error")

        val houseId = joinResult.houseId ?: throw Exception("No house ID returned")
        houseRepository.getHouseById(houseId).getOrThrow()
            ?: throw Exception("Could not load house details")
    }

    override suspend fun getPendingInvitations(): Result<List<InvitationWithHouse>> = runCatching {
        userId ?: return@runCatching emptyList()
        supabase.postgrest.rpc("get_my_pending_invitations_with_details").decodeList<InvitationWithHouse>()
    }

    override suspend fun acceptInvitation(invitationId: String): Result<Unit> = runCatching {
        supabase.postgrest.rpc("accept_house_invitation", mapOf("p_invitation_id" to invitationId))
    }

    override suspend fun rejectInvitation(invitationId: String): Result<Unit> = runCatching {
        supabase.from("house_invitations")
            .update(InvitationStatusUpdate(status = "rejected")) { filter { eq("id", invitationId) } }
    }

    override suspend fun cancelInvitation(houseId: String, email: String): Result<Unit> = runCatching {
        supabase.postgrest.rpc(
            function = "cancel_invitation",
            parameters = CancelInvitationParams(houseId = houseId, email = email)
        )
    }

    override suspend fun resendInvitationNotification(houseId: String, email: String): Result<Unit> = runCatching {
        supabase.postgrest.rpc(
            function = "resend_invitation",
            parameters = ResendInvitationParams(houseId = houseId, email = email)
        )
    }
}
