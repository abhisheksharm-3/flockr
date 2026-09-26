/** Where a house is, as it appears in the house forms: a map of the spot, or an invitation to pin it. */
package `in`.xroden.flockr.features.house.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import `in`.xroden.flockr.ui.components.HouseMap
import `in`.xroden.flockr.ui.components.LocationPickerSheet
import `in`.xroden.flockr.ui.components.PickedPlace
import `in`.xroden.flockr.ui.components.forms.Sentence
import `in`.xroden.flockr.ui.components.forms.SentenceToken
import `in`.xroden.flockr.ui.components.latLngOf
import `in`.xroden.flockr.ui.theme.Spacing

private const val MAP_ASPECT = 2f

/**
 * The house's pinned spot as a small still map with a way to move the pin, or, with nothing pinned
 * yet, a token that opens the picker. The picker starts from the typed [address] when there is no
 * pin, so most people only nudge it. A pinned house gets its street as a live backdrop wherever it
 * has no photo.
 */
@Composable
fun HouseLocationRow(latitude: Double?, longitude: Double?, address: String, enabled: Boolean, onPick: (PickedPlace) -> Unit) {
    var isPicking by remember { mutableStateOf(false) }
    val location = latLngOf(latitude, longitude)
    if (location == null) {
        Sentence { SentenceToken("Pin the house on a map", onClick = { isPicking = true }, enabled = enabled, icon = Icons.Rounded.LocationOn, isUnset = true) }
    } else {
        Column(Modifier.padding(horizontal = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Box(Modifier.fillMaxWidth().aspectRatio(MAP_ASPECT).clip(MaterialTheme.shapes.large)) { HouseMap(location) }
            TextButton(onClick = { isPicking = true }, enabled = enabled, shapes = ButtonDefaults.shapes()) { Text("Move the pin") }
        }
    }
    if (isPicking) {
        LocationPickerSheet(initial = location, initialQuery = address, onPick = onPick, onDismiss = { isPicking = false })
    }
}
