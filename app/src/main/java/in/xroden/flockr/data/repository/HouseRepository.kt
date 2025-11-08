package `in`.xroden.flockr.data.repository

import `in`.xroden.flockr.data.model.CreateHouseParams
import `in`.xroden.flockr.data.model.CreateHouseResponse
import `in`.xroden.flockr.data.model.GetUserHouseIdsParams
import `in`.xroden.flockr.data.model.House
import `in`.xroden.flockr.data.model.HouseConfig
import `in`.xroden.flockr.data.model.HouseConfigUpdate
import `in`.xroden.flockr.data.model.HouseInvitationInsert
import `in`.xroden.flockr.data.model.HouseMemberInsert
import `in`.xroden.flockr.data.model.HouseUpdate
import `in`.xroden.flockr.data.model.MemberWithProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HouseRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    fun getCurrentUserId(): String? = userId

    fun getHousesFlow(): Flow<List<House>> {
        return kotlinx.coroutines.flow.flow {
            // Emit initial value immediately
            android.util.Log.d("HouseRepository", "Emitting initial houses list")
            emit(getHouses())

            // Then listen for realtime updates
            val channelId = "houses_user_${userId}_${System.currentTimeMillis()}"
            val channel = supabase.realtime.channel(channelId)

            try {
                // Listen to both houses and house_members tables
                val housesFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "houses"
                }

                val membersFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "house_members"
                }

                // Subscribe to the channel
                android.util.Log.d("HouseRepository", "Subscribing to channel $channelId")
                channel.subscribe(blockUntilSubscribed = true)
                android.util.Log.d("HouseRepository", "Successfully subscribed to realtime updates")

                // Merge both flows
                kotlinx.coroutines.flow.merge(housesFlow, membersFlow).collect { action ->
                    android.util.Log.d("HouseRepository", "Realtime update received: $action")
                    // Small delay to ensure database consistency
                    kotlinx.coroutines.delay(100)
                    emit(getHouses())
                }
            } catch (e: Exception) {
                android.util.Log.e("HouseRepository", "Error in realtime subscription", e)
            } finally {
                try {
                    android.util.Log.d("HouseRepository", "Cleaning up channel")
                    supabase.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    android.util.Log.e("HouseRepository", "Error removing channel", e)
                }
            }
        }
    }

    suspend fun getHouses(): List<House> {
        return try {
            val currentUserId = userId ?: run {
                android.util.Log.d("HouseRepository", "No user ID, returning empty list")
                return emptyList()
            }

            android.util.Log.d("HouseRepository", "Fetching houses for user: $currentUserId")

            // Use RPC function to bypass RLS recursion issue
            // Instead of querying house_members directly, use the SECURITY DEFINER function
            @kotlinx.serialization.Serializable
            data class HouseIdResult(val house_id: String)

            val params = GetUserHouseIdsParams(userId = currentUserId)
            
            val houseMembers = supabase.postgrest.rpc(
                function = "get_user_house_ids",
                parameters = params
            ).decodeList<HouseIdResult>()

            val houseIds = houseMembers.map { it.house_id }
            android.util.Log.d("HouseRepository", "User is member of ${houseIds.size} houses: $houseIds")

            if (houseIds.isEmpty()) {
                android.util.Log.d("HouseRepository", "No houses found, returning empty list")
                return emptyList()
            }

            val houses = supabase.from("houses")
                .select(Columns.ALL) {
                    filter {
                        isIn("id", houseIds)
                    }
                }
                .decodeList<House>()

            android.util.Log.d("HouseRepository", "Successfully fetched ${houses.size} houses")
            houses
        } catch (e: Exception) {
            android.util.Log.e("HouseRepository", "Error fetching houses", e)
            emptyList()
        }
    }
    suspend fun getHouseById(houseId: String): House? {
        return try {
            supabase.from("houses")
                .select(Columns.ALL) {
                    filter {
                        eq("id", houseId)
                    }
                }
                .decodeSingle<House>()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createHouse(
        name: String,
        address: String?,
        latitude: Double?,
        longitude: Double?,
        currencyCode: String = "USD",
        currencySymbol: String = "$"
    ): Result<House> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            // Generate unique invite code (6 characters, alphanumeric)
            val inviteCode = generateInviteCode()

            android.util.Log.d("HouseRepository", "createHouse called - name='$name', address='$address', latitude=$latitude, longitude=$longitude, userId=$currentUserId, inviteCode=$inviteCode")

            // Use RPC function to create house
            android.util.Log.d("HouseRepository", "Calling create_house_with_owner RPC function")

            val params = CreateHouseParams(
                name = name,
                ownerId = currentUserId,
                inviteCode = inviteCode,
                address = address,
                latitude = latitude,
                longitude = longitude
            )

            // RPC returns a single object (not an array), so we need to handle the raw JSON
            val rpcResponseRaw = supabase.postgrest.rpc(
                function = "create_house_with_owner",
                parameters = params
            ).data

            // Decode the raw JSON string into our response object
            val rpcResponse = Json.decodeFromString<CreateHouseResponse>(rpcResponseRaw)

            android.util.Log.d("HouseRepository", "House created successfully via RPC: ${rpcResponse.houseId}")

            // Update house config with currency if not default USD
            if (currencyCode != "USD" || currencySymbol != "$") {
                android.util.Log.d("HouseRepository", "Updating house config with currency: $currencyCode ($currencySymbol)")
                try {
                    val configUpdate = HouseConfigUpdate(
                        currencyCode = currencyCode,
                        currencySymbol = currencySymbol
                    )
                    supabase.from("house_config")
                        .update(configUpdate) {
                            filter {
                                eq("house_id", rpcResponse.houseId)
                            }
                        }
                } catch (e: Exception) {
                    android.util.Log.e("HouseRepository", "Failed to update currency config (non-critical)", e)
                }
            }

            // Fetch the full house object
            val house = supabase.from("houses")
                .select {
                    filter {
                        eq("id", rpcResponse.houseId)
                    }
                }
                .decodeSingle<House>()

            android.util.Log.d("HouseRepository", "House object fetched: ${house.id}")
            Result.success(house)
        } catch (e: Exception) {
            android.util.Log.e("HouseRepository", "Error creating house", e)
            Result.failure(e)
        }
    }

    suspend fun addMemberToHouse(houseId: String, userId: String): Result<Unit> {
        return try {
            val member = HouseMemberInsert(
                houseId = houseId,
                userId = userId,
                role = "member"
            )
            supabase.from("house_members")
                .insert(member)
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("HouseRepository", "Error adding member", e)
            Result.failure(e)
        }
    }

    suspend fun removeMemberFromHouse(houseId: String, userId: String): Result<Unit> {
        return try {
            supabase.from("house_members")
                .delete {
                    filter {
                        eq("house_id", houseId)
                        eq("user_id", userId)
                    }
                }
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("HouseRepository", "Error removing member", e)
            Result.failure(e)
        }
    }

    suspend fun inviteMember(houseId: String, email: String): Result<Unit> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            android.util.Log.d("HouseRepository", "Inviting member: email=$email, houseId=$houseId")

            // Get the house name for notification
            val house = getHouseById(houseId) ?: return Result.failure(Exception("House not found"))

            // Get inviter's profile
            val inviterProfile = supabase.from("profiles")
                .select(Columns.ALL) {
                    filter {
                        eq("id", currentUserId)
                    }
                }
                .decodeSingleOrNull<kotlinx.serialization.json.JsonObject>()

            val inviterName = inviterProfile?.get("full_name")?.jsonPrimitive?.content ?: "Someone"

            // Check if user exists with this email
            val inviteeProfile = supabase.from("profiles")
                .select(Columns.raw("id")) {
                    filter {
                        eq("email", email)
                    }
                }
                .decodeSingleOrNull<kotlinx.serialization.json.JsonObject>()

            val inviteeUserId = inviteeProfile?.get("id")?.jsonPrimitive?.content

            // Insert invitation
            val invitation = HouseInvitationInsert(
                houseId = houseId,
                inviterId = currentUserId,
                inviteeEmail = email,
                status = "pending"
            )
            supabase.from("house_invitations")
                .insert(invitation)

            android.util.Log.d("HouseRepository", "Invitation inserted, creating notification for user: $inviteeUserId")

            // Create notification if the user exists
            if (inviteeUserId != null) {
                try {
                    val notificationData = kotlinx.serialization.json.buildJsonObject {
                        put("house_id", kotlinx.serialization.json.JsonPrimitive(houseId))
                        put("house_name", kotlinx.serialization.json.JsonPrimitive(house.name))
                        put("inviter_name", kotlinx.serialization.json.JsonPrimitive(inviterName))
                        put("invite_code", kotlinx.serialization.json.JsonPrimitive(house.inviteCode ?: ""))
                        put("action", kotlinx.serialization.json.JsonPrimitive("house_invitation"))
                    }

                    val notificationInsert = mapOf(
                        "user_id" to inviteeUserId,
                        "house_id" to houseId,
                        "title" to "House Invitation",
                        "message" to "$inviterName invited you to join ${house.name}",
                        "type" to "house_invitation",
                        "is_read" to false,
                        "data" to notificationData
                    )

                    supabase.from("notifications")
                        .insert(notificationInsert)

                    android.util.Log.d("HouseRepository", "Notification created successfully for user: $inviteeUserId")
                } catch (e: Exception) {
                    android.util.Log.e("HouseRepository", "Error creating notification (non-fatal)", e)
                    // Don't fail the invitation if notification fails
                }
            } else {
                android.util.Log.d("HouseRepository", "User not found with email $email, skipping notification")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("HouseRepository", "Error inviting member", e)
            Result.failure(e)
        }
    }

    suspend fun getHouseByInviteCode(inviteCode: String): House? {
        return try {
            val trimmedCode = inviteCode.trim().uppercase()
            android.util.Log.d("HouseRepository", "Looking up house by invite code: $trimmedCode")

            @kotlinx.serialization.Serializable
            data class InviteCodeParam(val code: String)

            @kotlinx.serialization.Serializable
            data class MinimalHouseResult(
                val id: String,
                val name: String,
                val header_image_url: String?
            )

            // Call safer RPC function that only returns minimal info
            val result = try {
                val response = supabase.postgrest.rpc("get_house_by_invite_code", parameters = InviteCodeParam(trimmedCode))
                    .decodeSingleOrNull<MinimalHouseResult>()

                response?.let {
                    House(
                        id = it.id,
                        name = it.name,
                        ownerId = "", // Not exposed for security
                        inviteCode = null,
                        address = null,
                        latitude = null,
                        longitude = null,
                        createdAt = null,
                        updatedAt = null,
                        headerImageUrl = it.header_image_url
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("HouseRepository", "RPC call failed", e)
                null
            }

            if (result == null) {
                android.util.Log.w("HouseRepository", "No house found with invite code: $trimmedCode")
            } else {
                android.util.Log.d("HouseRepository", "Found house: ${result.name} (${result.id})")
            }

            result
        } catch (e: Exception) {
            android.util.Log.e("HouseRepository", "Error looking up house by invite code", e)
            null
        }
    }

    suspend fun joinHouseByInviteCode(inviteCode: String): Result<House> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))
            val trimmedCode = inviteCode.trim().uppercase()

            android.util.Log.d("HouseRepository", "Attempting to join house with code: $trimmedCode")

            @kotlinx.serialization.Serializable
            data class InviteCodeParam(val code: String)

            @kotlinx.serialization.Serializable
            data class JoinResult(
                val success: Boolean,
                val error: String? = null,
                val house_id: String? = null
            )

            // Call the secure RPC function that handles joining
            val joinResult = supabase.postgrest.rpc("join_house_with_invite_code", parameters = InviteCodeParam(trimmedCode))
                .decodeSingle<JoinResult>()

            if (!joinResult.success) {
                val errorMsg = joinResult.error ?: "Unknown error"
                android.util.Log.w("HouseRepository", "Failed to join house: $errorMsg")
                return Result.failure(Exception(errorMsg))
            }

            val houseId = joinResult.house_id ?: return Result.failure(Exception("No house ID returned"))

            // Now fetch the full house details since we're a member
            val house = getHouseById(houseId)
                ?: return Result.failure(Exception("Could not load house details"))

            android.util.Log.d("HouseRepository", "Successfully joined house: ${house.name}")
            Result.success(house)
        } catch (e: Exception) {
            android.util.Log.e("HouseRepository", "Error joining house", e)
            Result.failure(e)
        }
    }


    suspend fun getHouseMembers(houseId: String): List<MemberWithProfile> {
        return try {
            android.util.Log.d("HouseRepository", "Fetching members for house: $houseId")

            val response = supabase.from("house_members")
                .select(Columns.raw("""
                    user_id,
                    role,
                    joined_at,
                    profiles!house_members_user_id_fkey(full_name, email)
                """.trimIndent())) {
                    filter {
                        eq("house_id", houseId)
                    }
                }
                .decodeList<kotlinx.serialization.json.JsonObject>()

            val members = response.mapNotNull { obj ->
                try {
                    val userId = obj["user_id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val role = obj["role"]?.jsonPrimitive?.content ?: "Member"
                    val joinedAt = obj["joined_at"]?.jsonPrimitive?.content ?: ""

                    val profiles = obj["profiles"]?.jsonObject ?: return@mapNotNull null
                    val fullName = profiles["full_name"]?.jsonPrimitive?.content
                    val email = profiles["email"]?.jsonPrimitive?.content ?: ""

                    MemberWithProfile(
                        userId = userId,
                        fullName = fullName,
                        email = email,
                        role = role,
                        joinedAt = joinedAt
                    )
                } catch (e: Exception) {
                    android.util.Log.e("HouseRepository", "Error parsing member", e)
                    null
                }
            }

            android.util.Log.d("HouseRepository", "Found ${members.size} members")
            members
        } catch (e: Exception) {
            android.util.Log.e("HouseRepository", "Error fetching house members", e)
            emptyList()
        }
    }

    suspend fun getPendingInvitations(houseId: String): List<`in`.xroden.flockr.data.model.HouseInvitation> {
        return try {
            android.util.Log.d("HouseRepository", "Fetching pending invitations for house: $houseId")
            supabase.from("house_invitations")
                .select(Columns.ALL) {
                    filter {
                        eq("house_id", houseId)
                        eq("status", "pending")
                    }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<`in`.xroden.flockr.data.model.HouseInvitation>()
        } catch (e: Exception) {
            android.util.Log.e("HouseRepository", "Error fetching pending invitations", e)
            emptyList()
        }
    }

    suspend fun cancelInvitation(invitationId: String): Result<Unit> {
        return try {
            android.util.Log.d("HouseRepository", "Cancelling invitation: $invitationId")

            // Actually delete the invitation instead of just updating status
            supabase.from("house_invitations")
                .delete {
                    filter {
                        eq("id", invitationId)
                    }
                }

            android.util.Log.d("HouseRepository", "Invitation deleted successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("HouseRepository", "Error cancelling invitation", e)
            Result.failure(e)
        }
    }

    suspend fun resendInvitationNotification(invitationId: String): Result<Unit> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            android.util.Log.d("HouseRepository", "Resending invitation notification: $invitationId")

            // Get invitation details
            val invitation = supabase.from("house_invitations")
                .select(Columns.ALL) {
                    filter {
                        eq("id", invitationId)
                    }
                }
                .decodeSingle<`in`.xroden.flockr.data.model.HouseInvitation>()

            // Verify the current user is the inviter
            if (invitation.inviterId != currentUserId) {
                return Result.failure(Exception("You can only resend your own invitations"))
            }

            // Get house and inviter information
            val house = getHouseById(invitation.houseId)
                ?: return Result.failure(Exception("House not found"))

            val inviterProfile = supabase.from("profiles")
                .select(Columns.ALL) {
                    filter {
                        eq("id", currentUserId)
                    }
                }
                .decodeSingleOrNull<kotlinx.serialization.json.JsonObject>()

            val inviterName = inviterProfile?.get("full_name")?.jsonPrimitive?.content ?: "Someone"

            // Check if invitee exists
            val inviteeProfile = supabase.from("profiles")
                .select(Columns.raw("id")) {
                    filter {
                        eq("email", invitation.inviteeEmail)
                    }
                }
                .decodeSingleOrNull<kotlinx.serialization.json.JsonObject>()

            val inviteeUserId = inviteeProfile?.get("id")?.jsonPrimitive?.content

            if (inviteeUserId != null) {
                // Create new notification
                val notificationInsert = mapOf(
                    "user_id" to inviteeUserId,
                    "house_id" to invitation.houseId,
                    "title" to "House Invitation",
                    "message" to "$inviterName invited you to join ${house.name}",
                    "type" to "house_invitation",
                    "is_read" to false,
                    "data" to """{"house_id":"${invitation.houseId}","inviter_name":"$inviterName","invitation_id":"$invitationId"}"""
                )

                supabase.from("notifications")
                    .insert(notificationInsert)

                android.util.Log.d("HouseRepository", "Notification resent successfully")
                Result.success(Unit)
            } else {
                android.util.Log.d("HouseRepository", "User not found with email ${invitation.inviteeEmail}")
                Result.failure(Exception("User with email ${invitation.inviteeEmail} not found"))
            }
        } catch (e: Exception) {
            android.util.Log.e("HouseRepository", "Error resending invitation notification", e)
            Result.failure(e)
        }
    }

    suspend fun acceptInvitation(invitationId: String): Result<House> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            android.util.Log.d("HouseRepository", "Accepting invitation: $invitationId")

            // Get invitation details
            val invitation = supabase.from("house_invitations")
                .select(Columns.ALL) {
                    filter {
                        eq("id", invitationId)
                    }
                }
                .decodeSingle<`in`.xroden.flockr.data.model.HouseInvitation>()

            // Verify the invitation is for the current user
            val currentUserEmail = supabase.auth.currentUserOrNull()?.email
            if (invitation.inviteeEmail != currentUserEmail) {
                return Result.failure(Exception("This invitation is not for you"))
            }

            // Check if user is already a member
            val existingMember = supabase.from("house_members")
                .select(Columns.ALL) {
                    filter {
                        eq("house_id", invitation.houseId)
                        eq("user_id", currentUserId)
                    }
                }
                .decodeSingleOrNull<Map<String, String>>()

            if (existingMember != null) {
                android.util.Log.d("HouseRepository", "User is already a member of this house")
                // Update invitation status to accepted anyway
                supabase.from("house_invitations")
                    .update(mapOf("status" to "accepted")) {
                        filter {
                        eq("id", invitationId)
                    }
                }

                val house = getHouseById(invitation.houseId)
                    ?: return Result.failure(Exception("House not found"))
                return Result.success(house)
            }

            // Add user as member
            android.util.Log.d("HouseRepository", "Adding user as member to house: ${invitation.houseId}")
            addMemberToHouse(invitation.houseId, currentUserId).getOrThrow()

            // Update invitation status
            supabase.from("house_invitations")
                .update(mapOf("status" to "accepted")) {
                    filter {
                        eq("id", invitationId)
                    }
                }

            val house = getHouseById(invitation.houseId)
                ?: return Result.failure(Exception("House not found"))

            android.util.Log.d("HouseRepository", "Successfully accepted invitation and joined house: ${house.name}")
            Result.success(house)
        } catch (e: Exception) {
            android.util.Log.e("HouseRepository", "Error accepting invitation", e)
            Result.failure(e)
        }
    }

    suspend fun getHouseConfig(houseId: String): HouseConfig? {
        return try {
            android.util.Log.d("HouseRepository", "Fetching config for house: $houseId")
            supabase.from("house_config")
                .select(Columns.ALL) {
                    filter {
                        eq("house_id", houseId)
                    }
                }
                .decodeSingleOrNull<HouseConfig>()
        } catch (e: Exception) {
            android.util.Log.e("HouseRepository", "Error fetching house config", e)
            null
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
            // Check if there are any updates
            if (name == null && address == null && latitude == null && longitude == null) {
                return Result.success(Unit)
            }

            val updates = HouseUpdate(
                name = name,
                address = address,
                latitude = latitude,
                longitude = longitude
            )

            supabase.from("houses")
                .update(updates) {
                    filter {
                        eq("id", houseId)
                    }
                }

            android.util.Log.d("HouseRepository", "House updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("HouseRepository", "Error updating house", e)
            Result.failure(e)
        }
    }

    suspend fun updateHouseConfig(
        houseId: String,
        currencyCode: String?,
        currencySymbol: String?,
        dateFormat: String?,
        firstDayOfWeek: Int?,
        timezone: String?
    ): Result<Unit> {
        return try {
            // Check if there are any updates
            if (currencyCode == null && currencySymbol == null && dateFormat == null &&
                firstDayOfWeek == null && timezone == null) {
                return Result.success(Unit)
            }

            val updates = HouseConfigUpdate(
                currencyCode = currencyCode,
                currencySymbol = currencySymbol,
                dateFormat = dateFormat,
                firstDayOfWeek = firstDayOfWeek,
                timezone = timezone
            )

            supabase.from("house_config")
                .update(updates) {
                    filter {
                        eq("house_id", houseId)
                    }
                }

            android.util.Log.d("HouseRepository", "House config updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("HouseRepository", "Error updating house config", e)
            Result.failure(e)
        }
    }

    suspend fun deleteHouse(houseId: String): Result<Unit> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            android.util.Log.d("HouseRepository", "deleteHouse: Checking ownership for houseId=$houseId, userId=$currentUserId")

            // Check if current user is the owner
            val house = supabase.from("houses")
                .select {
                    filter {
                        eq("id", houseId)
                    }
                }
                .decodeSingleOrNull<House>()

            if (house == null) {
                android.util.Log.e("HouseRepository", "deleteHouse: House not found")
                return Result.failure(Exception("House not found"))
            }

            if (house.ownerId != currentUserId) {
                android.util.Log.e("HouseRepository", "deleteHouse: User is not the owner")
                return Result.failure(Exception("Only the owner can delete this house"))
            }

            // Delete the house (cascade delete should handle related records)
            supabase.from("houses")
                .delete {
                    filter {
                        eq("id", houseId)
                    }
                }

            android.util.Log.d("HouseRepository", "deleteHouse: House deleted successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("HouseRepository", "deleteHouse: Error", e)
            Result.failure(e)
        }
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // Removed similar-looking characters
        return (1..6)
            .map { chars.random() }
            .joinToString("")
    }

    suspend fun getHouseAuditLogs(houseId: String): List<`in`.xroden.flockr.data.model.HouseAuditLog> {
        return try {
            android.util.Log.d("HouseRepository", "Fetching audit logs for house: $houseId")
            supabase.from("house_audit_log")
                .select(Columns.ALL) {
                    filter {
                        eq("house_id", houseId)
                    }
                    order("created_at", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<`in`.xroden.flockr.data.model.HouseAuditLog>()
        } catch (e: Exception) {
            android.util.Log.e("HouseRepository", "Error fetching audit logs", e)
            emptyList()
        }
    }
}
