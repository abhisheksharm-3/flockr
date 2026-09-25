/** Joining a house: invite codes, and email invitations sent and received. */
package `in`.xroden.flockr.features.house.data

import `in`.xroden.flockr.core.validation.Validators
import `in`.xroden.flockr.features.house.model.House
import `in`.xroden.flockr.features.house.model.HouseInvitation
import `in`.xroden.flockr.features.house.model.HousePreview
import `in`.xroden.flockr.features.house.model.InvitationWithHouse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HouseInvitationRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val houseRepository: HouseRepository
) {

    /** The invitee is told in the app when they sign in with this email; the database sends the notification. */
    suspend fun inviteMember(houseId: String, email: String): Result<Unit> = runCatching {
        val invitee = Validators.validateEmail(email.trim().lowercase()).getOrThrow()
        supabase.postgrest.rpc(
            "invite_to_house",
            buildJsonObject {
                put("p_house_id", houseId)
                put("p_email", invitee)
            }
        )
    }

    suspend fun getSentInvitations(houseId: String): Result<List<HouseInvitation>> = runCatching {
        supabase.from("house_invitations").select {
            filter {
                eq("house_id", houseId)
                eq("status", "pending")
            }
            order("created_at", Order.DESCENDING)
        }.decodeList<HouseInvitation>()
    }

    suspend fun cancelInvitation(invitationId: String): Result<Unit> = runCatching {
        supabase.postgrest.rpc("cancel_invitation", buildJsonObject { put("p_invitation_id", invitationId) })
    }

    suspend fun getPendingInvitations(): Result<List<InvitationWithHouse>> = runCatching {
        supabase.postgrest.rpc("get_my_pending_invitations").decodeList<InvitationWithHouse>()
    }

    /** Accepting joins the house and returns it; declining returns null. */
    suspend fun respondToInvitation(invitationId: String, accept: Boolean): Result<House?> = runCatching {
        val houseId = supabase.postgrest.rpc(
            "respond_to_invitation",
            buildJsonObject {
                put("p_invitation_id", invitationId)
                put("p_accept", accept)
            }
        ).decodeAs<String>()
        if (accept) houseRepository.getHouseById(houseId).getOrThrow() else null
    }

    /** The house an invite code opens, or null when the code is wrong or has expired. */
    suspend fun getHouseByInviteCode(inviteCode: String): Result<HousePreview?> = runCatching {
        supabase.postgrest.rpc("preview_house_by_invite_code", buildJsonObject { put("p_code", normalized(inviteCode)) })
            .decodeList<HousePreview>()
            .firstOrNull()
    }

    suspend fun joinHouseByInviteCode(inviteCode: String): Result<House> = runCatching {
        val houseId = supabase.postgrest.rpc(
            "join_house_with_invite_code",
            buildJsonObject { put("p_code", normalized(inviteCode)) }
        ).decodeAs<String>()
        houseRepository.getHouseById(houseId).getOrThrow()
    }

    private fun normalized(inviteCode: String) = inviteCode.trim().uppercase()
}
