/** Shows notifications in the Android shade, one channel per [NotificationGroup], each opening its subject. */
package `in`.xroden.flockr.features.notifications.system

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import `in`.xroden.flockr.MainActivity
import `in`.xroden.flockr.R
import `in`.xroden.flockr.features.notifications.model.Notification
import `in`.xroden.flockr.features.notifications.model.NotificationGroup
import javax.inject.Inject
import javax.inject.Singleton

/** The intent extra naming the notification a tap came from, which [MainActivity] routes to its subject. */
const val EXTRA_NOTIFICATION_ID = "in.xroden.flockr.NOTIFICATION_ID"

@Singleton
class NotificationPoster @Inject constructor(@ApplicationContext private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    /** Registers the channels. Android keeps a channel's user settings across calls, so this runs on every start. */
    fun createChannels() {
        NotificationGroup.entries.forEach { group ->
            val importance = if (group == NotificationGroup.MESSAGES) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            manager.createNotificationChannel(NotificationChannel(group.channelId, group.label, importance))
        }
    }

    /** Posts [notification], replacing an earlier post of the same one. Does nothing without the permission. */
    fun post(notification: Notification) {
        val isAllowed = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!isAllowed) return
        val open = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_NOTIFICATION_ID, notification.id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val tap = PendingIntent.getActivity(context, notification.id.hashCode(), open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val group = notification.kind?.group ?: NotificationGroup.HOUSE
        val shown = NotificationCompat.Builder(context, group.channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.body))
            .setWhen(notification.createdAt.toEpochMilliseconds())
            .setShowWhen(true)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        manager.notify(notification.id, 0, shown)
    }

    fun cancel(notificationId: String) = manager.cancel(notificationId, 0)
}
