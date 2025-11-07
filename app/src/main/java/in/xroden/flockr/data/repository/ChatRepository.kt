package `in`.xroden.flockr.data.repository

import `in`.xroden.flockr.data.model.Message
import `in`.xroden.flockr.data.model.CreateNotificationWithTypeParams
import `in`.xroden.flockr.utils.FlockrLogger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    fun getCurrentUserId(): String? = userId
    
    companion object {
        private const val TAG = "ChatRepository"
    }

    fun getMessagesFlow(houseId: String): Flow<List<Message>> {
        FlockrLogger.realtimeEvent(TAG, "getMessagesFlow", "Starting for house=$houseId")
        return kotlinx.coroutines.flow.flow {
            // Emit initial value immediately
            val initialMessages = getMessages(houseId)
            FlockrLogger.d(TAG, "getMessagesFlow: Emitting initial ${initialMessages.size} messages")
            emit(initialMessages)

            // Create and subscribe to the channel
            val channelId = "messages_${houseId}_${System.currentTimeMillis()}"
            val channel = supabase.realtime.channel(channelId)

            try {
                // Configure realtime subscription BEFORE subscribing
                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "messages"
                }
                
                FlockrLogger.realtimeEvent(TAG, "getMessagesFlow", "Subscribing to channel $channelId")
                // Subscribe and wait for it to be ready
                channel.subscribe(blockUntilSubscribed = true)
                FlockrLogger.realtimeEvent(TAG, "getMessagesFlow", "Successfully subscribed")

                // Now listen for changes
                changeFlow.collect { change ->
                    FlockrLogger.realtimeEvent(TAG, "getMessagesFlow", "Received update: ${change.javaClass.simpleName}")
                    // Small delay to ensure database consistency
                    kotlinx.coroutines.delay(100)
                    val updatedMessages = getMessages(houseId)
                    FlockrLogger.d(TAG, "getMessagesFlow: Emitting ${updatedMessages.size} messages after update")
                    emit(updatedMessages)
                }
            } catch (e: Exception) {
                FlockrLogger.repoError(TAG, "getMessagesFlow", e)
                // Keep the initial messages visible even if realtime fails
            } finally {
                try {
                    FlockrLogger.d(TAG, "getMessagesFlow: Cleaning up channel")
                    supabase.realtime.removeChannel(channel)
                } catch (e: Exception) {
                    FlockrLogger.e(TAG, "getMessagesFlow: Error removing channel", e)
                }
            }
        }
    }

    suspend fun getMessages(houseId: String): List<Message> {
        FlockrLogger.repoStart(TAG, "getMessages", mapOf("houseId" to houseId))
        return try {
            // Fetch messages with sender name from profiles table
            val response = supabase.from("messages")
                .select(Columns.raw("*, profiles!messages_user_id_fkey(full_name)")) {
                    filter {
                        eq("house_id", houseId)
                    }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                }

            FlockrLogger.d(TAG, "getMessages: Decoding JSON response")
            val result = response.decodeAs<JsonArray>()
            FlockrLogger.d(TAG, "getMessages: Successfully decoded ${result.size} messages")

            val messages = result.mapNotNull { element ->
                val obj = element.jsonObject
                val id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val houseId = obj["house_id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val userId = obj["user_id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val content = obj["content"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val createdAt = obj["created_at"]?.jsonPrimitive?.content ?: return@mapNotNull null

                // Extract sender name from profiles
                val senderName = obj["profiles"]?.jsonObject?.get("full_name")?.jsonPrimitive?.content

                Message(
                    id = id,
                    houseId = houseId,
                    userId = userId,
                    content = content,
                    createdAt = createdAt,
                    senderName = senderName ?: "Unknown"
                )
            }

            FlockrLogger.repoSuccess(TAG, "getMessages", "Returning ${messages.size} messages")
            messages
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "getMessages", e)
            emptyList()
        }
    }

    suspend fun sendMessage(houseId: String, content: String, houseName: String): Result<Unit> {
        FlockrLogger.repoStart(TAG, "sendMessage", mapOf(
            "houseId" to houseId,
            "contentLength" to content.length
        ))
        return try {
            val currentUserId = userId ?: run {
                FlockrLogger.e(TAG, "sendMessage: No user logged in")
                return Result.failure(Exception("No user logged in"))
            }

            supabase.from("messages")
                .insert(
                    mapOf(
                        "house_id" to houseId,
                        "user_id" to currentUserId,
                        "content" to content
                    )
                )

            FlockrLogger.d(TAG, "sendMessage: Message inserted successfully")

            // Create notification for house members
            val truncatedContent = if (content.length > 50) {
                content.substring(0, 50) + "..."
            } else {
                content
            }

            FlockrLogger.d(TAG, "sendMessage: Creating notification")
            val notificationParams = CreateNotificationWithTypeParams(
                houseId = houseId,
                title = "New message in $houseName",
                message = truncatedContent,
                type = "message",
                data = """{"houseId":"$houseId"}""",
                excludeUserId = currentUserId
            )
            supabase.postgrest.rpc(
                function = "create_notification_for_house",
                parameters = notificationParams
            )

            FlockrLogger.repoSuccess(TAG, "sendMessage", "Message sent and notification created")
            Result.success(Unit)
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "sendMessage", e)
            Result.failure(e)
        }
    }
}

