/** Sharing a live location with a house for a set time, and seeing who in it is sharing. */
package `in`.xroden.flockr.features.location.data

import `in`.xroden.flockr.core.network.RealtimeConnectionManager
import `in`.xroden.flockr.data.realtime.TableWatch
import `in`.xroden.flockr.data.realtime.liveQuery
import `in`.xroden.flockr.features.location.model.MemberLocation
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** How long a member can choose to share for, in minutes; the database accepts only these. */
enum class SharingLength(val minutes: Int, val label: String) {
    QUARTER_HOUR(15, "15 minutes"),
    HOUR(60, "1 hour"),
    WORKING_DAY(480, "8 hours"),
}

@Singleton
class LocationSharingRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val connectionManager: RealtimeConnectionManager,
) {
    fun getCurrentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    /** Everyone sharing with the house right now, the viewer included, kept current as points move, start and stop. */
    fun getLocationsFlow(houseId: String): Flow<Result<List<MemberLocation>>> =
        supabase.liveQuery(connectionManager, listOf(TableWatch("member_locations", "house_id", houseId))) {
            supabase.from("member_locations").select { filter { eq("house_id", houseId) } }.decodeList<MemberLocation>()
        }

    /** Starts sharing, or restarts it for [length] from now, at the given first fix. Returns when it ends, as the server set it. */
    suspend fun start(houseId: String, length: SharingLength, latitude: Double, longitude: Double, accuracyMeters: Float?): Result<Instant> = runCatching {
        supabase.postgrest.rpc(
            "start_location_sharing",
            buildJsonObject {
                put("p_house_id", houseId)
                put("p_minutes", length.minutes)
                put("p_latitude", latitude)
                put("p_longitude", longitude)
                put("p_accuracy", accuracyMeters)
            },
        ).decodeAs<String>().let(Instant::parse)
    }

    /** Moves the shared point. False once sharing has stopped or run out, which means the phone should stop too. */
    suspend fun update(houseId: String, latitude: Double, longitude: Double, accuracyMeters: Float?): Result<Boolean> = runCatching {
        supabase.postgrest.rpc(
            "update_my_location",
            buildJsonObject {
                put("p_house_id", houseId)
                put("p_latitude", latitude)
                put("p_longitude", longitude)
                put("p_accuracy", accuracyMeters)
            },
        ).decodeAs<Boolean>()
    }

    suspend fun stop(houseId: String): Result<Unit> = runCatching {
        supabase.postgrest.rpc("stop_location_sharing", buildJsonObject { put("p_house_id", houseId) })
        Unit
    }
}
