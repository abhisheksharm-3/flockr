package `in`.xroden.flockr.features.house.data

import `in`.xroden.flockr.data.enums.HouseMemberRole
import `in`.xroden.flockr.features.house.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for house management operations.
 * Enables easy mocking for unit tests.
 */
interface IHouseRepository {
    fun getUserHousesFlow(): Flow<Result<List<HouseCardData>>>
    suspend fun getHouseById(houseId: String): Result<House>
    suspend fun createHouse(
        name: String,
        address: String?,
        latitude: Double?,
        longitude: Double?,
        currencyCode: String,
        dateFormat: String,
        firstDayOfWeek: Int,
        timezone: String
    ): Result<House>
    suspend fun joinHouseByCode(inviteCode: String): Result<House?>
    suspend fun getHouseByInviteCode(inviteCode: String): Result<HousePreview?>
    suspend fun getHouseMembers(houseId: String): Result<List<MemberWithProfile>>
    suspend fun getHouseConfig(houseId: String): Result<HouseConfig>
    suspend fun updateHouseConfig(
        houseId: String,
        currencyCode: String?,
        dateFormat: String?,
        firstDayOfWeek: Int?,
        timezone: String?
    ): Result<Unit>
    suspend fun removeMember(houseId: String, userId: String): Result<Unit>
    suspend fun updateMemberRole(houseId: String, userId: String, role: HouseMemberRole): Result<Unit>
    suspend fun leaveHouse(houseId: String): Result<Unit>
    fun getCurrentUserId(): String?
}
