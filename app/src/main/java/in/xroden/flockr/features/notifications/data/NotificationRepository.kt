package `in`.xroden.flockr.features.notifications.data

import `in`.xroden.flockr.data.dto.NotificationUpdate
import `in`.xroden.flockr.features.notifications.model.Notification
import `in`.xroden.flockr.features.notifications.model.NotificationSerializer
import `in`.xroden.flockr.features.notifications.model.NotificationPreference
import `in`.xroden.flockr.features.notifications.service.NotificationService
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
                
                val initialResponse = initial
                val initialList = Json.decodeFromString(ListSerializer(NotificationSerializer), initialResponse.data)

                send(Result.success(initialList))

                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "notifications"
                    filter(FilterOperation("user_id", FilterOperator.EQ, currentUserId))
                }

                channel.subscribe(blockUntilSubscribed = true)

                changeFlow.collect { action ->
                    // Handle INSERT - show notification and refetch
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
                        } catch (_: Exception) {
                            // Ignore device notification errors
                        }
                    }

                    // Handle INSERT, UPDATE, and DELETE - all trigger a refetch for accurate unread count
                    delay(100)
                    val updated = supabase.from("notifications")
                        .select(Columns.ALL) {
                            filter {
                                eq("user_id", currentUserId)
                            }
                            order("created_at", Order.DESCENDING)
                        }
                    
                    val updatedList = Json.decodeFromString(ListSerializer(NotificationSerializer), updated.data)
                    
                    send(Result.success(updatedList))
                }
            } catch (e: Exception) {
                send(Result.failure(e))
            }

            awaitClose {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        supabase.realtime.removeChannel(channel)
                    } catch (_: Exception) {
                        // Ignore cleanup errors
                    }
                }
            }
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
            userId ?: return Result.failure(Exception("No user logged in"))

            supabase.postgrest.rpc("mark_all_notifications_read")

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            @Serializable
            data class DeleteNotificationParams(
                @SerialName("p_notification_id")
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
            
            @Serializable
            data class DeleteAllParams(
                @SerialName("p_user_id")
                val userId: String
            )

            supabase.postgrest.rpc(
                function = "delete_all_notifications",
                parameters = DeleteAllParams(userId = currentUserId)
            )

            Result.success(Unit)
        } catch (e2: Exception) {
            Result.failure(e2)
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
                val defaultPref = NotificationPreference(
                    userId = currentUserId,
                    houseId = houseId,
                    id = UUID.randomUUID().toString(),
                    enableMemberJoined = true,
                    enableExpenseAdded = true,
                    enableChoreAssigned = true,
                    enableMessageSent = true,
                    enableShoppingItemAdded = true
                )
                supabase.from("notification_preferences").insert(defaultPref)
            }
        } catch (_: Exception) {
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
        } catch (_: Exception) {
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
