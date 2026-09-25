/** Houses, their settings and their members, through the house RPCs and the columns members may edit. */
package `in`.xroden.flockr.features.house.data

import `in`.xroden.flockr.core.cache.CacheManager
import `in`.xroden.flockr.core.domain.requireAuthenticated
import `in`.xroden.flockr.core.network.RealtimeConnectionManager
import `in`.xroden.flockr.core.security.InputSanitizer
import `in`.xroden.flockr.core.storage.StorageRepository
import `in`.xroden.flockr.core.validation.Validators
import `in`.xroden.flockr.data.dto.HouseConfigUpdate
import `in`.xroden.flockr.data.dto.HouseUpdate
import `in`.xroden.flockr.data.enums.HouseMemberRole
import `in`.xroden.flockr.data.realtime.TableWatch
import `in`.xroden.flockr.data.realtime.liveQuery
import `in`.xroden.flockr.features.house.model.House
import `in`.xroden.flockr.features.house.model.HouseCardData
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

private const val CONFIG_CACHE_TTL_MS = 5 * 60 * 1000L

private const val HEADERS_BUCKET = "house_headers"

const val MAX_SPLIT_WEIGHT_DECIMALS = 3

private fun configCacheKey(houseId: String) = "house_config_$houseId"

@Singleton
class HouseRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val storageRepository: StorageRepository,
    private val cacheManager: CacheManager,
    private val realtimeConnectionManager: RealtimeConnectionManager
) {

    fun getCurrentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    fun getHousesFlow(): Flow<Result<List<HouseCardData>>> {
        val userId = getCurrentUserId() ?: return flowOf(Result.success(emptyList()))
        return supabase.liveQuery(
            realtimeConnectionManager,
            listOf(TableWatch("houses"), TableWatch("house_members", "user_id", userId), TableWatch("expenses"))
        ) { fetchHouses() }
    }

    suspend fun getHouses(): Result<List<HouseCardData>> = runCatching { fetchHouses() }

    private suspend fun fetchHouses(): List<HouseCardData> =
        supabase.postgrest.rpc("get_my_houses").decodeList<HouseCardData>()

    suspend fun getHouseById(houseId: String): Result<House> = runCatching {
        supabase.from("houses").select { filter { eq("id", houseId) } }.decodeSingle<House>()
    }

    suspend fun createHouse(
        name: String,
        address: String?,
        latitude: Double?,
        longitude: Double?,
        currencyCode: String,
        dateFormat: String,
        firstDayOfWeek: Int,
        timezone: String
    ): Result<House> = runCatching {
        requireAuthenticated(getCurrentUserId())
        val houseName = InputSanitizer.sanitizeText(Validators.validateHouseName(name).getOrThrow())
        val houseId = supabase.postgrest.rpc(
            "create_house",
            buildJsonObject {
                put("p_name", houseName)
                put("p_address", address?.let(InputSanitizer::sanitizeText)?.ifBlank { null })
                put("p_latitude", latitude)
                put("p_longitude", longitude)
                put("p_header_image_url", null as String?)
                put("p_currency_code", currencyCode)
                put("p_date_format", dateFormat)
                put("p_first_day_of_week", firstDayOfWeek)
                put("p_timezone", timezone)
            }
        ).decodeAs<String>()
        getHouseById(houseId).getOrThrow()
    }

    suspend fun updateHouse(
        houseId: String,
        name: String?,
        address: String?,
        latitude: Double?,
        longitude: Double?
    ): Result<Unit> = runCatching {
        val update = HouseUpdate(
            name = name?.let(InputSanitizer::sanitizeText),
            address = address?.let(InputSanitizer::sanitizeText),
            latitude = latitude,
            longitude = longitude
        )
        supabase.from("houses").update(update) { filter { eq("id", houseId) } }
    }

    suspend fun deleteHouse(houseId: String): Result<Unit> = runCatching {
        supabase.postgrest.rpc("delete_house", buildJsonObject { put("p_house_id", houseId) })
        cacheManager.invalidate(configCacheKey(houseId))
    }

    suspend fun getHouseMembers(houseId: String): Result<List<MemberWithProfile>> = runCatching {
        supabase.postgrest.rpc("get_house_members", buildJsonObject { put("p_house_id", houseId) })
            .decodeList<MemberWithProfile>()
    }

    suspend fun getHouseConfig(houseId: String): Result<HouseConfig> = runCatching {
        cacheManager.getOrPut(configCacheKey(houseId), CONFIG_CACHE_TTL_MS) {
            supabase.from("house_config").select { filter { eq("house_id", houseId) } }.decodeSingle<HouseConfig>()
        }
    }

    suspend fun updateHouseConfig(
        houseId: String,
        currencyCode: String?,
        dateFormat: String?,
        firstDayOfWeek: Int?,
        timezone: String?
    ): Result<Unit> = runCatching {
        val update = HouseConfigUpdate(currencyCode, dateFormat, firstDayOfWeek, timezone)
        supabase.from("house_config").update(update) { filter { eq("house_id", houseId) } }
        cacheManager.invalidate(configCacheKey(houseId))
    }

    /** Whether the house has recorded any money, after which its currency is fixed. */
    suspend fun hasRecordedMoney(houseId: String): Result<Boolean> = runCatching {
        listOf("expenses", "recurring_expenses", "per_diem_config").any { table ->
            supabase.from(table).select(Columns.raw("id")) {
                filter { eq("house_id", houseId) }
                limit(1)
            }.decodeList<JsonObject>().isNotEmpty()
        }
    }

    suspend fun removeMember(houseId: String, userId: String): Result<Unit> = runCatching {
        supabase.postgrest.rpc(
            "remove_house_member",
            buildJsonObject {
                put("p_house_id", houseId)
                put("p_user_id", userId)
            }
        )
    }

    /** Making someone the owner hands ownership over, and the current owner becomes an admin. */
    suspend fun updateMemberRole(houseId: String, userId: String, role: HouseMemberRole): Result<Unit> =
        runCatching {
            if (role == HouseMemberRole.OWNER) {
                supabase.postgrest.rpc(
                    "transfer_house_ownership",
                    buildJsonObject {
                        put("p_house_id", houseId)
                        put("p_new_owner_id", userId)
                    }
                )
            } else {
                supabase.from("house_members").update({ set("role", role) }) {
                    filter {
                        eq("house_id", houseId)
                        eq("user_id", userId)
                    }
                }
            }
        }

    /** The member's share when an expense is split by shares; the column holds up to three decimals and must be above zero. */
    suspend fun setDefaultSplitWeight(houseId: String, userId: String, weight: BigDecimal): Result<Unit> = runCatching {
        require(weight.signum() > 0 && weight.stripTrailingZeros().scale() <= MAX_SPLIT_WEIGHT_DECIMALS) { "Invalid split weight" }
        supabase.from("house_members").update({ set("default_split_weight", weight.toPlainString()) }) {
            filter {
                eq("house_id", houseId)
                eq("user_id", userId)
            }
        }
    }

    /** A fresh invite code, valid for seven days; the old one stops working. Admins only. */
    suspend fun regenerateInviteCode(houseId: String): Result<String> = runCatching {
        supabase.postgrest.rpc("regenerate_invite_code", buildJsonObject { put("p_house_id", houseId) }).decodeAs<String>()
    }

    suspend fun leaveHouse(houseId: String): Result<Unit> = runCatching {
        supabase.postgrest.rpc("leave_house", buildJsonObject { put("p_house_id", houseId) })
    }

    /** Stores the image under the house's own folder, which only its admins may write, and points the house at it. */
    suspend fun uploadHouseHeaderImage(houseId: String, byteArray: ByteArray): Result<String> = runCatching {
        val path = "$houseId/header_${System.currentTimeMillis()}.jpg"
        val publicUrl = storageRepository.uploadFile(HEADERS_BUCKET, path, byteArray).getOrThrow()
        supabase.from("houses").update(HouseUpdate(headerImageUrl = publicUrl)) { filter { eq("id", houseId) } }
        publicUrl
    }
}
