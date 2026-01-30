package `in`.xroden.flockr.features.chat.data

import `in`.xroden.flockr.core.network.RealtimeConnectionManager
import `in`.xroden.flockr.core.validation.Validators
import `in`.xroden.flockr.data.base.BaseRealtimeRepository
import `in`.xroden.flockr.data.dto.HouseNotificationParams
import `in`.xroden.flockr.data.dto.MessageInsert
import `in`.xroden.flockr.features.chat.model.Message
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonArray

@Singleton
class ChatRepository @Inject constructor(
    supabase: SupabaseClient,
    connectionManager: RealtimeConnectionManager
) : BaseRealtimeRepository(supabase, connectionManager), IChatRepository {

    private val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    override fun getCurrentUserId(): String? = userId

    override fun getMessagesFlow(houseId: String): Flow<Result<List<Message>>> {
        return createRealtimeFlow(
            channelId = "messages_$houseId",
            table = "messages",
            filterColumn = "house_id",
            filterValue = houseId,
            fetchData = { getMessages(houseId) }
        )
    }

    suspend fun getMessages(houseId: String): Result<List<Message>> {
        return try {
            val response = supabase.from("messages")
                .select(Columns.raw("*, profiles!messages_user_id_fkey(full_name)")) {
                    filter {
                        eq("house_id", houseId)
                    }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                }

            val result = response.decodeAs<JsonArray>()

            val messages = result.mapNotNull { element ->
                val obj = element.jsonObject
                val senderName = obj["profiles"]?.takeIf { it !is kotlinx.serialization.json.JsonNull }
                    ?.jsonObject?.get("full_name")?.jsonPrimitive?.content

                try {
                    Message(
                        id = obj["id"]?.jsonPrimitive?.content ?: "",
                        houseId = obj["house_id"]?.jsonPrimitive?.content ?: "",
                        userId = obj["user_id"]?.jsonPrimitive?.content ?: "",
                        content = obj["content"]?.jsonPrimitive?.content ?: "",
                        createdAt = obj["created_at"]?.jsonPrimitive?.content?.let { Instant.parse(it) }
                            ?: Instant.DISTANT_PAST,
                        senderName = senderName
                    )
                } catch (_: Exception) {
                    null
                }
            }

            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendMessage(houseId: String, content: String): Result<Unit> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            val validatedContent = Validators.validateMessageContent(content).getOrThrow()

            supabase.from("messages")
                .insert(
                    MessageInsert(
                        houseId = houseId,
                        userId = currentUserId,
                        content = validatedContent
                    )
                )

            try {
                supabase.postgrest.rpc(
                    function = "create_notification_for_house",
                    parameters = HouseNotificationParams(
                        houseId = houseId,
                        title = "New Message",
                        message = content.take(50),
                        type = "message_sent",
                        data = """{"type":"message_sent"}""",
                        excludeUserId = currentUserId
                    )
                )
            } catch (_: Exception) {
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


}
