/** A strip of page colour behind the status bar, for screens whose cobalt header scrolls away, and the scrolling column that uses it. */
package `in`.xroden.flockr.ui.components

import android.app.Activity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import `in`.xroden.flockr.ui.theme.Motion

/** True once the list's first item, the hero, has scrolled fully off screen. */
val LazyListState.isHeroScrolledAway: Boolean get() = firstVisibleItemIndex > 0

private const val DARK_PAGE_LUMINANCE = 0.5f

/**
 * Fades a page-coloured strip in behind the status bar once [isHeroGone], so scrolled content never
 * runs under the clock, and switches the status bar icons to suit the page while it shows. Place it
 * last in a full-screen `Box`, over the scrolling content.
 */
@Composable
fun HeroStatusBarScrim(isHeroGone: Boolean, modifier: Modifier = Modifier) {
    val page = MaterialTheme.colorScheme.background
    val alpha by animateFloatAsState(if (isHeroGone) 1f else 0f, Motion.effectsFast)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val controller = WindowCompat.getInsetsController((view.context as Activity).window, view)
            controller.isAppearanceLightStatusBars = isHeroGone && page.luminance() > DARK_PAGE_LUMINANCE
        }
    }
    Box(modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).graphicsLayer { this.alpha = alpha }.background(page))
}

/**
 * A scrolling column that opens with [hero] and keeps the status bar clear: once the hero has
 * scrolled up past the status bar, [HeroStatusBarScrim] fades in over whatever scrolls beneath it.
 * For a screen built on a plain scrolling column rather than a lazy list, such as a form.
 */
@Composable
fun HeroColumn(
    hero: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scroll = rememberScrollState()
    var heroHeight by remember { mutableIntStateOf(0) }
    val statusBar = WindowInsets.statusBars.getTop(LocalDensity.current)
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll), verticalArrangement = verticalArrangement) {
            Box(Modifier.onSizeChanged { heroHeight = it.height }) { hero() }
            content()
        }
        val isHeroGone by remember(statusBar) { derivedStateOf { heroHeight > 0 && scroll.value >= heroHeight - statusBar } }
        HeroStatusBarScrim(isHeroGone = isHeroGone)
    }
}
