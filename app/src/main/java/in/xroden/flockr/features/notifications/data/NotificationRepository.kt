package `in`.xroden.flockr.features.notifications.data

import `in`.xroden.flockr.data.dto.NotificationUpdate
import `in`.xroden.flockr.features.notifications.model.Notification
import `in`.xroden.flockr.features.notifications.model.NotificationSerializer
import `in`.xroden.flockr.features.notifications.model.NotificationPreference
import `in`.xroden.flockr.features.notifications.service.NotificationService
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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.launch

@Singleton
class NotificationRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val notificationService: NotificationService
) {
    private val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    fun getNotificationsFlow(): Flow<Result<List<Notification>>> {
        val currentUserId = userId ?: return flowOf(Result.success(emptyList()))

        return callbackFlow {
            val channelId = "notifications_$currentUserId"
            val channel = supabase.realtime.channel(channelId)

            try {
                val initial = supabase.from("notifications")
                    .select(Columns.ALL) {
                        filter {
                            eq("user_id", currentUserId)
                        }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList(NotificationSerializer)

                send(Result.success(initial))

                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "notifications"
                    filter = "user_id=eq.$currentUserId"
                }

                channel.subscribe(blockUntilSubscribed = true)

                changeFlow.collect { action ->
                    if (action is PostgresAction.Insert) {
                        try {
                            val record = action.record
                            val notificationId = record["id"]?.jsonPrimitive?.content ?: ""
                            val title = record["title"]?.jsonPrimitive?.content ?: "New Notification"
                            val message = record["message"]?.jsonPrimitive?.content ?: ""
                            val houseId = record["house_id"]?.jsonPrimitive?.content
                            val typeStr = record["type"]?.jsonPrimitive?.content ?: "general"

                            // Check preferences before showing notification
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
                            // Ignore device notification errors
                        }
                    }

                    kotlinx.coroutines.delay(100)
                    val updated = supabase.from("notifications")
                        .select(Columns.ALL) {
                            filter {
                                eq("user_id", currentUserId)
                            }
                            order("created_at", Order.DESCENDING)
                        }
                        .decodeList(NotificationSerializer)

                    send(Result.success(updated))
                }
            } catch (e: Exception) {
                send(Result.failure(e))
            }

            awaitClose {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        supabase.realtime.removeChannel(channel)
                    } catch (e: Exception) {
                        // Ignore cleanup errors
                    }
                }
            }
        }
    }

    suspend fun getNotifications(): Result<List<Notification>> {
        return try {
            val currentUserId = userId ?: return Result.success(emptyList())

            val notifications = supabase.from("notifications")
                .select(Columns.ALL) {
                    filter {
                        eq("user_id", currentUserId)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList(NotificationSerializer)

            Result.success(notifications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            supabase.from("notifications")
                .update(NotificationUpdate(isRead = true)) {
                    filter {
                        eq("id", notificationId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAllAsRead(): Result<Unit> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            supabase.postgrest.rpc("mark_all_notifications_read")

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            @kotlinx.serialization.Serializable
            data class DeleteNotificationParams(
                @kotlinx.serialization.SerialName("p_notification_id")
                val notificationId: String
            )

            supabase.postgrest.rpc(
                function = "delete_notification",
                parameters = DeleteNotificationParams(notificationId = notificationId)
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAllNotifications(): Result<Unit> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))
            
            @kotlinx.serialization.Serializable
            data class DeleteAllParams(
                @kotlinx.serialization.SerialName("p_user_id")
                val userId: String
            )

            supabase.postgrest.rpc(
                function = "delete_all_notifications",
                parameters = DeleteAllParams(userId = currentUserId)
            )

            Result.success(Unit)
        } catch (e: Exception) {
            // Fallback: delete using delete with filter if RPC doesn't exist yet
            try {
                val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))
                supabase.from("notifications").delete {
                     filter {
                         eq("user_id", currentUserId)
                     }
                }
                Result.success(Unit)
            } catch (e2: Exception) {
                Result.failure(e2)
            }
        }
    }

    suspend fun getNotificationPreferences(houseId: String): Result<NotificationPreference?> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            val prefs = supabase.from("notification_preferences")
                .select(Columns.ALL) {
                    filter {
                        eq("user_id", currentUserId)
                        eq("house_id", houseId)
                    }
                }
                .decodeSingleOrNull<NotificationPreference>()

            Result.success(prefs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateNotificationPreferences(
        houseId: String,
        enableMemberJoined: Boolean?,
        enableExpenseAdded: Boolean?,
        enableChoreAssigned: Boolean?,
        enableMessageSent: Boolean?,
        enableShoppingItemAdded: Boolean?
    ): Result<Unit> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            @kotlinx.serialization.Serializable
            data class PreferenceUpdate(
                @kotlinx.serialization.SerialName("enable_member_joined")
                val enableMemberJoined: Boolean? = null,
                @kotlinx.serialization.SerialName("enable_expense_added")
                val enableExpenseAdded: Boolean? = null,
                @kotlinx.serialization.SerialName("enable_chore_assigned")
                val enableChoreAssigned: Boolean? = null,
                @kotlinx.serialization.SerialName("enable_message_sent")
                val enableMessageSent: Boolean? = null,
                @kotlinx.serialization.SerialName("enable_shopping_item_added")
                val enableShoppingItemAdded: Boolean? = null
            )

            supabase.from("notification_preferences")
                .update(
                    PreferenceUpdate(
                        enableMemberJoined = enableMemberJoined,
                        enableExpenseAdded = enableExpenseAdded,
                        enableChoreAssigned = enableChoreAssigned,
                        enableMessageSent = enableMessageSent,
                        enableShoppingItemAdded = enableShoppingItemAdded
                    )
                ) {
                    filter {
                        eq("user_id", currentUserId)
                        eq("house_id", houseId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun ensurePreferencesExist(houseId: String) {
        try {
            val currentUserId = userId ?: return
            
            // Check if exists
            val existing = supabase.from("notification_preferences")
                .select(Columns.ALL) {
                    filter {
                        eq("user_id", currentUserId)
                        eq("house_id", houseId)
                    }
                }
                .decodeSingleOrNull<NotificationPreference>()
            
            if (existing == null) {
                // Create default
                val defaultPref = NotificationPreference(
                    userId = currentUserId,
                    houseId = houseId,
                    id = java.util.UUID.randomUUID().toString(),
                    enableMemberJoined = true,
                    enableExpenseAdded = true,
                    enableChoreAssigned = true,
                    enableMessageSent = true,
                    enableShoppingItemAdded = true
                )
                supabase.from("notification_preferences").insert(defaultPref)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    suspend fun getNotificationPreferences(): List<NotificationPreference> {
        return try {
            val currentUserId = userId ?: return emptyList()

            supabase.from("notification_preferences")
                .select(Columns.ALL) {
                    filter {
                        eq("user_id", currentUserId)
                    }
                }
                .decodeList<NotificationPreference>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateNotificationPreferences(
        houseId: String,
        key: String,
        enabled: Boolean
    ): Result<Unit> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            // Map key to column name if needed, or use dynamic update
            // Assuming key matches column name for simplicity, or map it
            val column = when(key) {
                "enable_member_joined" -> "enable_member_joined"
                "enable_expense_added" -> "enable_expense_added"
                "enable_chore_assigned" -> "enable_chore_assigned"
                "enable_message_sent" -> "enable_message_sent"
                "enable_shopping_item_added" -> "enable_shopping_item_added"
                else -> return Result.failure(Exception("Invalid preference key"))
            }

            // Use a map for update
            val updateMap = mapOf(column to enabled)

            supabase.from("notification_preferences")
                .update(updateMap) {
                    filter {
                        eq("user_id", currentUserId)
                        eq("house_id", houseId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
