package `in`.xroden.flockr.features.house.data

import `in`.xroden.flockr.core.cache.CacheManager
import `in`.xroden.flockr.core.constants.AppConstants
import `in`.xroden.flockr.core.domain.DomainError
import `in`.xroden.flockr.core.domain.requireAuthenticated
import `in`.xroden.flockr.core.security.CodeGenerator
import `in`.xroden.flockr.core.security.InputSanitizer
import `in`.xroden.flockr.core.validation.Validators
import `in`.xroden.flockr.data.dto.HouseConfigUpdate
import `in`.xroden.flockr.data.dto.HouseMemberInsert
import `in`.xroden.flockr.data.dto.HouseUpdate
import `in`.xroden.flockr.data.dto.house.CreateHouseParams
import `in`.xroden.flockr.data.dto.house.CreateHouseResponse
import `in`.xroden.flockr.data.dto.house.DeleteHouseParams
import `in`.xroden.flockr.data.dto.house.GetHouseMembersParams
import `in`.xroden.flockr.data.dto.house.GetHousesEnrichedParams
import `in`.xroden.flockr.data.dto.house.GetUserHouseIdsParams
import `in`.xroden.flockr.data.dto.house.HouseEnrichedResult
import `in`.xroden.flockr.data.dto.house.HouseIdResult
import `in`.xroden.flockr.data.dto.house.LeaveHouseParams
import `in`.xroden.flockr.data.enums.HouseMemberRole
import `in`.xroden.flockr.core.storage.IStorageRepository
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import `in`.xroden.flockr.core.network.RealtimeConnectionManager

private const val CONFIG_CACHE_TTL_MS = 5 * 60 * 1000L

/** Repository for house management operations. */
@Singleton
class HouseRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val storageRepository: IStorageRepository,
    private val cacheManager: CacheManager,
    private val realtimeConnectionManager: RealtimeConnectionManager
) : IHouseRepository {

    private val authenticatedUserId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    override fun getCurrentUserId(): String? = authenticatedUserId

    @OptIn(FlowPreview::class)
    override fun getHousesFlow(): Flow<Result<List<House>>> {
        val userId = authenticatedUserId ?: return flowOf(Result.success(emptyList()))
        val channelId = "houses_user_$userId"

        return callbackFlow {
            val channel = realtimeConnectionManager.getOrCreateChannel(channelId)

            try {
                val housesFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "houses"
                }

                val membersFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "house_members"
                    filter(FilterOperation("user_id", FilterOperator.EQ, userId))
                }

                channel.subscribe(blockUntilSubscribed = true)
                send(getHouses())

                merge(housesFlow, membersFlow)
                    .debounce(AppConstants.REALTIME_DEBOUNCE_MS)
                    .collect { send(getHouses()) }
            } catch (e: Exception) {
                send(Result.failure(DomainError.HouseError.LoadFailed(e)))
            }

            awaitClose {
                // Remove on the manager's own scope; launching here would be cancelled with the flow.
                realtimeConnectionManager.removeChannelByIdAsync(channelId)
            }
        }
    }

    override suspend fun getHouses(): Result<List<House>> = runCatching {
        val userId = authenticatedUserId ?: return@runCatching emptyList()

        val houseMembers = supabase.postgrest.rpc(
            function = "get_user_house_ids",
            parameters = GetUserHouseIdsParams(userId = userId)
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

    override suspend fun getHousesEnriched(month: String): Result<List<HouseEnrichedResult>> = runCatching {
        val userId = authenticatedUserId ?: return@runCatching emptyList()
        supabase.postgrest.rpc(
            function = "get_houses_enriched",
            parameters = GetHousesEnrichedParams(userId = userId, month = month)
        ).decodeList<HouseEnrichedResult>()
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
        val userId = requireAuthenticated(authenticatedUserId)

        val validatedName = Validators.validateHouseName(name).getOrThrow()
        val sanitizedName = InputSanitizer.sanitizeText(validatedName)
        val sanitizedAddress = address?.trim()?.takeIf { it.isNotBlank() }
            ?.let { InputSanitizer.sanitizeText(it) }
        val sanitizedTimezone = InputSanitizer.sanitizeText(timezone)
        val sanitizedDateFormat = InputSanitizer.sanitizeText(dateFormat)
        val sanitizedCurrency = InputSanitizer.sanitizeText(currencyCode)
        val inviteCode = generateInviteCode()

        val rpcResponseRaw = supabase.postgrest.rpc(
            function = "create_house_with_owner",
            parameters = CreateHouseParams(
                name = sanitizedName,
                ownerId = userId,
                inviteCode = inviteCode,
                address = sanitizedAddress,
                latitude = latitude,
                longitude = longitude
            )
        ).data

        val rpcResponse = Json.decodeFromString<CreateHouseResponse>(rpcResponseRaw)

        supabase.from("house_config")
            .update(
                HouseConfigUpdate(
                    currencyCode = sanitizedCurrency,
                    dateFormat = sanitizedDateFormat,
                    firstDayOfWeek = firstDayOfWeek,
                    timezone = sanitizedTimezone
                )
            ) {
                filter { eq("house_id", rpcResponse.houseId) }
            }

        supabase.from("houses")
            .select {
                filter { eq("id", rpcResponse.houseId) }
            }
            .decodeSingle<House>()
    }

    override suspend fun updateHouse(
        houseId: String,
        name: String?,
        address: String?,
        latitude: Double?,
        longitude: Double?
    ): Result<Unit> = runCatching {
        val sanitizedName = name?.let { InputSanitizer.sanitizeText(it) }
        val sanitizedAddress = address?.let { InputSanitizer.sanitizeText(it) }

        supabase.from("houses")
            .update(
                HouseUpdate(
                    name = sanitizedName,
                    address = sanitizedAddress,
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

    override suspend fun getHouseConfig(houseId: String): Result<HouseConfig> = runCatching {
        cacheManager.getOrPut("house_config_$houseId", CONFIG_CACHE_TTL_MS) {
            supabase.from("house_config")
                .select(Columns.ALL) {
                    filter { eq("house_id", houseId) }
                }
                .decodeSingle<HouseConfig>()
        }
    }

    override suspend fun updateHouseConfig(
        houseId: String,
        currencyCode: String?,
        dateFormat: String?,
        firstDayOfWeek: Int?,
        timezone: String?
    ): Result<Unit> = runCatching {
        val sanitizedCurrency = currencyCode?.let { InputSanitizer.sanitizeText(it) }
        val sanitizedDateFormat = dateFormat?.let { InputSanitizer.sanitizeText(it) }
        val sanitizedTimezone = timezone?.let { InputSanitizer.sanitizeText(it) }

        supabase.from("house_config")
            .update(
                HouseConfigUpdate(
                    currencyCode = sanitizedCurrency,
                    dateFormat = sanitizedDateFormat,
                    firstDayOfWeek = firstDayOfWeek,
                    timezone = sanitizedTimezone
                )
            ) {
                filter { eq("house_id", houseId) }
            }
        cacheManager.invalidate("house_config_$houseId")
    }

    override suspend fun deleteHouse(houseId: String): Result<Unit> = runCatching {
        supabase.postgrest.rpc(
            function = "delete_house",
            parameters = DeleteHouseParams(houseId = houseId)
        )
        cacheManager.invalidate("house_config_$houseId")
    }

    override suspend fun uploadHouseHeaderImage(houseId: String, byteArray: ByteArray): Result<String> = runCatching {
        val fileName = "header_${houseId}_${System.currentTimeMillis()}.jpg"
        
        val publicUrl = storageRepository.uploadFile("house_headers", fileName, byteArray)
            .getOrThrow()

        supabase.from("houses").update(
            mapOf("header_image_url" to publicUrl)
        ) {
            filter { eq("id", houseId) }
        }

        publicUrl
    }

    override fun generateInviteCode(): String = CodeGenerator.generateInviteCode()

    override fun getUserHousesFlow(): Flow<Result<List<HouseCardData>>> {
        return getHousesFlow().map { result ->
            result.map { houses ->
                houses.map { house ->
                    HouseCardData(
                        house = house,
                        memberCount = 0
                    )
                }
            }
        }
    }

    override suspend fun joinHouseByCode(inviteCode: String): Result<House?> = runCatching {
        val validatedCode = Validators.validateInviteCode(inviteCode).getOrThrow()
        
        val house = supabase.from("houses")
            .select(Columns.ALL) {
                filter { eq("invite_code", validatedCode) }
            }
            .decodeSingleOrNull<House>()

        if (house != null) {
            val userId = requireAuthenticated(authenticatedUserId)
            supabase.from("house_members")
                .insert(
                    HouseMemberInsert(
                        houseId = house.id,
                        userId = userId
                    )
                )
        }
        house
    }

    override suspend fun getHouseByInviteCode(inviteCode: String): Result<HousePreview?> = runCatching {
        val sanitizedCode = InputSanitizer.sanitizeText(inviteCode).uppercase()
        supabase.from("houses")
            .select(Columns.raw("id, name, header_image_url")) {
                filter { eq("invite_code", sanitizedCode) }
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
        val userId = requireAuthenticated(authenticatedUserId)
        supabase.from("house_members")
            .delete {
                filter {
                    eq("house_id", houseId)
                    eq("user_id", userId)
                }
            }
    }
}
