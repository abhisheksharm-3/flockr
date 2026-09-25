/** Receives pushes from Firebase and shows them the way the rest of the app's notifications look. */
package `in`.xroden.flockr.features.notifications.system

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import `in`.xroden.flockr.features.notifications.model.Notification
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** The push carries the notification's fields as data, written by the push Edge Function. */
@AndroidEntryPoint
class FlockrMessagingService : FirebaseMessagingService() {

    @Inject lateinit var poster: NotificationPoster
    @Inject lateinit var pushTokens: PushTokens

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val id = data["notification_id"] ?: return
        poster.post(
            Notification(
                id = id,
                houseId = data["house_id"]?.ifBlank { null },
                type = data["type"].orEmpty(),
                title = data["title"].orEmpty(),
                body = data["body"].orEmpty(),
                createdAt = data["created_at"]?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: Clock.System.now(),
            )
        )
    }

    override fun onNewToken(token: String) {
        scope.launch { pushTokens.register(token) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
