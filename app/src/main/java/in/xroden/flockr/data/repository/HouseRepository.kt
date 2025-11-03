package `in`.xroden.flockr.data.repository

import `in`.xroden.flockr.data.model.House
import `in`.xroden.flockr.data.model.HouseConfig
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HouseRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    fun getCurrentUserId(): String? = userId

    fun getHousesFlow(): Flow<List<House>> {
        return kotlinx.coroutines.flow.flow {
            // Emit initial value immediately
            android.util.Log.d("HouseRepository", "Emitting initial houses list")
            emit(getHouses())

            // Then listen for realtime updates
            val channel = supabase.realtime.channel("houses_channel")
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "houses"
            }.collect {
                android.util.Log.d("HouseRepository", "Realtime update received, fetching houses")
                emit(getHouses())
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

            val houseMembers = supabase.postgrest.rpc(
                "get_user_house_ids",
                mapOf("p_user_id" to currentUserId)
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
        longitude: Double?
    ): Result<House> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            // Generate unique invite code (6 characters, alphanumeric)
            val inviteCode = generateInviteCode()

            android.util.Log.d("HouseRepository", "createHouse called - name='$name', address='$address', latitude=$latitude, longitude=$longitude, userId=$currentUserId, inviteCode=$inviteCode")

            // Prepare insert payloads
            val responseList: List<Map<String, Any?>> = when {
                address != null && latitude != null && longitude != null -> {
                    @kotlinx.serialization.Serializable
                    data class HouseInsertFull(
                        val name: String,
                        val owner_id: String,
                        val invite_code: String,
                        val address: String,
                        val latitude: Double,
                        val longitude: Double
                    )
                    supabase.from("houses")
                        .insert(HouseInsertFull(name, currentUserId, inviteCode, address, latitude, longitude)) {
                            select()
                        }
                        .decodeList()
                }
                address != null -> {
                    @kotlinx.serialization.Serializable
                    data class HouseInsertWithAddress(
                        val name: String,
                        val owner_id: String,
                        val invite_code: String,
                        val address: String
                    )
                    supabase.from("houses")
                        .insert(HouseInsertWithAddress(name, currentUserId, inviteCode, address)) {
                            select()
                        }
                        .decodeList()
                }
                else -> {
                    @kotlinx.serialization.Serializable
                    data class HouseInsertBasic(
                        val name: String,
                        val owner_id: String,
                        val invite_code: String
                    )
                    supabase.from("houses")
                        .insert(HouseInsertBasic(name, currentUserId, inviteCode)) {
                            select()
                        }
                        .decodeList()
                }
            }

            android.util.Log.d("HouseRepository", "Raw insert response rows: ${responseList.size}")
            if (responseList.isEmpty()) {
                android.util.Log.e("HouseRepository", "createHouse: no rows returned from insert")
                return Result.failure(Exception("No rows returned from insert"))
            }

            val row = responseList[0]
            android.util.Log.d("HouseRepository", "createHouse: returned keys = ${row.keys}")

            // Extract fields with tolerant key names (snake_case or camelCase)
            val id = (row["id"] ?: row["ID"] ?: row["Id"])?.toString()
            val returnedName = (row["name"] ?: row["Name"])?.toString() ?: name
            val ownerIdFromRow = (row["owner_id"] ?: row["ownerId"] ?: row["ownerId"])?.toString() ?: currentUserId
            val addr = (row["address"] ?: row["Address"])?.toString()
            val lat = when (val v = row["latitude"] ?: row["Latitude"]) {
                is Number -> v.toDouble()
                is String -> v.toDoubleOrNull()
                else -> null
            }
            val lon = when (val v = row["longitude"] ?: row["Longitude"]) {
                is Number -> v.toDouble()
                is String -> v.toDoubleOrNull()
                else -> null
            }
            val createdAt = (row["created_at"] ?: row["createdAt"])?.toString()
            val returnedInviteCode = (row["invite_code"] ?: row["inviteCode"])?.toString()

            if (id == null) {
                android.util.Log.e("HouseRepository", "createHouse: returned row missing id: $row")
                return Result.failure(Exception("Insert returned row without id"))
            }

            val house = House(
                id = id,
                name = returnedName,
                ownerId = ownerIdFromRow,
                inviteCode = returnedInviteCode,
                address = addr,
                latitude = lat,
                longitude = lon,
                createdAt = createdAt
            )

            // Automatically add the creator as a member with Owner role
            try {
                addMemberToHouse(house.id, currentUserId, "Owner")
                android.util.Log.d("HouseRepository", "Added creator as Owner to house")
            } catch (e: Exception) {
                android.util.Log.e("HouseRepository", "Failed to add creator as member", e)
                // Continue anyway, the house was created
            }

            android.util.Log.d("HouseRepository", "createHouse success - created house id=${house.id}, name=${house.name}, inviteCode=${house.inviteCode}")
            Result.success(house)
        } catch (e: Exception) {
            android.util.Log.e("HouseRepository", "createHouse failed", e)
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
            val updates = mutableMapOf<String, Any?>()
            name?.let { updates["name"] = it }
            address?.let { updates["address"] = it }
            latitude?.let { updates["latitude"] = it }
            longitude?.let { updates["longitude"] = it }

            supabase.from("houses")
                .update(updates) {
                    filter {
                        eq("id", houseId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addMemberToHouse(houseId: String, userId: String, role: String = "Member"): Result<Unit> {
        return try {
            // Check if member already exists to avoid duplicate key error
            val existingMember = supabase.from("house_members")
                .select(Columns.list("id")) {
                    filter {
                        eq("house_id", houseId)
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<Map<String, String>>()

            if (existingMember != null) {
                android.util.Log.d("HouseRepository", "Member already exists in house, skipping insert")
                return Result.success(Unit)
            }

            supabase.from("house_members")
                .insert(
                    mapOf(
                        "house_id" to houseId,
                        "user_id" to userId,
                        "role" to role
                    )
                )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHouseMembers(houseId: String): List<`in`.xroden.flockr.data.model.MemberWithProfile> {
        return try {
            android.util.Log.d("HouseRepository", "Fetching house members for houseId: $houseId")

            val response = supabase.from("house_members")
                .select(Columns.raw("user_id, role, joined_at, profiles!inner(id, email, full_name)")) {
                    filter {
                        eq("house_id", houseId)
                    }
                }

            android.util.Log.d("HouseRepository", "Response received, attempting to decode")

            val result = response.decodeAs<JsonArray>()

            android.util.Log.d("HouseRepository", "Successfully decoded ${result.size} members")

            val members = result.mapNotNull { element ->
                val obj = element.jsonObject
                val userId = obj["user_id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val role = obj["role"]?.jsonPrimitive?.content ?: "Member"
                val joinedAt = obj["joined_at"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val profiles = obj["profiles"]?.jsonObject ?: return@mapNotNull null
                val email = profiles["email"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val fullName = profiles["full_name"]?.jsonPrimitive?.content

                android.util.Log.d("HouseRepository", "Parsed member: userId=$userId, role=$role, fullName=$fullName, email=$email")

                `in`.xroden.flockr.data.model.MemberWithProfile(
                    userId = userId,
                    fullName = fullName,
                    email = email,
                    role = role,
                    joinedAt = joinedAt
                )
            }

            android.util.Log.d("HouseRepository", "Returning ${members.size} members")
            members
        } catch (e: Exception) {
            android.util.Log.e("HouseRepository", "Error fetching house members for houseId: $houseId", e)
            android.util.Log.e("HouseRepository", "Exception type: ${e.javaClass.name}")
            android.util.Log.e("HouseRepository", "Exception message: ${e.message}")
            e.printStackTrace()
            emptyList()
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
            Result.failure(e)
        }
    }

    suspend fun inviteMember(houseId: String, email: String): Result<Unit> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            supabase.from("house_invitations")
                .insert(
                    mapOf(
                        "house_id" to houseId,
                        "inviter_id" to currentUserId,
                        "invitee_email" to email,
                        "status" to "pending"
                    )
                )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPendingInvitations(houseId: String): List<`in`.xroden.flockr.data.model.HouseInvitation> {
        return try {
            supabase.from("house_invitations")
                .select(Columns.ALL) {
                    filter {
                        eq("house_id", houseId)
                        eq("status", "pending")
                    }
                }
                .decodeList<`in`.xroden.flockr.data.model.HouseInvitation>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getHouseByInviteCode(inviteCode: String): House? {
        return try {
            android.util.Log.d("HouseRepository", "Looking up house by invite code: $inviteCode")
            val house = supabase.from("houses")
                .select(Columns.ALL) {
                    filter {
                        eq("invite_code", inviteCode.uppercase().trim())
                    }
                }
                .decodeSingleOrNull<House>()

            android.util.Log.d("HouseRepository", "House found: ${house?.name}")
            house
        } catch (e: Exception) {
            android.util.Log.e("HouseRepository", "Error looking up house by invite code", e)
            null
        }
    }

    suspend fun joinHouseByInviteCode(inviteCode: String): Result<House> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            android.util.Log.d("HouseRepository", "Attempting to join house with code: $inviteCode")

            // Find house by invite code
            val house = getHouseByInviteCode(inviteCode)
                ?: return Result.failure(Exception("Invalid invite code"))

            // Check if user is already a member
            val existingMember = supabase.from("house_members")
                .select(Columns.list("id")) {
                    filter {
                        eq("house_id", house.id)
                        eq("user_id", currentUserId)
                    }
                }
                .decodeSingleOrNull<Map<String, String>>()

            if (existingMember != null) {
                android.util.Log.d("HouseRepository", "User is already a member of this house")
                return Result.failure(Exception("You are already a member of this household"))
            }

            // Add user as member
            android.util.Log.d("HouseRepository", "Adding user as member to house: ${house.id}")
            addMemberToHouse(house.id, currentUserId).getOrThrow()

            android.util.Log.d("HouseRepository", "Successfully joined house: ${house.name}")
            Result.success(house)
        } catch (e: Exception) {
            android.util.Log.e("HouseRepository", "Error joining house by invite code", e)
            Result.failure(e)
        }
    }

    suspend fun getHouseConfig(houseId: String): HouseConfig? {
        return try {
            supabase.from("house_config")
                .select(Columns.ALL) {
                    filter {
                        eq("house_id", houseId)
                    }
                }
                .decodeSingleOrNull<HouseConfig>()
        } catch (e: Exception) {
            android.util.Log.e("HouseRepository", "Error getting house config", e)
            null
        }
    }

    suspend fun updateHouseConfig(
        houseId: String,
        currencyCode: String? = null,
        currencySymbol: String? = null,
        dateFormat: String? = null,
        firstDayOfWeek: Int? = null,
        timezone: String? = null
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>()
            currencyCode?.let { updates["currency_code"] = it }
            currencySymbol?.let { updates["currency_symbol"] = it }
            dateFormat?.let { updates["date_format"] = it }
            firstDayOfWeek?.let { updates["first_day_of_week"] = it }
            timezone?.let { updates["timezone"] = it }
            updates["updated_at"] = "now()"

            supabase.from("house_config")
                .update(updates) {
                    filter {
                        eq("house_id", houseId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("HouseRepository", "Error updating house config", e)
            Result.failure(e)
        }
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // Removed similar-looking characters
        return (1..6)
            .map { chars.random() }
            .joinToString("")
    }
}
