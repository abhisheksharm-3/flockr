package `in`.xroden.flockr.features.notifications.data

import `in`.xroden.flockr.core.domain.requireAuthenticated
import `in`.xroden.flockr.core.logging.Logger
import `in`.xroden.flockr.data.dto.NotificationUpdate
import `in`.xroden.flockr.data.dto.notification.DeleteAllNotificationsParams
import `in`.xroden.flockr.data.dto.notification.DeleteNotificationParams
import `in`.xroden.flockr.features.notifications.model.Notification
import `in`.xroden.flockr.features.notifications.model.NotificationPreference
import `in`.xroden.flockr.features.notifications.model.NotificationSerializer
import `in`.xroden.flockr.features.notifications.service.NotificationService
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val notificationService: NotificationService
) : INotificationRepository {

    private val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    // App-lifetime scope for channel teardown that must outlive a cancelled flow collector.
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val lenientJson = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    override fun getNotificationsFlow(): Flow<Result<List<Notification>>> {
        val currentUserId = userId ?: return flowOf(Result.success(emptyList()))

        return callbackFlow {
            // Use unique channel ID to prevent reuse of already-subscribed channels
            val channelId = "notifications_${currentUserId}_${UUID.randomUUID()}"
            val channel = supabase.realtime.channel(channelId)

            try {
                // Set up change flow BEFORE subscribing (required by Supabase SDK)
                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "notifications"
                    filter(FilterOperation("user_id", FilterOperator.EQ, currentUserId))
                }

                // Subscribe to channel
                channel.subscribe(blockUntilSubscribed = true)

                // Fetch and send initial data AFTER subscription
                val initialList = fetchNotifications(currentUserId)
                send(Result.success(initialList))

                // Collect changes
                changeFlow.collect { action ->
                    if (action is PostgresAction.Insert) {
                        handleNewNotification(action)
                    }
                    delay(100)
                    val updatedList = fetchNotifications(currentUserId)
                    send(Result.success(updatedList))
                }
            } catch (e: Exception) {
                send(Result.failure(e))
            }

            awaitClose {
                cleanupScope.launch { runCatching { supabase.realtime.removeChannel(channel) } }
            }
        }
    }

    private suspend fun fetchNotifications(userId: String): List<Notification> {
        val response = supabase.from("notifications")
            .select(Columns.ALL) {
                filter { eq("user_id", userId) }
                order("created_at", Order.DESCENDING)
            }
        return lenientJson.decodeFromString(ListSerializer(NotificationSerializer), response.data)
    }

    private suspend fun handleNewNotification(action: PostgresAction.Insert) {
        try {
            val record = action.record
            val notificationId = record["id"]?.jsonPrimitive?.content ?: ""
            val title = record["title"]?.jsonPrimitive?.content ?: "New Notification"
            val message = record["message"]?.jsonPrimitive?.content ?: ""
            val houseId = record["house_id"]?.jsonPrimitive?.content
            val typeStr = record["type"]?.jsonPrimitive?.content ?: "general"

            var shouldShow = true
            if (houseId != null) {
                val prefs = getNotificationPreferences(houseId).getOrNull()
                if (prefs != null) {
                    shouldShow = when (typeStr) {
                        "member_joined" -> prefs.enableMemberJoined
                        "expense", "expense_added", "per_diem" -> prefs.enableExpenseAdded
                        "chore", "chore_assigned" -> prefs.enableChoreAssigned
                        "message", "message_sent" -> prefs.enableMessageSent
                        "shopping", "shopping_item", "shopping_item_added" -> prefs.enableShoppingItemAdded
                        else -> true
                    }
                }
            }

            if (shouldShow) {
                notificationService.showNotification(
                    id = notificationId,
                    title = title,
                    message = message,
                    houseId = houseId,
                    type = typeStr,
                    data = null
                )
            }
        } catch (e: Exception) {
            Logger.w("NotificationRepository", "Failed to show device notification", e)
        }
    }

    override suspend fun markAsRead(notificationId: String): Result<Unit> = runCatching {
        supabase.from("notifications")
            .update(NotificationUpdate(isRead = true)) {
                filter { eq("id", notificationId) }
            }
    }

    override suspend fun markAllAsRead(): Result<Unit> = runCatching {
        requireAuthenticated(userId)
        supabase.postgrest.rpc("mark_all_notifications_read")
    }

    override suspend fun deleteNotification(notificationId: String): Result<Unit> = runCatching {
        supabase.postgrest.rpc(
            function = "delete_notification",
            parameters = DeleteNotificationParams(notificationId = notificationId)
        )
    }

    override suspend fun deleteAllNotifications(): Result<Unit> = runCatching {
        val currentUserId = requireAuthenticated(userId)
        supabase.postgrest.rpc(
            function = "delete_all_notifications",
            parameters = DeleteAllNotificationsParams(userId = currentUserId)
        )
    }

    override suspend fun getNotificationPreferences(houseId: String): Result<NotificationPreference?> = runCatching {
        val currentUserId = requireAuthenticated(userId)
        supabase.from("notification_preferences")
            .select(Columns.ALL) {
                filter {
                    eq("user_id", currentUserId)
                    eq("house_id", houseId)
                }
            }
            .decodeSingleOrNull<NotificationPreference>()
    }

    override suspend fun ensurePreferencesExist(houseId: String) {
        val currentUserId = userId ?: return
        runCatching {
            val existing = supabase.from("notification_preferences")
                .select(Columns.ALL) {
                    filter {
                        eq("user_id", currentUserId)
                        eq("house_id", houseId)
                    }
                }
                .decodeSingleOrNull<NotificationPreference>()

            if (existing == null) {
                supabase.from("notification_preferences").insert(
                    NotificationPreference(
                        id = UUID.randomUUID().toString(),
                        userId = currentUserId,
                        houseId = houseId,
                        enableMemberJoined = true,
                        enableExpenseAdded = true,
                        enableChoreAssigned = true,
                        enableMessageSent = true,
                        enableShoppingItemAdded = true
                    )
                )
            }
        }.onFailure { Logger.w("NotificationRepository", "Failed to ensure notification preferences exist", it) }
    }

    override suspend fun getNotificationPreferences(): List<NotificationPreference> {
        val currentUserId = userId ?: return emptyList()
        return runCatching {
            supabase.from("notification_preferences")
                .select(Columns.ALL) { filter { eq("user_id", currentUserId) } }
                .decodeList<NotificationPreference>()
        }.getOrElse { emptyList() }
    }

    override suspend fun updateNotificationPreferences(
        houseId: String,
        key: String,
        enabled: Boolean
    ): Result<Unit> = runCatching {
        val currentUserId = requireAuthenticated(userId)
        val column = PREFERENCE_KEY_MAP[key] ?: throw IllegalArgumentException("Invalid preference key: $key")

        supabase.from("notification_preferences")
            .update(mapOf(column to enabled)) {
                filter {
                    eq("user_id", currentUserId)
                    eq("house_id", houseId)
                }
            }
    }

    private companion object {
        val PREFERENCE_KEY_MAP = mapOf(
            "enable_member_joined" to "enable_member_joined",
            "enable_expense_added" to "enable_expense_added",
            "enable_chore_assigned" to "enable_chore_assigned",
            "enable_message_sent" to "enable_message_sent",
            "enable_shopping_item_added" to "enable_shopping_item_added"
        )
    }
}
