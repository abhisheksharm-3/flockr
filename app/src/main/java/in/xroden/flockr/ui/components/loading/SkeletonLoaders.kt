package `in`.xroden.flockr.ui.components.loading

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import `in`.xroden.flockr.ui.theme.Motion
import `in`.xroden.flockr.ui.theme.PillShape
import `in`.xroden.flockr.ui.theme.Spacing
import kotlinx.coroutines.delay

private const val PulseIntervalMillis = 700L
private const val PulseAlphaLow = 0.2f
private const val PulseAlphaHigh = 0.55f

/**
 * The placeholder fill, breathing between [PulseAlphaLow] and [PulseAlphaHigh] so a skeleton reads
 * as "still loading" rather than as empty content.
 */
@Composable
private fun pulsingPlaceholderColor(): Color {
    var atPeak by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            atPeak = !atPeak
            delay(PulseIntervalMillis)
        }
    }
    val alpha by animateFloatAsState(
        targetValue = if (atPeak) PulseAlphaHigh else PulseAlphaLow,
        animationSpec = Motion.effectsSlow,
        label = "skeletonPulse"
    )
    return MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
}

@Composable
private fun SkeletonBlock(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraSmall
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(pulsingPlaceholderColor())
    )
}

@Composable
private fun SkeletonLine(width: Dp, height: Dp) {
    SkeletonBlock(
        modifier = Modifier
            .width(width)
            .height(height)
    )
}

@Composable
private fun SkeletonListItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        SkeletonBlock(modifier = Modifier.size(48.dp), shape = CircleShape)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            SkeletonLine(width = 140.dp, height = 18.dp)
            SkeletonLine(width = 200.dp, height = 14.dp)
        }
        SkeletonLine(width = 60.dp, height = 20.dp)
    }
}

@Composable
private fun SkeletonHouseCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(Spacing.xl),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SkeletonBlock(
                modifier = Modifier
                    .width(50.dp)
                    .height(28.dp),
                shape = PillShape
            )
            SkeletonBlock(
                modifier = Modifier
                    .width(70.dp)
                    .height(28.dp),
                shape = PillShape
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SkeletonLine(width = 180.dp, height = 24.dp)
            SkeletonLine(width = 220.dp, height = 16.dp)
        }
    }
}

/** Stands in for the house list on the home screen while it loads. */
@Composable
fun HomeScreenSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxl)
    ) {
        SkeletonLine(width = 140.dp, height = 18.dp)
        repeat(2) {
            SkeletonHouseCard()
        }
    }
}

/** Stands in for any icon-title-subtitle list while it loads. */
@Composable
fun ListScreenSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 5
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        repeat(itemCount) {
            SkeletonListItem()
        }
    }
}

/** Stands in for a message thread while it loads, alternating incoming and outgoing bubbles. */
@Composable
fun ChatScreenSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        repeat(6) { index ->
            val isIncoming = index % 2 == 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isIncoming) Arrangement.Start else Arrangement.End
            ) {
                SkeletonBlock(
                    modifier = Modifier
                        .width(if (isIncoming) 220.dp else 160.dp)
                        .height(48.dp),
                    shape = MaterialTheme.shapes.large
                )
            }
        }
    }
}

/** Stands in for a hero-plus-details screen while it loads. */
@Composable
fun DetailScreenSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl)
    ) {
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = MaterialTheme.shapes.extraLarge
        )
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SkeletonLine(width = 200.dp, height = 28.dp)
            SkeletonLine(width = 300.dp, height = 16.dp)
        }
        repeat(3) {
            SkeletonListItem()
        }
    }
}
