/** A band across the top of the app while the phone has no internet, saying what is on screen is saved. */
package `in`.xroden.flockr.ui.components

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import `in`.xroden.flockr.ui.theme.Spacing

@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = !rememberIsOnline(), modifier = modifier, enter = expandVertically(), exit = shrinkVertically()) {
        Text(
            text = "You're offline. Showing what was saved; changes need a connection.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .statusBarsPadding()
                .padding(horizontal = Spacing.lg, vertical = Spacing.xs)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/** Whether the default network can reach the internet, following it as it changes. */
@Composable
private fun rememberIsOnline(): Boolean {
    val connectivity = LocalContext.current.getSystemService(ConnectivityManager::class.java)
    var isOnline by remember {
        mutableStateOf(connectivity.getNetworkCapabilities(connectivity.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true)
    }
    DisposableEffect(connectivity) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                isOnline = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }

            override fun onLost(network: Network) {
                isOnline = false
            }
        }
        connectivity.registerDefaultNetworkCallback(callback)
        onDispose { connectivity.unregisterNetworkCallback(callback) }
    }
    return isOnline
}
