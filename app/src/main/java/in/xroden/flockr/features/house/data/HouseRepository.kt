package `in`.xroden.flockr.features.house.data

import `in`.xroden.flockr.data.dto.HouseConfigUpdate
import `in`.xroden.flockr.data.dto.HouseInsert
import `in`.xroden.flockr.data.dto.HouseInvitationInsert
import `in`.xroden.flockr.data.dto.HouseMemberInsert
import `in`.xroden.flockr.data.dto.HouseMemberUpdate
import `in`.xroden.flockr.data.dto.HouseUpdate
import `in`.xroden.flockr.data.enums.HouseMemberRole
import `in`.xroden.flockr.features.house.model.House
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.model.HouseInvitation
import `in`.xroden.flockr.features.house.model.InvitationWithHouse
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.merge
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.coroutines.launch

@Singleton
class HouseRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    fun getCurrentUserId(): String? = userId

    fun getHousesFlow(): Flow<Result<List<House>>> = callbackFlow {
        val currentUserId = userId
        if (currentUserId == null) {
            send(Result.success(emptyList()))
            close()
            return@callbackFlow
        }

        val channelId = "houses_user_$currentUserId"
        val channel = supabase.realtime.channel(channelId)

        try {
            send(getHouses())

            val housesFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "houses"
            }

            val membersFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "house_members"
                filter(FilterOperation("user_id", FilterOperator.EQ, currentUserId))
            }

            channel.subscribe(blockUntilSubscribed = true)

            merge(housesFlow, membersFlow).collect {
                // Debounce slightly to avoid duplicate updates from same event if caught by multiple filters
                kotlinx.coroutines.delay(100) 
                send(getHouses())
            }
        } catch (e: Exception) {
            send(Result.failure(e))
        }

        awaitClose {
            launch {
                try {
                    supabase.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    suspend fun getHouses(): Result<List<House>> = runCatching {
        val currentUserId = userId ?: return@runCatching emptyList()

        @Serializable
        data class GetUserHouseIdsParams(
            @SerialName("p_user_id") val userId: String
        )

        @Serializable
        data class HouseIdResult(
            @SerialName("house_id") val houseId: String
        )

        val houseMembers = supabase.postgrest.rpc(
            function = "get_user_house_ids",
            parameters = GetUserHouseIdsParams(userId = currentUserId)
        ).decodeList<HouseIdResult>()

        val houseIds = houseMembers.map { it.houseId }

        if (houseIds.isEmpty()) {
            emptyList()
        } else {
            supabase.from("houses")
                .select(Columns.ALL) {
                    filter { isIn("id", houseIds) }
                }
                .decodeList<House>()
        }
    }

    suspend fun getHouseById(houseId: String): Result<House?> = runCatching {
        supabase.from("houses")
            .select(Columns.ALL) {
                filter { eq("id", houseId) }
            }
            .decodeSingleOrNull<House>()
    }

    suspend fun createHouse(
        name: String,
        address: String?,
        latitude: Double?,
        longitude: Double?,
        currencyCode: String = "USD",
        dateFormat: String = "dd/MM/yyyy",
        firstDayOfWeek: Int = 1, // Monday
        timezone: String = "UTC"
    ): Result<House> = runCatching {
        val currentUserId = userId ?: throw IllegalStateException("No user logged in")
        val inviteCode = generateInviteCode()

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

        val rpcResponseRaw = supabase.postgrest.rpc(
            function = "create_house_with_owner",
            parameters = CreateHouseParams(
                name = name,
                ownerId = currentUserId,
                inviteCode = inviteCode,
                address = address,
                latitude = latitude,
                longitude = longitude
            )
        ).data

        val rpcResponse = Json.decodeFromString<CreateHouseResponse>(rpcResponseRaw)

        // Best effort config update
        runCatching {
            supabase.from("house_config")
                .update(
                    HouseConfigUpdate(
                        currencyCode = currencyCode,
                        dateFormat = dateFormat,
                        firstDayOfWeek = firstDayOfWeek,
                        timezone = timezone
                    )
                ) {
                    filter { eq("house_id", rpcResponse.houseId) }
                }
        }

        supabase.from("houses")
            .select {
                filter { eq("id", rpcResponse.houseId) }
            }
            .decodeSingle<House>()
    }

    suspend fun updateHouse(
        houseId: String,
        name: String?,
        address: String?,
        latitude: Double?,
        longitude: Double?
    ): Result<Unit> = runCatching {
        supabase.from("houses")
            .update(
                HouseUpdate(
                    name = name,
                    address = address,
                    latitude = latitude,
                    longitude = longitude
                )
            ) {
                filter { eq("id", houseId) }
            }
    }

    suspend fun addMemberToHouse(
        houseId: String,
        userId: String,
        role: HouseMemberRole = HouseMemberRole.MEMBER
    ): Result<Unit> = runCatching {
        supabase.from("house_members")
            .insert(
                HouseMemberInsert(
                    houseId = houseId,
                    userId = userId,
                    role = role
                )
            )
    }

    suspend fun removeMemberFromHouse(houseId: String, userId: String): Result<Unit> = runCatching {
        @Serializable
        data class LeaveHouseParams(
            @SerialName("p_house_id") val houseId: String,
            @SerialName("p_user_id") val userId: String
        )

        supabase.postgrest.rpc(
            function = "remove_house_member",
            parameters = LeaveHouseParams(houseId = houseId, userId = userId)
        )
    }

    suspend fun updateMemberRole(
        houseId: String,
        userId: String,
        newRole: HouseMemberRole
    ): Result<Unit> = runCatching {
        supabase.from("house_members")
            .update(
                HouseMemberUpdate(role = newRole)
            ) {
                filter {
                    eq("house_id", houseId)
                    eq("user_id", userId)
                }
            }
    }

    suspend fun inviteMember(houseId: String, email: String): Result<Unit> = runCatching {
        val currentUserId = userId ?: throw IllegalStateException("No user logged in")
        // We need the house details for the notification message
        val house = getHouseById(houseId).getOrNull() ?: throw IllegalStateException("House not found")

        val inviteeProfile = supabase.from("profiles")
            .select(Columns.raw("id")) {
                filter { eq("email", email) }
            }
            .decodeSingleOrNull<kotlinx.serialization.json.JsonObject>()

        val inviteeUserId = inviteeProfile?.get("id")?.jsonPrimitive?.content

        // Insert invitation
        supabase.from("house_invitations")
            .insert(
                HouseInvitationInsert(
                    houseId = houseId,
                    inviterId = currentUserId,
                    inviteeEmail = email
                )
            )

        // Best effort notification
        if (inviteeUserId != null) {
            runCatching {
                @Serializable
                data class NotificationInsert(
                    @SerialName("user_id") val userId: String,
                    @SerialName("house_id") val houseId: String,
                    val title: String,
                    val message: String,
                    val type: String,
                    @SerialName("is_read") val isRead: Boolean = false
                )

                supabase.from("notifications")
                    .insert(
                        NotificationInsert(
                            userId = inviteeUserId,
                            houseId = houseId,
                            title = "House Invitation",
                            message = "You've been invited to join ${house.name}",
                            type = "house_invitation:${house.inviteCode ?: ""}"
                        )
                    )
            }
        }
    }

    suspend fun getHouseByInviteCode(inviteCode: String): Result<`in`.xroden.flockr.features.house.model.HousePreview?> = runCatching {
        val trimmedCode = inviteCode.trim().uppercase()

        @Serializable
        data class InviteCodeParam(val code: String)

        supabase.postgrest.rpc(
            "get_house_by_invite_code_v2",
            parameters = InviteCodeParam(trimmedCode)
        ).decodeSingleOrNull<`in`.xroden.flockr.features.house.model.HousePreview>()
    }

    suspend fun joinHouseByInviteCode(inviteCode: String): Result<House> = runCatching {
        val trimmedCode = inviteCode.trim().uppercase()

        @Serializable
        data class InviteCodeParam(val code: String)

        @Serializable
        data class JoinResult(
            val success: Boolean,
            val error: String? = null,
            @SerialName("house_id") val houseId: String? = null
        )

        val joinResult = supabase.postgrest.rpc(
            "join_house_with_invite_code",
            parameters = InviteCodeParam(trimmedCode)
        ).decodeAs<JoinResult>()

        if (!joinResult.success) {
            throw Exception(joinResult.error ?: "Unknown error")
        }

        val houseId = joinResult.houseId ?: throw Exception("No house ID returned")
        // Return the full house object
        getHouseById(houseId).getOrThrow() ?: throw Exception("Could not load house details")
    }

    suspend fun getHouseMembers(houseId: String): Result<List<MemberWithProfile>> = runCatching {
        @Serializable
        data class GetMembersParams(
            @SerialName("p_house_id") val houseId: String
        )

        supabase.postgrest.rpc(
            function = "get_house_members_with_profiles",
            parameters = GetMembersParams(houseId = houseId)
        ).decodeList<MemberWithProfile>()
    }

    suspend fun getHouseConfig(houseId: String): Result<HouseConfig?> = runCatching {
        supabase.from("house_config")
            .select(Columns.ALL) {
                filter { eq("house_id", houseId) }
            }
            .decodeSingleOrNull<HouseConfig>()
    }

    suspend fun updateHouseConfig(
        houseId: String,
        currencyCode: String?,
        dateFormat: String?,
        firstDayOfWeek: Int?,
        timezone: String?
    ): Result<Unit> = runCatching {
        supabase.from("house_config")
            .update(
                HouseConfigUpdate(
                    currencyCode = currencyCode,
                    dateFormat = dateFormat,
                    firstDayOfWeek = firstDayOfWeek,
                    timezone = timezone
                )
            ) {
                filter { eq("house_id", houseId) }
            }
    }

    suspend fun getPendingInvitations(): Result<List<InvitationWithHouse>> = runCatching {
        val currentUserId = userId ?: return@runCatching emptyList()

        val userProfile = supabase.from("profiles")
            .select(Columns.raw("email")) {
                filter { eq("id", currentUserId) }
            }
            .decodeSingleOrNull<kotlinx.serialization.json.JsonObject>()

        val userEmail = userProfile?.get("email")?.jsonPrimitive?.content
            ?: return@runCatching emptyList()

        supabase.postgrest.rpc(
            "get_my_pending_invitations_with_details"
        ).decodeList<InvitationWithHouse>()
    }

    suspend fun acceptInvitation(invitationId: String): Result<Unit> = runCatching {
        supabase.postgrest.rpc(
            "accept_house_invitation",
            mapOf("p_invitation_id" to invitationId)
        )
    }

    suspend fun rejectInvitation(invitationId: String): Result<Unit> = runCatching {
        @Serializable
        data class InvitationUpdate(val status: String)

        supabase.from("house_invitations")
            .update(InvitationUpdate(status = "rejected")) {
                filter { eq("id", invitationId) }
            }
    }

    suspend fun getHouseAuditLogs(houseId: String): List<`in`.xroden.flockr.features.house.model.HouseAuditLog> {
        // We use try-catch here because this function returns a List (not Result),
        // and we want to return empty list on failure rather than crashing or throwing.
        return try {
            supabase.from("house_audit_log")
                .select(Columns.ALL) {
                    filter {
                        eq("house_id", houseId)
                    }
                    order(column = "created_at", order = Order.DESCENDING)
                }
                .decodeList<`in`.xroden.flockr.features.house.model.HouseAuditLog>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun cancelInvitation(houseId: String, email: String): Result<Unit> = runCatching {
        @Serializable
        data class CancelInviteParams(
            @SerialName("p_house_id") val houseId: String,
            @SerialName("p_email") val email: String
        )

        supabase.postgrest.rpc(
            function = "cancel_invitation",
            parameters = CancelInviteParams(houseId = houseId, email = email)
        )
    }

    suspend fun resendInvitationNotification(houseId: String, email: String): Result<Unit> = runCatching {
        @Serializable
        data class ResendInviteParams(
            @SerialName("p_house_id") val houseId: String,
            @SerialName("p_email") val email: String
        )

        supabase.postgrest.rpc(
            function = "resend_invitation",
            parameters = ResendInviteParams(houseId = houseId, email = email)
        )
    }

    suspend fun deleteHouse(houseId: String): Result<Unit> = runCatching {
        @Serializable
        data class DeleteHouseParams(
            @SerialName("p_house_id") val houseId: String
        )

        supabase.postgrest.rpc(
            function = "delete_house",
            parameters = DeleteHouseParams(houseId = houseId)
        )
    }

    suspend fun uploadHouseHeaderImage(houseId: String, byteArray: ByteArray): Result<String> = runCatching {
        val fileName = "header_${houseId}_${System.currentTimeMillis()}.jpg"
        val bucket = supabase.storage.from("house_headers")
        bucket.upload(fileName, byteArray, upsert = true)
        val publicUrl = bucket.publicUrl(fileName)

        supabase.from("houses").update(
            mapOf("header_image_url" to publicUrl)
        ) {
            filter { eq("id", houseId) }
        }

        publicUrl
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }
}
