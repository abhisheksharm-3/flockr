/** The launch loader, drenched in the hero cobalt so the app opens the same colour the welcome screen is, and the logo lockup the full-cobalt screens share. */
package `in`.xroden.flockr.ui.components.loading

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import `in`.xroden.flockr.R
import `in`.xroden.flockr.ui.components.HeroBackdrop
import `in`.xroden.flockr.ui.components.LightStatusBarIcons
import `in`.xroden.flockr.ui.theme.IconSize
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.ui.theme.flockrColors

/**
 * The opaque full-screen loader shown while the app resolves where to send the user.
 *
 * Covers whatever is already composed underneath, so the half-built destination behind it never
 * shows through.
 */
@Composable
fun FlockrSplashLoader(modifier: Modifier = Modifier) {
    LightStatusBarIcons()
    Box(modifier.fillMaxSize().semantics { contentDescription = "Loading" }) {
        HeroBackdrop(imageUrl = null, modifier = Modifier.matchParentSize())
        FlockrLockup(markSize = IconSize.display, style = MaterialTheme.typography.displayMediumEmphasized, modifier = Modifier.align(Alignment.Center))
        LoadingIndicator(
            color = MaterialTheme.flockrColors.sun,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = Spacing.xxxxl),
        )
    }
}

/** The Flockr mark beside its wordmark, in the hero's text colour; the wordmark is what a screen reader hears. */
@Composable
fun FlockrLockup(markSize: Dp, style: TextStyle, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Image(painter = painterResource(R.drawable.logo), contentDescription = null, modifier = Modifier.size(markSize))
        Text(stringResource(R.string.app_name), style = style, color = MaterialTheme.flockrColors.onHero)
    }
}
