/**
 * A house's place on a real map, in the app's theme, on MapLibre and OpenFreeMap's free tiles: no API
 * key, no account and no billing. Still maps are rendered once to a picture; the picker gets a live one.
 */
package `in`.xroden.flockr.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.snapshotter.MapSnapshotter

private const val STREET_ZOOM = 15.5
private const val DARK_PAGE_LUMINANCE = 0.5f
private const val SNAPSHOTS_KEPT = 8
private const val LIGHT_STYLE = "https://tiles.openfreemap.org/styles/positron"
private const val DARK_STYLE = "https://tiles.openfreemap.org/styles/fiord"

/** Maps already drawn this session, keyed by place, size and theme, so scrolling back doesn't redraw them. */
private val snapshots = LruCache<String, Bitmap>(SNAPSHOTS_KEPT)

/** The OpenFreeMap style for the current theme: pale "positron" by day, deep blue "fiord" by night. */
@Composable
fun mapStyleUri(): String = if (MaterialTheme.colorScheme.background.luminance() < DARK_PAGE_LUMINANCE) DARK_STYLE else LIGHT_STYLE

/**
 * The streets around [location] at street level, as a still picture in the app's theme. It ignores
 * touches, so it can sit under anything tappable, and shows the page's container colour while it draws.
 */
@Composable
fun HouseMap(location: LatLng, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val style = mapStyleUri()
    val placeholder = MaterialTheme.colorScheme.surfaceContainerHigh
    BoxWithConstraints(modifier.background(placeholder)) {
        val density = LocalDensity.current
        val width = with(density) { maxWidth.roundToPx() }
        val height = with(density) { maxHeight.roundToPx() }
        val key = "${location.latitude},${location.longitude},$width,$height,$style"
        val picture by produceState(snapshots.get(key), key) {
            if (value == null && width > 0 && height > 0) value = snapshot(context, location, width, height, style, density.density)?.also { snapshots.put(key, it) }
        }
        picture?.let { Image(it.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize()) }
    }
}

private suspend fun snapshot(context: Context, location: LatLng, width: Int, height: Int, style: String, pixelRatio: Float): Bitmap? {
    MapLibre.getInstance(context)
    val options = MapSnapshotter.Options(width, height)
        .withStyleBuilder(Style.Builder().fromUri(style))
        .withCameraPosition(CameraPosition.Builder().target(location).zoom(STREET_ZOOM).build())
        .withPixelRatio(pixelRatio)
        .withLogo(false)
    val snapshotter = MapSnapshotter(context, options)
    return suspendCancellableCoroutine { done ->
        done.invokeOnCancellation { snapshotter.cancel() }
        snapshotter.start({ done.resume(it.bitmap) }, { done.resume(null) })
    }
}

/**
 * A live MapLibre view tied to the screen's lifecycle, for the one place the map is touched: the
 * location picker. Every call gets its own view, destroyed when it leaves composition.
 */
@Composable
fun rememberLiveMapView(): MapView {
    val context = LocalContext.current
    val view = remember { MapLibre.getInstance(context); MapView(context).apply { onCreate(null) } }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, view) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> view.onStart()
                Lifecycle.Event.ON_RESUME -> view.onResume()
                Lifecycle.Event.ON_PAUSE -> view.onPause()
                Lifecycle.Event.ON_STOP -> view.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) view.onPause()
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) view.onStop()
            view.onDestroy()
        }
    }
    return view
}

/** A map position from stored coordinates, or null when either is missing. */
fun latLngOf(latitude: Double?, longitude: Double?): LatLng? =
    if (latitude != null && longitude != null) LatLng(latitude, longitude) else null
