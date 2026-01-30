package `in`.xroden.flockr.features.house.data

import android.util.Log
import `in`.xroden.flockr.core.constants.AppConstants
import `in`.xroden.flockr.core.validation.Validators
import `in`.xroden.flockr.core.security.InputSanitizer
import `in`.xroden.flockr.data.dto.CreateHouseParams
import `in`.xroden.flockr.data.dto.CreateHouseResponse
import `in`.xroden.flockr.data.dto.DeleteHouseParams
import `in`.xroden.flockr.data.dto.GetHouseMembersParams
import `in`.xroden.flockr.data.dto.GetUserHouseIdsParams
import `in`.xroden.flockr.data.dto.HouseConfigUpdate
import `in`.xroden.flockr.data.dto.HouseIdResult
import `in`.xroden.flockr.data.dto.HouseMemberInsert
import `in`.xroden.flockr.data.dto.HouseUpdate
import `in`.xroden.flockr.data.dto.LeaveHouseParams
import `in`.xroden.flockr.data.enums.HouseMemberRole
import `in`.xroden.flockr.features.house.model.House
import `in`.xroden.flockr.features.house.model.HouseCardData
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.model.HousePreview
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Repository for core house management operations.
 * Handles house CRUD, members, and configuration.
 * 
 * @see HouseInvitationRepository for invitation operations
 * @see HouseAuditRepository for audit log operations
 */
@Singleton
class HouseRepository @Inject constructor(
    private val supabase: SupabaseClient
) : IHouseRepository {
    private val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    // In-memory cache for house configs with TTL
    private data class CachedConfig(
        val config: HouseConfig,
        val timestamp: Long
    )
    private val configCache = mutableMapOf<String, CachedConfig>()
    private val CONFIG_CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes

    override fun getCurrentUserId(): String? = userId

    @OptIn(FlowPreview::class)
    fun getHousesFlow(): Flow<Result<List<House>>> = callbackFlow {
        Log.d("HouseRepository", "getHousesFlow started")
        // Wait for user ID with retry - handles timing issues during auth state propagation
        var currentUserId: String? = null
        var retryCount = 0
        val maxRetries = 5
        val retryDelayMs = 200L

        while (currentUserId == null && retryCount < maxRetries) {
            currentUserId = userId
            if (currentUserId == null) {
                Log.d("HouseRepository", "User ID null, retry $retryCount/$maxRetries")
                retryCount++
                kotlinx.coroutines.delay(retryDelayMs)
            }
        }

        if (currentUserId == null) {
            Log.w("HouseRepository", "User ID still null after $maxRetries retries, returning empty list")
            send(Result.success(emptyList()))
            close()
            return@callbackFlow
        }

        Log.d("HouseRepository", "User ID obtained: $currentUserId")

        // Use unique channel ID to prevent reuse of already-subscribed channels
        val channelId = "houses_user_${currentUserId}_${java.util.UUID.randomUUID()}"
        val channel = supabase.realtime.channel(channelId)
        Log.d("HouseRepository", "Created realtime channel: $channelId")

        try {
            // Set up change flows BEFORE subscribing (required by Supabase SDK)
            val housesFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "houses"
            }

            val membersFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "house_members"
                filter(FilterOperation("user_id", FilterOperator.EQ, currentUserId))
            }

            // Subscribe to channel
            Log.d("HouseRepository", "Subscribing to realtime channel...")
            channel.subscribe(blockUntilSubscribed = true)
            Log.d("HouseRepository", "Subscribed to realtime channel")

            // Fetch and send initial data AFTER subscription
            Log.d("HouseRepository", "Fetching initial houses data...")
            val initialResult = getHouses()
            Log.d("HouseRepository", "Initial houses result: isSuccess=${initialResult.isSuccess}, count=${initialResult.getOrNull()?.size}")
            send(initialResult)

            merge(housesFlow, membersFlow)
                .debounce(AppConstants.REALTIME_DEBOUNCE_MS)
                .collect {
                    Log.d("HouseRepository", "Realtime update received, refetching houses")
                    send(getHouses())
                }
        } catch (e: Exception) {
            Log.e("HouseRepository", "Exception in getHousesFlow", e)
            send(Result.failure(e))
        }

        awaitClose {
            Log.d("HouseRepository", "Closing getHousesFlow, removing channel")
            launch { runCatching { supabase.realtime.removeChannel(channel) } }
        }
    }

    suspend fun getHouses(): Result<List<House>> = runCatching {
        val currentUserId = userId ?: return@runCatching emptyList()

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

    override suspend fun getHouseById(houseId: String): Result<House> = runCatching {
        supabase.from("houses")
            .select(Columns.ALL) {
                filter { eq("id", houseId) }
            }
            .decodeSingle<House>()
    }

    override suspend fun createHouse(
        name: String,
        address: String?,
        latitude: Double?,
        longitude: Double?,
        currencyCode: String,
        dateFormat: String,
        firstDayOfWeek: Int,
        timezone: String
    ): Result<House> = runCatching {
        val currentUserId = userId ?: throw IllegalStateException("No user logged in")

        val validatedName = Validators.validateHouseName(name).getOrThrow()
        val sanitizedName = InputSanitizer.sanitizeText(validatedName)
        val sanitizedAddress = address?.trim()?.takeIf { it.isNotBlank() }?.let { InputSanitizer.sanitizeText(it) }
        val inviteCode = generateInviteCode()

        val rpcResponseRaw = supabase.postgrest.rpc(
            function = "create_house_with_owner",
            parameters = CreateHouseParams(
                name = sanitizedName,
                ownerId = currentUserId,
                inviteCode = inviteCode,
                address = sanitizedAddress,
                latitude = latitude,
                longitude = longitude
            )
        ).data

        val rpcResponse = Json.decodeFromString<CreateHouseResponse>(rpcResponseRaw)

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

    suspend fun removeMemberFromHouse(houseId: String, userId: String): Result<Unit> = runCatching {
        supabase.postgrest.rpc(
            function = "remove_house_member",
            parameters = LeaveHouseParams(houseId = houseId, userId = userId)
        )
    }

    override suspend fun getHouseMembers(houseId: String): Result<List<MemberWithProfile>> = runCatching {
        supabase.postgrest.rpc(
            function = "get_house_members_with_profiles",
            parameters = GetHouseMembersParams(houseId = houseId)
        ).decodeList<MemberWithProfile>()
    }

    override suspend fun getHouseConfig(houseId: String): Result<HouseConfig> {
        // Check cache first
        val cached = configCache[houseId]
        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < CONFIG_CACHE_TTL_MS) {
            return Result.success(cached.config)
        }
        
        return runCatching {
            val config = supabase.from("house_config")
                .select(Columns.ALL) {
                    filter { eq("house_id", houseId) }
                }
                .decodeSingle<HouseConfig>()
            
            // Cache the result
            configCache[houseId] = CachedConfig(config, System.currentTimeMillis())
            config
        }
    }

    override suspend fun updateHouseConfig(
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
        // Invalidate cache on update
        configCache.remove(houseId)
    }

    suspend fun deleteHouse(houseId: String): Result<Unit> = runCatching {
        supabase.postgrest.rpc(
            function = "delete_house",
            parameters = DeleteHouseParams(houseId = houseId)
        )
    }

    suspend fun uploadHouseHeaderImage(houseId: String, byteArray: ByteArray): Result<String> = runCatching {
        val fileName = "header_${houseId}_${System.currentTimeMillis()}.jpg"
        val bucket = supabase.storage.from("house_headers")
        bucket.upload(fileName, byteArray) { upsert = true }
        val publicUrl = bucket.publicUrl(fileName)

        supabase.from("houses").update(
            mapOf("header_image_url" to publicUrl)
        ) {
            filter { eq("id", houseId) }
        }

        publicUrl
    }

    fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }

    override fun getUserHousesFlow(): Flow<Result<List<HouseCardData>>> {
        return getHousesFlow().map { result ->
            result.map { houses ->
                houses.map { house ->
                    HouseCardData(
                        house = house,
                        memberCount = 0 // Will be enriched later
                    )
                }
            }
        }
    }

    override suspend fun joinHouseByCode(inviteCode: String): Result<House?> = runCatching {
        // Validate invite code format
        val validatedCode = Validators.validateInviteCode(inviteCode).getOrThrow()
        
        val house = supabase.from("houses")
            .select(Columns.ALL) {
                filter { eq("invite_code", validatedCode) }
            }
            .decodeSingleOrNull<House>()

        if (house != null) {
            val currentUserId = userId ?: throw IllegalStateException("No user logged in")
            supabase.from("house_members")
                .insert(
                    HouseMemberInsert(
                        houseId = house.id,
                        userId = currentUserId
                    )
                )
        }
        house
    }

    override suspend fun getHouseByInviteCode(inviteCode: String): Result<HousePreview?> = runCatching {
        supabase.from("houses")
            .select(Columns.raw("id, name, header_image_url")) {
                filter { eq("invite_code", inviteCode.uppercase()) }
            }
            .decodeSingleOrNull<HousePreview>()
    }

    override suspend fun removeMember(houseId: String, userId: String): Result<Unit> = runCatching {
        supabase.from("house_members")
            .delete {
                filter {
                    eq("house_id", houseId)
                    eq("user_id", userId)
                }
            }
    }

    override suspend fun updateMemberRole(houseId: String, userId: String, role: HouseMemberRole): Result<Unit> = runCatching {
        supabase.from("house_members")
            .update(mapOf("role" to role.name.lowercase())) {
                filter {
                    eq("house_id", houseId)
                    eq("user_id", userId)
                }
            }
    }

    override suspend fun leaveHouse(houseId: String): Result<Unit> = runCatching {
        val currentUserId = userId ?: throw IllegalStateException("No user logged in")
        supabase.from("house_members")
            .delete {
                filter {
                    eq("house_id", houseId)
                    eq("user_id", currentUserId)
                }
            }
    }
}
