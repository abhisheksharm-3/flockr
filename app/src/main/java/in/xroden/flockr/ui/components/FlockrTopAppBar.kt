/** The app's screen header, so every screen shares one title treatment and one back affordance. */
package `in`.xroden.flockr.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable

/**
 * A flexible top app bar that collapses from a large title as the content scrolls under it, when
 * given a [scrollBehavior] connected to that content.
 *
 * The bar matches the page until content scrolls under it, then tints, so there is no seam at rest.
 * [onNavigateBack] adds the back arrow; leave it null on a top-level screen. Going back is
 * navigation, so it fires no haptic.
 */
@Composable
fun FlockrTopAppBar(
    title: String,
    onNavigateBack: (() -> Unit)?,
    subtitle: String? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    MediumFlexibleTopAppBar(
        title = { Text(title) },
        subtitle = subtitle?.let { { Text(it) } },
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
}
