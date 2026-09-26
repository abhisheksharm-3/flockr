/** The app's screen header, so every screen shares one title treatment and one back affordance. */
package `in`.xroden.flockr.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable

/**
 * A flexible top app bar that collapses from a large title as the content scrolls under it, when
 * given a [scrollBehavior] connected to that content.
 *
 * The bar matches the page until content scrolls under it, then tints, so there is no seam at rest.
 * It has no back arrow: Android's system back gesture and button already go back from every screen.
 */
@Composable
fun FlockrTopAppBar(
    title: String,
    subtitle: String? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    MediumFlexibleTopAppBar(
        title = { Text(title) },
        subtitle = subtitle?.let { { Text(it) } },
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    )
}
