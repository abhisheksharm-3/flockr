package `in`.xroden.flockr.data.repository

import `in`.xroden.flockr.data.model.House
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HouseRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id

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

            // Query houses where user is a member
            val houseMembers = supabase.from("house_members")
                .select(Columns.list("house_id")) {
                    filter {
                        eq("user_id", currentUserId)
                    }
                }
                .decodeList<Map<String, String>>()

            val houseIds = houseMembers.mapNotNull { it["house_id"] }
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

            android.util.Log.d("HouseRepository", "createHouse called - name='$name', address='$address', latitude=$latitude, longitude=$longitude, userId=$currentUserId")

            // Prepare insert payloads
            val responseList: List<Map<String, Any?>> = when {
                address != null && latitude != null && longitude != null -> {
                    @kotlinx.serialization.Serializable
                    data class HouseInsertFull(
                        val name: String,
                        val owner_id: String,
                        val address: String,
                        val latitude: Double,
                        val longitude: Double
                    )
                    supabase.from("houses")
                        .insert(HouseInsertFull(name, currentUserId, address, latitude, longitude)) {
                            select()
                        }
                        .decodeList()
                }
                address != null -> {
                    @kotlinx.serialization.Serializable
                    data class HouseInsertWithAddress(
                        val name: String,
                        val owner_id: String,
                        val address: String
                    )
                    supabase.from("houses")
                        .insert(HouseInsertWithAddress(name, currentUserId, address)) {
                            select()
                        }
                        .decodeList()
                }
                else -> {
                    @kotlinx.serialization.Serializable
                    data class HouseInsertBasic(
                        val name: String,
                        val owner_id: String
                    )
                    supabase.from("houses")
                        .insert(HouseInsertBasic(name, currentUserId)) {
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

            if (id == null) {
                android.util.Log.e("HouseRepository", "createHouse: returned row missing id: $row")
                return Result.failure(Exception("Insert returned row without id"))
            }

            val house = House(
                id = id,
                name = returnedName,
                ownerId = ownerIdFromRow,
                address = addr,
                latitude = lat,
                longitude = lon,
                createdAt = createdAt
            )

            android.util.Log.d("HouseRepository", "createHouse success - created house id=${house.id}, name=${house.name}, ownerId=${house.ownerId}")
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

    suspend fun addMemberToHouse(houseId: String, userId: String): Result<Unit> {
        return try {
            supabase.from("house_members")
                .insert(
                    mapOf(
                        "house_id" to houseId,
                        "user_id" to userId
                    )
                )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
