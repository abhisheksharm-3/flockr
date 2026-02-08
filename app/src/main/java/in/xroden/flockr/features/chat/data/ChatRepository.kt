package `in`.xroden.flockr.features.chat.data

import `in`.xroden.flockr.core.domain.requireAuthenticated
import `in`.xroden.flockr.core.network.RateLimiter
import `in`.xroden.flockr.core.network.RealtimeConnectionManager
import `in`.xroden.flockr.core.notification.NotificationService
import `in`.xroden.flockr.core.security.InputSanitizer
import `in`.xroden.flockr.core.validation.Validators
import `in`.xroden.flockr.data.base.BaseRealtimeRepository
import `in`.xroden.flockr.data.dto.MessageInsert
import `in`.xroden.flockr.features.chat.model.Message
import `in`.xroden.flockr.features.chat.model.MessageWithProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Repository for chat message operations. */
@Singleton
class ChatRepository @Inject constructor(
    supabase: SupabaseClient,
    connectionManager: RealtimeConnectionManager,
    private val notificationService: NotificationService,
    private val rateLimiter: RateLimiter
) : BaseRealtimeRepository(supabase, connectionManager), IChatRepository {

    override fun getCurrentUserId(): String? = authenticatedUserId

    override fun getMessagesFlow(houseId: String): Flow<Result<List<Message>>> =
        createRealtimeFlow(
            channelId = "messages_$houseId",
            table = "messages",
            filterColumn = "house_id",
            filterValue = houseId,
            fetchData = { getMessages(houseId) }
        )

    private suspend fun getMessages(houseId: String): Result<List<Message>> = runCatching {
        supabase.from("messages")
            .select(Columns.raw("*, profiles!messages_user_id_fkey(full_name)")) {
                filter { eq("house_id", houseId) }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<MessageWithProfile>()
            .map { it.toMessage() }
    }

    override suspend fun sendMessage(houseId: String, content: String): Result<Unit> =
        rateLimiter.throttle("send_message_$houseId", maxRequestsPerMinute = 60) {
            runCatching {
                val userId = requireAuthenticated(authenticatedUserId)
                val validatedContent = Validators.validateMessageContent(content).getOrThrow()
                val sanitizedContent = InputSanitizer.sanitizeText(validatedContent)

                supabase.from("messages")
                    .insert(
                        MessageInsert(
                            houseId = houseId,
                            userId = userId,
                            content = sanitizedContent
                        )
                    )

                notificationService.sendMessageNotification(houseId, sanitizedContent, userId)
            }
        }
}
