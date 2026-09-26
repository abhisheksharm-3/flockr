/** The in-app notifications the database writes for each member, and the kinds there are. */
package `in`.xroden.flockr.features.notifications.model

import androidx.compose.runtime.Immutable
import `in`.xroden.flockr.data.serialization.InstantSerializer
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** The groups notifications are posted under, which a member can mute separately in Android settings. */
enum class NotificationGroup(val channelId: String, val label: String) {
    MONEY("money", "Money"),
    CHORES("chores", "Chores and shopping"),
    HOUSE("house", "House"),
    MESSAGES("messages", "Messages"),
}

/** The kinds in `notification_types`. [key] is the stored value and [label] what the preference switch says. */
enum class NotificationType(val key: String, val label: String, val group: NotificationGroup) {
    EXPENSE_ADDED("expense_added", "An expense you're on is added", NotificationGroup.MONEY),
    EXPENSE_UPDATED("expense_updated", "An expense you're on changes", NotificationGroup.MONEY),
    SETTLEMENT_RECEIVED("settlement_received", "Someone pays you back", NotificationGroup.MONEY),
    SETTLEMENT_RECORDED("settlement_recorded", "Someone records a payment you made", NotificationGroup.MONEY),
    BILL_DUE("bill_due", "A bill is due soon", NotificationGroup.MONEY),
    BILL_PAID("bill_paid", "A bill you share is paid", NotificationGroup.MONEY),
    CHORE_ASSIGNED("chore_assigned", "A chore is assigned to you", NotificationGroup.CHORES),
    CHORE_COMPLETED("chore_completed", "A chore you made is done", NotificationGroup.CHORES),
    SHOPPING_ITEM_ADDED("shopping_item_added", "Something is added to the list", NotificationGroup.CHORES),
    MESSAGE("message", "A new message in the chat", NotificationGroup.MESSAGES),
    MEMBER_JOINED("member_joined", "Someone joins", NotificationGroup.HOUSE),
    MEMBER_LEFT("member_left", "Someone leaves", NotificationGroup.HOUSE),
    DOCUMENT_UPLOADED("document_uploaded", "A document is added", NotificationGroup.HOUSE),
    INVITATION("invitation", "You're invited to a house", NotificationGroup.HOUSE),
    LOCATION_SHARED("location_shared", "Someone starts sharing their location", NotificationGroup.HOUSE);

    companion object {
        fun of(key: String): NotificationType? = entries.firstOrNull { it.key == key }
    }
}

/**
 * One notification. [data] carries the ids a tap opens, such as `expense_id` or `chore_id`. [type]
 * stays a string so a kind added to the database later still shows, just without a destination.
 */
@Immutable
@Serializable
data class Notification(
    val id: String,
    @SerialName("house_id")
    val houseId: String? = null,
    @SerialName("actor_id")
    val actorId: String? = null,
    val type: String,
    val title: String,
    val body: String,
    val data: JsonObject = JsonObject(emptyMap()),
    @SerialName("is_read")
    val isRead: Boolean = false,
    @SerialName("created_at")
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant
) {
    val kind: NotificationType? get() = NotificationType.of(type)

    /** The string value of [key] in [data], or null. */
    fun dataId(key: String): String? = (data[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
}
