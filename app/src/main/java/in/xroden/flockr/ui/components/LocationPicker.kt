/** Picking where a house is: a live map under a fixed pin, with address search both ways. */
package `in`.xroden.flockr.ui.components

import android.content.Context
import android.location.Address
import android.location.Geocoder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.viewinterop.AndroidView
import `in`.xroden.flockr.ui.components.buttons.FlockrPrimaryButton
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.ui.theme.IconSize
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics
import kotlin.coroutines.resume
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap

private const val PICKER_ZOOM = 16.0
private const val COUNTRY_ZOOM = 4.5
private const val MAP_ASPECT = 1f

/** Where the map opens with nothing to go on: the middle of India, at country scale. */
private val FirstLook = LatLng(20.59, 78.96)

/** Where a house sits, as the picker hands it back: the point, and the address found for it, if any. */
data class PickedPlace(val latitude: Double, val longitude: Double, val address: String?)

/**
 * A sheet with a map to drag under a fixed pin, and a search box that jumps the map to an address.
 * Whenever the map comes to rest, the address under the pin is looked up and shown, so the user
 * sees what they are choosing before choosing it. [initial] centres the map; without it, the map
 * searches for [initialQuery], usually the typed address, or opens on the whole country.
 */
@Composable
fun LocationPickerSheet(initial: LatLng?, initialQuery: String?, onPick: (PickedPlace) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val style = mapStyleUri()
    val mapView = rememberLiveMapView()
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var query by remember { mutableStateOf(if (initial == null) initialQuery.orEmpty() else "") }
    var address by remember { mutableStateOf<String?>(null) }
    var searchError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(mapView, style) {
        mapView.getMapAsync { ready ->
            ready.setStyle(style)
            ready.uiSettings.isLogoEnabled = false
            ready.uiSettings.isCompassEnabled = false
            ready.uiSettings.isRotateGesturesEnabled = false
            ready.uiSettings.isTiltGesturesEnabled = false
            ready.cameraPosition = CameraPosition.Builder().target(initial ?: FirstLook).zoom(if (initial != null) PICKER_ZOOM else COUNTRY_ZOOM).build()
            ready.addOnCameraIdleListener { scope.launch { address = ready.cameraPosition.target?.let { addressAt(context, it) } } }
            map = ready
        }
    }
    LaunchedEffect(map) {
        val ready = map ?: return@LaunchedEffect
        if (initial == null) placeFor(context, query)?.let { ready.animateCamera(CameraUpdateFactory.newLatLngZoom(it, PICKER_ZOOM)) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, sheetGesturesEnabled = false) {
        Column(
            modifier = Modifier.navigationBarsPadding().imePadding().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text("Where is the house?", style = MaterialTheme.typography.titleLargeEmphasized)
            FlockrTextField(
                value = query,
                onValueChange = { query = it; searchError = null },
                placeholder = "Search an address or area",
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Search,
                isError = searchError != null,
                supportingText = searchError,
                keyboardActions = KeyboardActions(onSearch = {
                    scope.launch {
                        val found = placeFor(context, query)
                        if (found == null) {
                            searchError = "Nothing found for that. Try a street and a city."
                        } else {
                            haptics.select()
                            map?.animateCamera(CameraUpdateFactory.newLatLngZoom(found, PICKER_ZOOM))
                        }
                    }
                }),
                modifier = Modifier.fillMaxWidth(),
            )
            Box(Modifier.fillMaxWidth().aspectRatio(MAP_ASPECT).clip(MaterialTheme.shapes.large)) {
                AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
                Icon(
                    Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Center).size(IconSize.xl).offset(y = -IconSize.xl / 2),
                )
            }
            Text(
                address ?: "Drag the map so the pin sits on the house",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlockrPrimaryButton(
                text = "Use this spot",
                onClick = {
                    map?.cameraPosition?.target?.let { target -> onPick(PickedPlace(target.latitude, target.longitude, address)) }
                    scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss() }
                },
                enabled = map != null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** The address under [point], one line, or null when the geocoder has nothing or isn't available. */
private suspend fun addressAt(context: Context, point: LatLng): String? {
    if (!Geocoder.isPresent()) return null
    return suspendCancellableCoroutine { done ->
        Geocoder(context).getFromLocation(point.latitude, point.longitude, 1, object : Geocoder.GeocodeListener {
            override fun onGeocode(addresses: MutableList<Address>) { done.resume(addresses.firstOrNull()?.getAddressLine(0)) }
            override fun onError(errorMessage: String?) { done.resume(null) }
        })
    }
}

/** Where [query] is, or null when nothing matches or the geocoder isn't available. */
private suspend fun placeFor(context: Context, query: String): LatLng? {
    if (query.isBlank() || !Geocoder.isPresent()) return null
    return suspendCancellableCoroutine { done ->
        Geocoder(context).getFromLocationName(query, 1, object : Geocoder.GeocodeListener {
            override fun onGeocode(addresses: MutableList<Address>) { done.resume(addresses.firstOrNull()?.let { LatLng(it.latitude, it.longitude) }) }
            override fun onError(errorMessage: String?) { done.resume(null) }
        })
    }
}
