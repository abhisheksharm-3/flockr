/** A housemate's picture, or their initial on a tinted circle when they have none. */
package `in`.xroden.flockr.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import `in`.xroden.flockr.ui.theme.ComponentHeight
import `in`.xroden.flockr.ui.theme.flockrColors

/**
 * The avatar is decorative: the name beside it is what a screen reader announces. Its colour comes
 * from the first name, so a person looks the same whether a screen passes their full or short name.
 */
@Composable
fun MemberAvatar(
    name: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = ComponentHeight.avatar,
) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (name.trim().substringBefore(' ').lowercase().hashCode().mod(5)) {
        0 -> colors.primaryContainer to colors.onPrimaryContainer
        1 -> MaterialTheme.flockrColors.sun to MaterialTheme.flockrColors.onSun
        2 -> colors.tertiaryContainer to colors.onTertiaryContainer
        3 -> colors.errorContainer to colors.onErrorContainer
        else -> colors.secondaryContainer to colors.onSecondaryContainer
    }
    Surface(modifier = modifier.size(size), shape = CircleShape, color = container) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name.trim().firstOrNull()?.uppercase() ?: "?",
                style = if (size > ComponentHeight.avatar) MaterialTheme.typography.titleLargeEmphasized else MaterialTheme.typography.titleMediumEmphasized,
                color = content,
            )
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
