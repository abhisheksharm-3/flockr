package `in`.xroden.flockr.features.notifications.data

import `in`.xroden.flockr.data.dto.NotificationUpdate
import `in`.xroden.flockr.features.notifications.model.Notification
import `in`.xroden.flockr.features.notifications.model.NotificationPreference
import `in`.xroden.flockr.features.notifications.service.NotificationService
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
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
                    .decodeList<Notification>()

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

                            notificationService.showNotification(
                                id = notificationId,
                                title = title,
                                message = message,
                                houseId = houseId,
                                type = typeStr,
                                data = null
                            )
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
                        .decodeList<Notification>()

                    send(Result.success(updated))
                }
            } catch (e: Exception) {
                send(Result.failure(e))
            }

            awaitClose {
                try {
                    supabase.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    // Ignore cleanup errors
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
                .decodeList<Notification>()

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

            supabase.from("notifications")
                .update(NotificationUpdate(isRead = true)) {
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

    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            supabase.from("notifications")
                .delete {
                    filter {
                        eq("id", notificationId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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
}
