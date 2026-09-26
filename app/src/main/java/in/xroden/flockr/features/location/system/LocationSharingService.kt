/** The foreground service that keeps a member's shared location current until the time they chose runs out. */
package `in`.xroden.flockr.features.location.system

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import `in`.xroden.flockr.MainActivity
import `in`.xroden.flockr.R
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.features.location.data.LocationSharingRepository
import `in`.xroden.flockr.features.location.data.SharingLength
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

private const val CHANNEL_ID = "location_sharing"
private const val NOTIFICATION_ID = 7301
private const val UPDATE_EVERY_MILLIS = 30_000L
private const val UPDATE_EVERY_METRES = 25f
private const val ACTION_START = "in.xroden.flockr.location.START"
private const val ACTION_STOP = "in.xroden.flockr.location.STOP"
private const val EXTRA_HOUSE_ID = "house_id"
private const val EXTRA_HOUSE_NAME = "house_name"
private const val EXTRA_LENGTH = "length"

/**
 * Shares with one house at a time: starting in another house stops the first. It takes a first fix,
 * asks the server to start sharing (the server decides when it ends), then sends a fix every 30
 * seconds or 25 metres, whichever comes later, until the server's end time, a tap on Stop, or the
 * server answering that sharing has already stopped. It only starts from the app in the foreground,
 * so it needs no background location permission. Failures are reported through [LocationSharingStatus].
 */
@AndroidEntryPoint
class LocationSharingService : Service() {

    @Inject lateinit var repository: LocationSharingRepository
    @Inject lateinit var status: LocationSharingStatus

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val locations by lazy { getSystemService(LocationManager::class.java) }
    private var session: Job? = null
    private var houseId: String? = null
    private var listener: LocationListener? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val house = intent.getStringExtra(EXTRA_HOUSE_ID) ?: return stopNow()
                val name = intent.getStringExtra(EXTRA_HOUSE_NAME).orEmpty()
                val length = SharingLength.entries.getOrNull(intent.getIntExtra(EXTRA_LENGTH, -1)) ?: return stopNow()
                startForeground(NOTIFICATION_ID, notification(house, name, until = null), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
                begin(house, name, length)
            }
            else -> {
                houseId = houseId ?: intent?.getStringExtra(EXTRA_HOUSE_ID)
                end()
            }
        }
        return START_NOT_STICKY
    }

    private fun begin(house: String, name: String, length: SharingLength) {
        session?.cancel()
        stopUpdates()
        val previous = houseId
        houseId = house
        status.starting(house)
        session = scope.launch {
            if (previous != null && previous != house) repository.stop(previous)
            if (!hasLocationPermission()) return@launch fail("Flockr needs your location to share it.")
            val provider = bestProvider() ?: return@launch fail("Turn on location in your phone's settings to share it.")
            val first = firstFix(provider) ?: return@launch fail("Couldn't find where you are. Try again somewhere with a clearer sky.")
            repository.start(house, length, first.latitude, first.longitude, first.accuracyIfKnown()).fold(
                onSuccess = { expiresAt ->
                    status.sharing(house, expiresAt)
                    getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(house, name, expiresAt))
                    followMoves(house, provider)
                    delay((expiresAt - Clock.System.now()).inWholeMilliseconds.coerceAtLeast(0))
                    end()
                },
                onFailure = { fail(it.userMessage()) },
            )
        }
    }

    /** Location permission is checked in [begin] before this runs; lint can't see across the call. */
    @SuppressLint("MissingPermission")
    private fun followMoves(house: String, provider: String) {
        val request = LocationRequest.Builder(UPDATE_EVERY_MILLIS)
            .setMinUpdateDistanceMeters(UPDATE_EVERY_METRES)
            .setQuality(LocationRequest.QUALITY_BALANCED_POWER_ACCURACY)
            .build()
        val onMove = LocationListener { fix ->
            scope.launch {
                val stillSharing = repository.update(house, fix.latitude, fix.longitude, fix.accuracyIfKnown()).getOrDefault(true)
                if (!stillSharing) end(alreadyStopped = true)
            }
        }
        listener = onMove
        runCatching { locations.requestLocationUpdates(provider, request, mainExecutor, onMove) }
    }

    /** Stops sharing: tells the server unless it already knows, drops the notification and the service. */
    private fun end(alreadyStopped: Boolean = false) {
        val house = houseId
        houseId = null
        session?.cancel()
        stopUpdates()
        status.stopped()
        scope.launch {
            if (house != null && !alreadyStopped) repository.stop(house)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun fail(message: String) {
        status.failed(message)
        end(alreadyStopped = true)
    }

    private fun stopNow(): Int {
        stopSelf()
        return START_NOT_STICKY
    }

    private fun stopUpdates() {
        listener?.let { locations.removeUpdates(it) }
        listener = null
    }

    private fun hasLocationPermission(): Boolean =
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

    /** Google's fused provider where the phone has it, otherwise GPS, otherwise the network. */
    private fun bestProvider(): String? =
        listOf(LocationManager.FUSED_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .firstOrNull { locations.hasProvider(it) && locations.isProviderEnabled(it) }

    /** Location permission is checked in [begin] before this runs; lint can't see across the call. */
    @SuppressLint("MissingPermission")
    private suspend fun firstFix(provider: String): Location? = suspendCancellableCoroutine { done ->
        runCatching { locations.getCurrentLocation(provider, null, mainExecutor) { done.resume(it ?: locations.getLastKnownLocation(provider)) } }
            .onFailure { done.resume(null) }
    }

    private fun Location.accuracyIfKnown(): Float? = if (hasAccuracy()) accuracy else null

    private fun notification(house: String, houseName: String, until: Instant?): android.app.Notification {
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CHANNEL_ID, "Location sharing", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stopIntent = Intent(this, LocationSharingService::class.java).setAction(ACTION_STOP).putExtra(EXTRA_HOUSE_ID, house)
        val stop = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Sharing your location with $houseName")
            .setContentText(until?.let { "Until ${it.toLocalTime()}" } ?: "Finding where you are")
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(0, "Stop", stop)
            .build()
    }

    private fun Instant.toLocalTime(): String =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(toJavaInstant().atZone(ZoneId.systemDefault()))

    override fun onDestroy() {
        stopUpdates()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        /** Starts or restarts sharing with [houseId] for [length]. Call it only while the app is on screen. */
        fun start(context: Context, houseId: String, houseName: String, length: SharingLength) {
            val intent = Intent(context, LocationSharingService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_HOUSE_ID, houseId)
                .putExtra(EXTRA_HOUSE_NAME, houseName)
                .putExtra(EXTRA_LENGTH, length.ordinal)
            ContextCompat.startForegroundService(context, intent)
        }

        /**
         * Stops sharing with [houseId]. The house is named so the server hears about it even when this
         * service isn't the one that started the share, such as after the app was closed.
         */
        fun stop(context: Context, houseId: String) {
            context.startService(Intent(context, LocationSharingService::class.java).setAction(ACTION_STOP).putExtra(EXTRA_HOUSE_ID, houseId))
        }
    }
}
