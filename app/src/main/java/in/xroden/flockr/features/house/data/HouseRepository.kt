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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
                filter = "user_id=eq.$currentUserId"
            }

            channel.subscribe(blockUntilSubscribed = true)

            kotlinx.coroutines.flow.merge(housesFlow, membersFlow).collect {
                kotlinx.coroutines.delay(100)
                send(getHouses())
            }
        } catch (e: Exception) {
            send(Result.failure(e))
        }

        awaitClose {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    supabase.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
            }
        }
    }

    suspend fun getHouses(): Result<List<House>> {
        return try {
            val currentUserId = userId ?: return Result.success(emptyList())

            @Serializable
            data class GetUserHouseIdsParams(
                @SerialName("p_user_id")
                val userId: String
            )

            @Serializable
            data class HouseIdResult(
                @SerialName("house_id")
                val houseId: String
            )

            val houseMembers = supabase.postgrest.rpc(
                function = "get_user_house_ids",
                parameters = GetUserHouseIdsParams(userId = currentUserId)
            ).decodeList<HouseIdResult>()

            val houseIds = houseMembers.map { it.houseId }

            if (houseIds.isEmpty()) {
                return Result.success(emptyList())
            }

            val houses = supabase.from("houses")
                .select(Columns.ALL) {
                    filter {
                        isIn("id", houseIds)
                    }
                }
                .decodeList<House>()

            Result.success(houses)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHouseById(houseId: String): Result<House?> {
        return try {
            val house = supabase.from("houses")
                .select(Columns.ALL) {
                    filter {
                        eq("id", houseId)
                    }
                }
                .decodeSingleOrNull<House>()

            Result.success(house)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createHouse(
        name: String,
        address: String?,
        latitude: Double?,
        longitude: Double?,
        currencyCode: String = "USD"
    ): Result<House> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))
            val inviteCode = generateInviteCode()

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
            data class CreateHouseResponse(
                @SerialName("out_house_id")
                val houseId: String,
                @SerialName("out_house_name")
                val houseName: String,
                @SerialName("out_invite_code")
                val inviteCode: String
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

            if (currencyCode != "USD") {
                try {
                    supabase.from("house_config")
                        .update(
                            HouseConfigUpdate(currencyCode = currencyCode)
                        ) {
                            filter {
                                eq("house_id", rpcResponse.houseId)
                            }
                        }
                } catch (e: Exception) {
                    // Non-critical
                }
            }

            val house = supabase.from("houses")
                .select {
                    filter {
                        eq("id", rpcResponse.houseId)
                    }
                }
                .decodeSingle<House>()

            Result.success(house)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateHouse(
        houseId: String,
        name: String?,
        address: String?,
        latitude: Double?,
        longitude: Double?
    ): Result<Unit> {
        return try {
            supabase.from("houses")
                .update(
                    HouseUpdate(
                        name = name,
                        address = address,
                        latitude = latitude,
                        longitude = longitude
                    )
                ) {
                    filter {
                        eq("id", houseId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addMemberToHouse(
        houseId: String,
        userId: String,
        role: HouseMemberRole = HouseMemberRole.MEMBER
    ): Result<Unit> {
        return try {
            supabase.from("house_members")
                .insert(
                    HouseMemberInsert(
                        houseId = houseId,
                        userId = userId,
                        role = role
                    )
                )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeMemberFromHouse(houseId: String, userId: String): Result<Unit> {
        return try {
            @Serializable
            data class LeaveHouseParams(
                @SerialName("p_house_id")
                val houseId: String,
                @SerialName("p_user_id")
                val userId: String
            )

            supabase.postgrest.rpc(
                function = "remove_house_member",
                parameters = LeaveHouseParams(houseId = houseId, userId = userId)
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMemberRole(
        houseId: String,
        userId: String,
        newRole: HouseMemberRole
    ): Result<Unit> {
        return try {
            supabase.from("house_members")
                .update(
                    HouseMemberUpdate(role = newRole)
                ) {
                    filter {
                        eq("house_id", houseId)
                        eq("user_id", userId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun inviteMember(houseId: String, email: String): Result<Unit> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            val houseResult = getHouseById(houseId)
            val house =
                houseResult.getOrNull() ?: return Result.failure(Exception("House not found"))

            val inviteeProfile = supabase.from("profiles")
                .select(Columns.raw("id")) {
                    filter {
                        eq("email", email)
                    }
                }
                .decodeSingleOrNull<kotlinx.serialization.json.JsonObject>()

            val inviteeUserId = inviteeProfile?.get("id")?.jsonPrimitive?.content

            supabase.from("house_invitations")
                .insert(
                    HouseInvitationInsert(
                        houseId = houseId,
                        inviterId = currentUserId,
                        inviteeEmail = email
                    )
                )

            if (inviteeUserId != null) {
                try {
                    @Serializable
                    data class NotificationInsert(
                        @SerialName("user_id")
                        val userId: String,
                        @SerialName("house_id")
                        val houseId: String,
                        val title: String,
                        val message: String,
                        val type: String,
                        @SerialName("is_read")
                        val isRead: Boolean = false
                    )

                    supabase.from("notifications")
                        .insert(
                            NotificationInsert(
                                userId = inviteeUserId,
                                houseId = houseId,
                                title = "House Invitation",
                                message = "You've been invited to join ${house.name}",
                                type = "house_invitation"
                            )
                        )
                } catch (e: Exception) {
                    // Non-critical
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHouseByInviteCode(inviteCode: String): Result<House?> {
        return try {
            val trimmedCode = inviteCode.trim().uppercase()

            @Serializable
            data class InviteCodeParam(val code: String)

            @Serializable
            data class MinimalHouseResult(
                val id: String,
                val name: String,
                @SerialName("header_image_url")
                val headerImageUrl: String?
            )

            val result = supabase.postgrest.rpc(
                "get_house_by_invite_code",
                parameters = InviteCodeParam(trimmedCode)
            ).decodeSingleOrNull<MinimalHouseResult>()

            val house = result?.let {
                House(
                    id = it.id,
                    name = it.name,
                    ownerId = "",
                    inviteCode = null,
                    address = null,
                    latitude = null,
                    longitude = null,
                    createdAt = null,
                    updatedAt = null,
                    headerImageUrl = it.headerImageUrl
                )
            }

            Result.success(house)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinHouseByInviteCode(inviteCode: String): Result<House> {
        return try {
            val trimmedCode = inviteCode.trim().uppercase()

            @Serializable
            data class InviteCodeParam(val code: String)

            @Serializable
            data class JoinResult(
                val success: Boolean,
                val error: String? = null,
                @SerialName("house_id")
                val houseId: String? = null
            )

            val joinResult = supabase.postgrest.rpc(
                "join_house_with_invite_code",
                parameters = InviteCodeParam(trimmedCode)
            ).decodeAs<JoinResult>()

            if (!joinResult.success) {
                val errorMsg = joinResult.error ?: "Unknown error"
                return Result.failure(Exception(errorMsg))
            }

            val houseId =
                joinResult.houseId ?: return Result.failure(Exception("No house ID returned"))

            val houseResult = getHouseById(houseId)
            val house = houseResult.getOrNull()
                ?: return Result.failure(Exception("Could not load house details"))

            Result.success(house)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun getHouseMembers(houseId: String): Result<List<MemberWithProfile>> {
        return try {
            android.util.Log.d(
                "HouseRepository",
                "getHouseMembers called - houseId: $houseId"
            )
            @Serializable
            data class GetMembersParams(
                @SerialName("p_house_id")
                val houseId: String
            )

            val members = supabase.postgrest.rpc(
                function = "get_house_members_with_profiles",
                parameters = GetMembersParams(houseId = houseId)
            ).decodeList<MemberWithProfile>()

            android.util.Log.d(
                "HouseRepository",
                "getHouseMembers result: ${members.size} members"
            )
            Result.success(members)
        } catch (e: Exception) {
            android.util.Log.e("HouseRepository", "getHouseMembers failed", e)
            Result.failure(e)
        }
    }

    suspend fun getHouseConfig(houseId: String): Result<HouseConfig?> {
        return try {
            val config = supabase.from("house_config")
                .select(Columns.ALL) {
                    filter {
                        eq("house_id", houseId)
                    }
                }
                .decodeSingleOrNull<HouseConfig>()

            Result.success(config)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateHouseConfig(
        houseId: String,
        currencyCode: String?,
        dateFormat: String?,
        firstDayOfWeek: Int?,
        timezone: String?
    ): Result<Unit> {
        return try {
            supabase.from("house_config")
                .update(
                    HouseConfigUpdate(
                        currencyCode = currencyCode,
                        dateFormat = dateFormat,
                        firstDayOfWeek = firstDayOfWeek,
                        timezone = timezone
                    )
                ) {
                    filter {
                        eq("house_id", houseId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPendingInvitations(): Result<List<HouseInvitation>> {
        return try {
            val currentUserId = userId ?: return Result.success(emptyList())

            val userProfile = supabase.from("profiles")
                .select(Columns.raw("email")) {
                    filter {
                        eq("id", currentUserId)
                    }
                }
                .decodeSingleOrNull<kotlinx.serialization.json.JsonObject>()

            val userEmail = userProfile?.get("email")?.jsonPrimitive?.content
                ?: return Result.success(emptyList())

            val invitations = supabase.from("house_invitations")
                .select(Columns.ALL) {
                    filter {
                        eq("invitee_email", userEmail)
                        eq("status", "pending")
                    }
                }
                .decodeList<HouseInvitation>()

            Result.success(invitations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptInvitation(invitationId: String): Result<Unit> {
        return try {
            val currentUserId =
                userId ?: return Result.failure(Exception("No user logged in"))

            val invitation = supabase.from("house_invitations")
                .select(Columns.ALL) {
                    filter {
                        eq("id", invitationId)
                    }
                }
                .decodeSingleOrNull<HouseInvitation>()
                ?: return Result.failure(Exception("Invitation not found"))

            supabase.from("house_members")
                .insert(
                    HouseMemberInsert(
                        houseId = invitation.houseId,
                        userId = currentUserId
                    )
                )

            @Serializable
            data class InvitationUpdate(
                val status: String
            )

            supabase.from("house_invitations")
                .update(InvitationUpdate(status = "accepted")) {
                    filter {
                        eq("id", invitationId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectInvitation(invitationId: String): Result<Unit> {
        return try {
            @Serializable
            data class InvitationUpdate(
                val status: String
            )

            supabase.from("house_invitations")
                .update(InvitationUpdate(status = "rejected")) {
                    filter {
                        eq("id", invitationId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHouseAuditLogs(houseId: String): List<`in`.xroden.flockr.features.house.model.HouseAuditLog> {
        return try {
            supabase.from("house_audit_logs")
                .select(Columns.ALL) {
                    filter {
                        eq("house_id", houseId)
                    }
                    order(column = "created_at", order = Order.DESCENDING)
                }
                .decodeList<`in`.xroden.flockr.features.house.model.HouseAuditLog>()
        } catch (e: Exception) {
            android.util.Log.e("HouseRepository", "Error fetching audit logs", e)
            emptyList()
        }
    }

    suspend fun cancelInvitation(houseId: String, email: String): Result<Unit> {
        return try {
            @Serializable
            data class CancelInviteParams(
                @SerialName("p_house_id")
                val houseId: String,
                @SerialName("p_email")
                val email: String
            )

            supabase.postgrest.rpc(
                function = "cancel_invitation",
                parameters = CancelInviteParams(houseId = houseId, email = email)
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resendInvitationNotification(houseId: String, email: String): Result<Unit> {
        return try {
            @Serializable
            data class ResendInviteParams(
                @SerialName("p_house_id")
                val houseId: String,
                @SerialName("p_email")
                val email: String
            )

            supabase.postgrest.rpc(
                function = "resend_invitation",
                parameters = ResendInviteParams(houseId = houseId, email = email)
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteHouse(houseId: String): Result<Unit> {
        return try {
            @Serializable
            data class DeleteHouseParams(
                @SerialName("p_house_id")
                val houseId: String
            )

            supabase.postgrest.rpc(
                function = "delete_house",
                parameters = DeleteHouseParams(houseId = houseId)
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }
}
