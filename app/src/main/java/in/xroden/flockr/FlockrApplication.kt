package `in`.xroden.flockr

import android.app.Application
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import `in`.xroden.flockr.features.notifications.system.NotificationPoster
import javax.inject.Inject

/**
 * Versions before push delivery scheduled a periodic check under this name. Its worker class no
 * longer exists, so an upgraded install would fail it every 15 minutes until it is cancelled.
 */
private const val RETIRED_NOTIFICATION_CHECK = "notification_check"

@HiltAndroidApp
class FlockrApplication : Application() {

    @Inject
    lateinit var notificationPoster: NotificationPoster

    override fun onCreate() {
        super.onCreate()
        notificationPoster.createChannels()
        WorkManager.getInstance(this).cancelUniqueWork(RETIRED_NOTIFICATION_CHECK)
    }
}
