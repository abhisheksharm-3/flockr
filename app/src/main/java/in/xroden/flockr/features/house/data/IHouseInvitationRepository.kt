package `in`.xroden.flockr.features.house.data

import `in`.xroden.flockr.features.house.model.House
import `in`.xroden.flockr.features.house.model.HousePreview
import `in`.xroden.flockr.features.house.model.InvitationWithHouse

interface IHouseInvitationRepository {
    suspend fun inviteMember(houseId: String, email: String): Result<Unit>
    suspend fun getHouseByInviteCode(inviteCode: String): Result<HousePreview?>
    suspend fun joinHouseByInviteCode(inviteCode: String): Result<House>
    suspend fun getPendingInvitations(): Result<List<InvitationWithHouse>>
    suspend fun acceptInvitation(invitationId: String): Result<Unit>
    suspend fun rejectInvitation(invitationId: String): Result<Unit>
    suspend fun cancelInvitation(houseId: String, email: String): Result<Unit>
    suspend fun resendInvitationNotification(houseId: String, email: String): Result<Unit>
}
