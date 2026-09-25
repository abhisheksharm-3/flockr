/** The signed-in member's notifications and which kinds they want, per house. */
package `in`.xroden.flockr.features.notifications.data

import `in`.xroden.flockr.core.network.RealtimeConnectionManager
import `in`.xroden.flockr.data.realtime.TableWatch
import `in`.xroden.flockr.data.realtime.liveQuery
import `in`.xroden.flockr.features.notifications.model.Notification
import `in`.xroden.flockr.features.notifications.model.NotificationType
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** How many notifications the inbox keeps loaded. */
private const val INBOX_SIZE = 100L

@Serializable
private data class PreferenceRow(val type: String, @SerialName("is_enabled") val isEnabled: Boolean)

@Singleton
class NotificationRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val connectionManager: RealtimeConnectionManager,
) {
    /** The newest notifications, kept current as the database writes more. */
    fun getNotificationsFlow(): Flow<Result<List<Notification>>> {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return flowOf(Result.success(emptyList()))
        return supabase.liveQuery(connectionManager, listOf(TableWatch("notifications", "user_id", userId))) {
            supabase.from("notifications").select {
                order("created_at", Order.DESCENDING)
                limit(INBOX_SIZE)
            }.decodeList<Notification>()
        }
    }

    suspend fun getNotification(id: String): Result<Notification?> = runCatching {
        supabase.from("notifications").select { filter { eq("id", id) } }.decodeSingleOrNull<Notification>()
    }

    /** Unread notifications created after [after], oldest first, which is the order to post them in. */
    suspend fun getUnreadAfter(after: Instant): Result<List<Notification>> = runCatching {
        supabase.from("notifications").select {
            filter {
                eq("is_read", false)
                gt("created_at", after.toString())
            }
            order("created_at", Order.ASCENDING)
            limit(INBOX_SIZE)
        }.decodeList<Notification>()
    }

    /** Marks [ids] as read, or every notification when [ids] is null. */
    suspend fun markRead(ids: List<String>?): Result<Unit> = runCatching {
        supabase.postgrest.rpc(
            "mark_notifications_read",
            buildJsonObject {
                if (ids == null) put("p_ids", JsonNull) else put("p_ids", buildJsonArray { ids.forEach { add(JsonPrimitive(it)) } })
            }
        )
    }

    suspend fun delete(id: String): Result<Unit> = runCatching {
        supabase.from("notifications").delete { filter { eq("id", id) } }
    }

    suspend fun deleteRead(): Result<Unit> = runCatching {
        supabase.from("notifications").delete { filter { eq("is_read", true) } }
    }

    /** Whether each kind is on for [houseId]. A kind with no stored choice is on. */
    suspend fun getPreferences(houseId: String): Result<Map<NotificationType, Boolean>> = runCatching {
        val stored = supabase.from("notification_preferences").select { filter { eq("house_id", houseId) } }
            .decodeList<PreferenceRow>()
            .associate { it.type to it.isEnabled }
        NotificationType.entries.filter { it != NotificationType.INVITATION }.associateWith { stored[it.key] ?: true }
    }

    suspend fun setPreference(houseId: String, type: NotificationType, isEnabled: Boolean): Result<Unit> = runCatching {
        supabase.postgrest.rpc(
            "set_notification_preference",
            buildJsonObject {
                put("p_house_id", houseId)
                put("p_type", type.key)
                put("p_enabled", isEnabled)
            }
        )
    }
}
