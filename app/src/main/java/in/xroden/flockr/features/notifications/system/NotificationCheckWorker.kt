/** Posts new notifications while Flockr is closed, by checking for them every 15 minutes. */
package `in`.xroden.flockr.features.notifications.system

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import `in`.xroden.flockr.features.notifications.data.NotificationRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.time.toJavaDuration
import kotlinx.coroutines.flow.first

private val LAST_POSTED_AT = stringPreferencesKey("notifications_last_posted_at")

private const val WORK_NAME = "notification_check"

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationCheckEntryPoint {
    fun supabase(): SupabaseClient
    fun notifications(): NotificationRepository
    fun poster(): NotificationPoster
    fun dataStore(): DataStore<Preferences>
}

/**
 * ponytail: polling, up to 15 minutes late. Replace with an FCM push from a database webhook once
 * the app has a Firebase project; `device_tokens` is already in the schema for it.
 *
 * The first check after install looks back one day at most, so a new device is not flooded with an
 * old backlog.
 */
class NotificationCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, NotificationCheckEntryPoint::class.java)
        deps.supabase().auth.awaitInitialization()
        if (deps.supabase().auth.currentUserOrNull() == null) return Result.success()

        val store = deps.dataStore()
        val after = store.data.first()[LAST_POSTED_AT]?.let(Instant::parse) ?: (Clock.System.now() - 1.days)
        val fresh = deps.notifications().getUnreadAfter(after).getOrElse { return Result.retry() }
        fresh.forEach(deps.poster()::post)
        fresh.lastOrNull()?.let { last -> store.edit { it[LAST_POSTED_AT] = last.createdAt.toString() } }
        return Result.success()
    }

    companion object {
        /** Keeps one periodic check scheduled; calling it again leaves the existing schedule alone. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotificationCheckWorker>(15.minutes.toJavaDuration())
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
