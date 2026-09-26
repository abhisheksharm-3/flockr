/** What sharing a location looks like in a house: the live map, starting and managing a share, and the words for where someone is. */
package `in`.xroden.flockr.features.location.ui

import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.features.location.data.SharingLength
import `in`.xroden.flockr.features.location.model.MemberLocation
import `in`.xroden.flockr.ui.components.ListRow
import `in`.xroden.flockr.ui.components.MemberAvatar
import `in`.xroden.flockr.ui.components.mapStyleUri
import `in`.xroden.flockr.ui.components.rememberLiveMapView
import `in`.xroden.flockr.ui.theme.ComponentHeight
import `in`.xroden.flockr.ui.theme.IconSize
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.ui.theme.flockrColors
import `in`.xroden.flockr.utils.rememberHaptics
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap

private const val AT_HOME_METRES = 150f
private const val METRES_PER_KM = 1000f
private const val SINGLE_POINT_ZOOM = 15.0
private const val MAP_ASPECT = 0.9f
private val MarkerRing = 3.dp

/** A housemate who is sharing, with their profile when they are still on the roster. */
data class SharedPerson(val location: MemberLocation, val member: MemberWithProfile?, val isViewer: Boolean) {
    val name: String get() = if (isViewer) "You" else member?.shortName ?: "A housemate"
}

/** "at home", "1.2 km from home", or "sharing now" when the house has no pin to measure from. */
fun whereLabel(location: MemberLocation, home: LatLng?): String {
    home ?: return "sharing now"
    val metres = FloatArray(1).also { Location.distanceBetween(home.latitude, home.longitude, location.latitude, location.longitude, it) }[0]
    return when {
        metres <= AT_HOME_METRES -> "at home"
        metres < METRES_PER_KM -> "${(metres / 10).roundToInt() * 10} m from home"
        else -> "${"%.1f".format(metres / METRES_PER_KM)} km from home"
    }
}

/** "updated just now", "updated 3 minutes ago", as the device says it. */
fun updatedLabel(location: MemberLocation): String {
    val now = Clock.System.now().toEpochMilliseconds()
    val then = location.updatedAt.toEpochMilliseconds()
    return if (now - then < DateUtils.MINUTE_IN_MILLIS) "updated just now"
    else "updated " + DateUtils.getRelativeTimeSpanString(then, now, DateUtils.MINUTE_IN_MILLIS).toString().lowercase()
}

/** The time a share ends, in the phone's own clock format, such as "7:30 pm". */
fun untilLabel(expiresAt: Instant): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(expiresAt.toJavaInstant().atZone(ZoneId.systemDefault()))

/** Opens whichever maps app the phone uses, with directions to [location]. Does nothing without one. */
fun openDirections(context: Context, location: MemberLocation, label: String) {
    val uri = Uri.parse("geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude}(${Uri.encode(label)})")
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/**
 * The house and everyone sharing with it on one live map, framed to fit them all when it opens and
 * whenever someone starts or stops. People are drawn as their avatars over the map and follow it as
 * it pans and zooms: every camera move bumps a frame counter the overlay is keyed on, so it
 * re-projects each point.
 */
@Composable
fun LiveHouseMap(home: LatLng?, people: List<SharedPerson>, modifier: Modifier = Modifier) {
    val style = mapStyleUri()
    val mapView = rememberLiveMapView()
    val density = LocalDensity.current
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var frame by remember { mutableIntStateOf(0) }
    var paddingPx by remember { mutableIntStateOf(0) }

    LaunchedEffect(mapView, style) {
        mapView.getMapAsync { ready ->
            ready.setStyle(style)
            ready.uiSettings.isLogoEnabled = false
            ready.uiSettings.isCompassEnabled = false
            ready.uiSettings.isRotateGesturesEnabled = false
            ready.uiSettings.isTiltGesturesEnabled = false
            ready.addOnCameraMoveListener { frame++ }
            ready.addOnCameraIdleListener { frame++ }
            map = ready
        }
    }
    val points = people.map { LatLng(it.location.latitude, it.location.longitude) } + listOfNotNull(home)
    LaunchedEffect(map, people.map { it.location.userId }.toSet(), home) {
        val ready = map ?: return@LaunchedEffect
        when {
            points.size >= 2 -> ready.easeCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.Builder().includes(points).build(), paddingPx))
            points.size == 1 -> ready.cameraPosition = CameraPosition.Builder().target(points.single()).zoom(SINGLE_POINT_ZOOM).build()
        }
    }

    Box(modifier.clip(MaterialTheme.shapes.large).onSizeChanged { paddingPx = it.width / 6 }) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        map?.let { ready -> key(frame) {
            val half = with(density) { (ComponentHeight.avatar / 2).roundToPx() }
            home?.let { spot ->
                val at = ready.projection.toScreenLocation(spot)
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.offset { IntOffset(at.x.roundToInt() - half, at.y.roundToInt() - half) }.size(ComponentHeight.avatar),
                ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Home, contentDescription = "Home", modifier = Modifier.size(IconSize.sm)) } }
            }
            people.forEach { person ->
                val at = ready.projection.toScreenLocation(LatLng(person.location.latitude, person.location.longitude))
                MemberAvatar(
                    name = person.member?.displayName ?: person.name,
                    avatarUrl = person.member?.avatarUrl,
                    modifier = Modifier
                        .offset { IntOffset(at.x.roundToInt() - half, at.y.roundToInt() - half) }
                        .border(MarkerRing, MaterialTheme.flockrColors.sun, CircleShape),
                )
            }
        } }
    }
}

/** The map in a sheet, with a row per sharer and their directions. */
@Composable
fun LiveMapSheet(houseName: String, home: LatLng?, people: List<SharedPerson>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss, sheetGesturesEnabled = false) {
        Column(Modifier.navigationBarsPadding().padding(bottom = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                if (people.isEmpty()) "Nobody is sharing right now" else "Around $houseName",
                style = MaterialTheme.typography.titleLargeEmphasized,
                modifier = Modifier.padding(horizontal = Spacing.lg),
            )
            LiveHouseMap(home, people, Modifier.padding(horizontal = Spacing.lg).fillMaxWidth().aspectRatio(MAP_ASPECT))
            people.forEach { person ->
                ListRow(
                    headline = person.name,
                    supporting = "${whereLabel(person.location, home)} · ${updatedLabel(person.location)}",
                    leading = { MemberAvatar(name = person.member?.displayName ?: person.name, avatarUrl = person.member?.avatarUrl) },
                    trailing = if (person.isViewer) null else ({
                        OutlinedButton(onClick = { openDirections(context, person.location, person.name) }, shapes = ButtonDefaults.shapes()) { Text("Directions") }
                    }),
                )
            }
        }
    }
}

/**
 * Choosing how long to share for. It says plainly who sees what, and that nothing is kept, before
 * anything is asked of the phone.
 */
@Composable
fun StartSharingSheet(houseName: String, onPick: (SharingLength) -> Unit, onDismiss: () -> Unit) {
    val haptics = rememberHaptics()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.navigationBarsPadding().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text("Share where you are with $houseName", style = MaterialTheme.typography.titleLargeEmphasized)
            Text(
                "Everyone in the house sees you on a map until it ends. You can stop any time, and nothing is kept afterwards.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SharingLength.entries.forEach { length ->
                FilledTonalButton(
                    onClick = { haptics.select(); onPick(length) },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = ButtonDefaults.MediumContainerHeight),
                ) {
                    Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Text("For ${length.label}", style = MaterialTheme.typography.labelLargeEmphasized, modifier = Modifier.padding(start = Spacing.sm))
                }
            }
        }
    }
}

/** While sharing: when it ends, stopping it, or starting it again for longer. */
@Composable
fun ManageSharingSheet(houseName: String, expiresAt: Instant, onStop: () -> Unit, onExtend: (SharingLength) -> Unit, onDismiss: () -> Unit) {
    val haptics = rememberHaptics()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.navigationBarsPadding().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text("Sharing with $houseName until ${untilLabel(expiresAt)}", style = MaterialTheme.typography.titleLargeEmphasized, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("Everyone in the house can see where you are.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = { haptics.tap(); onStop() },
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                modifier = Modifier.fillMaxWidth().heightIn(min = ButtonDefaults.MediumContainerHeight),
            ) { Text("Stop sharing", style = MaterialTheme.typography.labelLargeEmphasized) }
            Text("Or share for longer, starting now", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SharingLength.entries.forEach { length ->
                OutlinedButton(onClick = { haptics.select(); onExtend(length) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) {
                    Text("For ${length.label}")
                }
            }
        }
    }
}
