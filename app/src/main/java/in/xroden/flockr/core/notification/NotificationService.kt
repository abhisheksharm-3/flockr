package `in`.xroden.flockr.core.notification

import `in`.xroden.flockr.core.logging.Logger
import `in`.xroden.flockr.data.dto.notification.HouseNotificationParams
import `in`.xroden.flockr.data.dto.notification.NotificationParams
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/** Centralized service for sending notifications via Supabase RPC. */
@Singleton
class NotificationService @Inject constructor(
    private val supabase: SupabaseClient
) {
    /** Sends a notification to a specific user. */
    suspend fun sendToUser(
        userId: String,
        houseId: String,
        title: String,
        message: String,
        type: String,
        data: JsonObject = buildJsonObject { }
    ) {
        runCatching {
            supabase.postgrest.rpc(
                function = "create_notification",
                parameters = NotificationParams(
                    userId = userId,
                    houseId = houseId,
                    title = title,
                    message = message,
                    type = type,
                    data = data.toString()
                )
            )
        }.onFailure { e ->
            Logger.w(TAG, "Failed to send notification to user $userId", e)
        }
    }

    /** Sends a notification to all members of a house except the specified user. */
    suspend fun sendToHouse(
        houseId: String,
        title: String,
        message: String,
        type: String,
        excludeUserId: String,
        data: JsonObject = buildJsonObject { }
    ) {
        runCatching {
            supabase.postgrest.rpc(
                function = "create_notification_for_house",
                parameters = HouseNotificationParams(
                    houseId = houseId,
                    title = title,
                    message = message,
                    type = type,
                    data = data.toString(),
                    excludeUserId = excludeUserId
                )
            )
        }.onFailure { e ->
            Logger.w(TAG, "Failed to send house notification", e)
        }
    }

    /** Sends a chore assigned notification. */
    suspend fun sendChoreAssigned(
        houseId: String,
        assigneeId: String,
        taskName: String,
        assignerId: String
    ) {
        val data = buildJsonObject {
            put("type", "chore_assigned")
            put("taskName", taskName)
        }
        sendToUser(
            userId = assigneeId,
            houseId = houseId,
            title = "New Chore Assigned",
            message = "You have been assigned a new chore: $taskName.",
            type = "chore_assigned",
            data = data
        )
    }

    /** Sends a chore created notification to house members. */
    suspend fun sendChoreCreated(
        houseId: String,
        taskName: String,
        creatorId: String
    ) {
        val data = buildJsonObject {
            put("type", "chore")
            put("taskName", taskName)
        }
        sendToHouse(
            houseId = houseId,
            title = "New Chore Created",
            message = "New chore created: $taskName.",
            type = "chore",
            excludeUserId = creatorId,
            data = data
        )
    }

    /** Sends a chore completed notification. */
    suspend fun sendChoreCompleted(
        houseId: String,
        choreId: String,
        taskName: String,
        completedBy: String
    ) {
        val data = buildJsonObject {
            put("id", choreId)
            put("type", "chore")
        }
        sendToHouse(
            houseId = houseId,
            title = "Chore Completed",
            message = "Completed the chore: $taskName.",
            type = "chore",
            excludeUserId = completedBy,
            data = data
        )
    }

    /** Sends a message notification. */
    suspend fun sendMessageNotification(
        houseId: String,
        messagePreview: String,
        senderId: String
    ) {
        val data = buildJsonObject { put("type", "message_sent") }
        sendToHouse(
            houseId = houseId,
            title = "New Message",
            message = messagePreview.take(50),
            type = "message_sent",
            excludeUserId = senderId,
            data = data
        )
    }

    /** Sends a shopping item added notification. */
    suspend fun sendShoppingItemAdded(
        houseId: String,
        itemName: String,
        addedBy: String
    ) {
        val data = buildJsonObject {
            put("type", "shopping")
            put("itemName", itemName)
        }
        sendToHouse(
            houseId = houseId,
            title = "Shopping List Updated",
            message = "New item added: $itemName",
            type = "shopping",
            excludeUserId = addedBy,
            data = data
        )
    }

    /** Sends an item purchased notification. */
    suspend fun sendShoppingItemPurchased(
        houseId: String,
        itemId: String,
        itemName: String,
        purchasedBy: String
    ) {
        val data = buildJsonObject {
            put("type", "shopping")
            put("itemId", itemId)
        }
        sendToHouse(
            houseId = houseId,
            title = "Item Purchased",
            message = "Purchased: $itemName",
            type = "shopping",
            excludeUserId = purchasedBy,
            data = data
        )
    }

    /** Sends a document uploaded notification. */
    suspend fun sendDocumentUploaded(
        houseId: String,
        documentId: String,
        fileName: String,
        uploadedBy: String
    ) {
        val data = buildJsonObject { put("id", documentId) }
        sendToHouse(
            houseId = houseId,
            title = "New Document Uploaded",
            message = "Uploaded a new document: $fileName.",
            type = "document",
            excludeUserId = uploadedBy,
            data = data
        )
    }

    companion object {
        private const val TAG = "NotificationService"
    }
}
