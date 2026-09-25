package `in`.xroden.flockr

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import `in`.xroden.flockr.features.notifications.system.NotificationCheckWorker
import `in`.xroden.flockr.features.notifications.system.NotificationPoster
import javax.inject.Inject

@HiltAndroidApp
class FlockrApplication : Application() {

    @Inject
    lateinit var notificationPoster: NotificationPoster

    override fun onCreate() {
        super.onCreate()
        notificationPoster.createChannels()
        NotificationCheckWorker.schedule(this)
    }
}
