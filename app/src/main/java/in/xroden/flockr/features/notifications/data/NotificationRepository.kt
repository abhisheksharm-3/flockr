package `in`.xroden.flockr.features.notifications.data

import `in`.xroden.flockr.data.model.CreateNotificationWithTypeParams
import `in`.xroden.flockr.data.model.NotificationInsert
import `in`.xroden.flockr.data.model.NotificationUpdate
import `in`.xroden.flockr.features.notifications.model.Notification
import `in`.xroden.flockr.features.notifications.service.NotificationService
import `in`.xroden.flockr.utils.FlockrLogger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val notificationService: NotificationService
) {
    private val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id
    
    companion object {
        private const val TAG = "NotificationRepository"
    }

    fun getNotificationsFlow(): Flow<List<Notification>> {
        val currentUserId = userId ?: run {
            FlockrLogger.e(TAG, "getNotificationsFlow: No user logged in")
            return kotlinx.coroutines.flow.flowOf(emptyList())
        }

        FlockrLogger.realtimeEvent(TAG, "getNotificationsFlow", "Starting for user=$currentUserId")
        return kotlinx.coroutines.flow.flow {
            val initialList = supabase.from("notifications")
                .select(Columns.ALL) {
                    filter {
                        eq("user_id", currentUserId)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<Notification>()

            FlockrLogger.d(TAG, "getNotificationsFlow: Emitting initial ${initialList.size} notifications")
            emit(initialList)

            val channelId = "notifications_${currentUserId}_${System.currentTimeMillis()}"
            val channel = supabase.realtime.channel(channelId)
            
            try {
                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "notifications"
                }
                
                FlockrLogger.realtimeEvent(TAG, "getNotificationsFlow", "Subscribing to channel $channelId")
                channel.subscribe(blockUntilSubscribed = true)
                FlockrLogger.realtimeEvent(TAG, "getNotificationsFlow", "Successfully subscribed")
                
                changeFlow.collect { action ->
                    FlockrLogger.realtimeEvent(TAG, "getNotificationsFlow", "Received update: $action")
                    
                    // CRITICAL: Trigger device notification for INSERT events
                    // This ensures notifications appear in the device panel, not just in-app
                    if (action is PostgresAction.Insert) {
                        try {
                            val record = action.record
                            val notificationId = record["id"]?.jsonPrimitive?.content ?: ""
                            val title = record["title"]?.jsonPrimitive?.content ?: "New Notification"
                            val message = record["message"]?.jsonPrimitive?.content ?: ""
                            val houseId = record["house_id"]?.jsonPrimitive?.content
                            val typeStr = record["type"]?.jsonPrimitive?.content
                            val dataStr = record["data"]?.toString()
                            
                            // Extract type from 'type' column or fall back to data JSON
                            val type = typeStr ?: dataStr?.let { json ->
                                try {
                                    val typeMatch = Regex(""""type"\s*:\s*"([^"]+)"""").find(json)
                                    typeMatch?.groupValues?.get(1) ?: "general"
                                } catch (e: Exception) {
                                    FlockrLogger.w(TAG, "Failed to extract type from data: $e")
                                    "general"
                                }
                            } ?: "general"
                            
                            FlockrLogger.d(TAG, "✅ Triggering device notification: id=$notificationId, title=$title, type=$type")

                            // Show device notification (will appear in system tray/notification panel)
                            notificationService.showNotification(
                                id = notificationId,
                                title = title,
                                message = message,
                                type = type,
                                houseId = houseId
                            )

                            FlockrLogger.d(TAG, "✅ Device notification triggered successfully")
                        } catch (e: Exception) {
                            FlockrLogger.e(TAG, "❌ Error triggering device notification", e)
                        }
                    }
                    
                    val updatedList = getNotifications()
                    FlockrLogger.d(TAG, "getNotificationsFlow: Emitting ${updatedList.size} notifications after update")
                    emit(updatedList)
                }
            } catch (e: Exception) {
                FlockrLogger.repoError(TAG, "getNotificationsFlow", e)
            } finally {
                try {
                    FlockrLogger.d(TAG, "getNotificationsFlow: Cleaning up channel")
                    supabase.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    FlockrLogger.e(TAG, "getNotificationsFlow: Error removing channel", e)
                }
            }
        }
    }

    suspend fun getNotifications(): List<Notification> {
        FlockrLogger.repoStart(TAG, "getNotifications", emptyMap())
        return try {
            val currentUserId = userId ?: run {
                FlockrLogger.e(TAG, "getNotifications: No user logged in")
                return emptyList()
            }

            val notifications = supabase.from("notifications")
                .select(Columns.ALL) {
                    filter {
                        eq("user_id", currentUserId)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<Notification>()
            FlockrLogger.repoSuccess(TAG, "getNotifications", "Found ${notifications.size} notifications")
            notifications
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "getNotifications", e)
            emptyList()
        }
    }

    suspend fun createNotificationForHouse(
        houseId: String,
        title: String,
        message: String,
        type: String,
        data: String? = null
    ): Result<Unit> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            val notificationParams = CreateNotificationWithTypeParams(
                houseId = houseId,
                title = title,
                message = message,
                type = type,
                data = data ?: "{}",
                excludeUserId = currentUserId
            )

            supabase.postgrest.rpc(
                function = "create_notification_for_house",
                parameters = notificationParams
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createNotificationForUser(
        targetUserId: String,
        houseId: String,
        title: String,
        message: String,
        type: String,
        data: String? = null
    ): Result<Unit> {
        return try {
            val notificationInsert = NotificationInsert(
                userId = targetUserId,
                houseId = houseId,
                title = title,
                message = message,
                type = type,
                isRead = false,
                data = data ?: "{}"
            )

            supabase.from("notifications")
                .insert(notificationInsert)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAllAsRead(): Result<Unit> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            val update = NotificationUpdate(isRead = true)

            supabase.from("notifications")
                .update(update) {
                    filter {
                        eq("user_id", currentUserId)
                        eq("is_read", false)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUnreadCountFlow(): Flow<Int> {
        val currentUserId = userId ?: return kotlinx.coroutines.flow.flowOf(0)

        return kotlinx.coroutines.flow.flow {
            val initialCount = getUnreadCount()
            emit(initialCount)

            val channel = supabase.realtime.channel("notifications_count_$currentUserId")
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "notifications"
            }.collect {
                emit(getUnreadCount())
            }
        }
    }

    suspend fun getUnreadCount(): Int {
        return try {
            val currentUserId = userId ?: return 0

            val notifications = supabase.from("notifications")
                .select(Columns.ALL) {
                    filter {
                        eq("user_id", currentUserId)
                        eq("is_read", false)
                    }
                }
                .decodeList<Notification>()

            notifications.size
        } catch (e: Exception) {
            0
        }
    }

    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            val update = NotificationUpdate(isRead = true)

            supabase.from("notifications")
                .update(update) {
                    filter {
                        eq("id", notificationId)
                    }
                }


            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNotificationPreferences(): List<`in`.xroden.flockr.features.notifications.model.NotificationPreference> {
        return try {
            val currentUserId = userId ?: run {
                FlockrLogger.e(TAG, "getNotificationPreferences: No user logged in")
                return emptyList()
            }

            supabase.from("notification_preferences")
                .select(Columns.ALL) {
                    filter {
                        eq("user_id", currentUserId)
                    }
                }
                .decodeList<`in`.xroden.flockr.features.notifications.model.NotificationPreference>()
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "getNotificationPreferences", e)
            emptyList()
        }
    }

    suspend fun updateNotificationPreference(
        houseId: String,
        key: String,
        enabled: Boolean
    ): Result<Unit> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            // Check if preferences exist for this user and house
            val existing = supabase.from("notification_preferences")
                .select(Columns.raw("id")) {
                    filter {
                        eq("user_id", currentUserId)
                        eq("house_id", houseId)
                    }
                }
                .decodeList<Map<String, String>>()

            if (existing.isEmpty()) {
                // Create default preferences first
                createDefaultPreferences(houseId).getOrThrow()
            }

            // Build update map dynamically
            val updateMap = mutableMapOf<String, Any>(key to enabled)

            supabase.from("notification_preferences")
                .update(updateMap) {
                    filter {
                        eq("user_id", currentUserId)
                        eq("house_id", houseId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "updateNotificationPreference", e)
            Result.failure(e)
        }
    }

    suspend fun createDefaultPreferences(houseId: String): Result<Unit> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            @kotlinx.serialization.Serializable
            data class NotificationPreferenceInsert(
                @kotlinx.serialization.SerialName("user_id")
                val userId: String,
                @kotlinx.serialization.SerialName("house_id")
                val houseId: String,
                @kotlinx.serialization.SerialName("enable_member_joined")
                val enableMemberJoined: Boolean = true,
                @kotlinx.serialization.SerialName("enable_expense_added")
                val enableExpenseAdded: Boolean = true,
                @kotlinx.serialization.SerialName("enable_chore_assigned")
                val enableChoreAssigned: Boolean = true,
                @kotlinx.serialization.SerialName("enable_message_sent")
                val enableMessageSent: Boolean = true,
                @kotlinx.serialization.SerialName("enable_shopping_item_added")
                val enableShoppingItemAdded: Boolean = true
            )

            val insert = NotificationPreferenceInsert(
                userId = currentUserId,
                houseId = houseId
            )

            supabase.from("notification_preferences")
                .insert(insert)

            FlockrLogger.d(TAG, "Created default notification preferences for house $houseId")
            Result.success(Unit)
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "createDefaultPreferences", e)
            Result.failure(e)
        }
    }

    suspend fun ensurePreferencesExist(houseId: String): Result<Unit> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            val existing = supabase.from("notification_preferences")
                .select(Columns.raw("id")) {
                    filter {
                        eq("user_id", currentUserId)
                        eq("house_id", houseId)
                    }
                }
                .decodeList<Map<String, String>>()

            if (existing.isEmpty()) {
                createDefaultPreferences(houseId)
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "ensurePreferencesExist", e)
            Result.failure(e)
        }
    }
}
