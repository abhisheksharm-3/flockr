package `in`.xroden.flockr.ui.components.loading

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Base shimmer effect composable
 */
@Composable
fun shimmerBrush(showShimmer: Boolean = true): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )

    if (!showShimmer) {
        return Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = Offset.Zero,
            end = Offset.Zero
        )
    }

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 500f, translateAnim - 500f),
        end = Offset(translateAnim, translateAnim)
    )
}

/**
 * Basic shimmer box
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(shimmerBrush())
    )
}

/**
 * Skeleton text line
 */
@Composable
fun SkeletonText(
    modifier: Modifier = Modifier,
    width: Dp = 150.dp,
    height: Dp = 16.dp
) {
    ShimmerBox(
        modifier = modifier
            .width(width)
            .height(height),
        shape = RoundedCornerShape(4.dp)
    )
}

/**
 * Skeleton for a card item
 */
@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier,
    height: Dp = 120.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(shimmerBrush())
    )
}

/**
 * Skeleton for list items with icon, title, and subtitle
 */
@Composable
fun SkeletonListItem(
    modifier: Modifier = Modifier,
    showTrailing: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Leading icon
        ShimmerBox(
            modifier = Modifier.size(48.dp),
            shape = CircleShape
        )
        
        // Text content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SkeletonText(width = 140.dp, height = 18.dp)
            SkeletonText(width = 200.dp, height = 14.dp)
        }
        
        // Trailing
        if (showTrailing) {
            SkeletonText(width = 60.dp, height = 20.dp)
        }
    }
}

/**
 * Skeleton for house cards on home screen
 */
@Composable
fun SkeletonHouseCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(shimmerBrush())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .width(50.dp)
                        .height(28.dp),
                    shape = CircleShape
                )
                ShimmerBox(
                    modifier = Modifier
                        .width(70.dp)
                        .height(28.dp),
                    shape = CircleShape
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonText(width = 180.dp, height = 24.dp)
                SkeletonText(width = 220.dp, height = 16.dp)
            }
        }
    }
}

/**
 * Skeleton for expense/finance cards
 */
@Composable
fun SkeletonExpenseCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonText(width = 120.dp, height = 18.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShimmerBox(
                    modifier = Modifier.size(width = 60.dp, height = 20.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                SkeletonText(width = 80.dp, height = 14.dp)
            }
        }
        SkeletonText(width = 70.dp, height = 22.dp)
    }
}

/**
 * Skeleton for dashboard stat cards
 */
@Composable
fun SkeletonStatCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(90.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SkeletonText(width = 80.dp, height = 12.dp)
            SkeletonText(width = 100.dp, height = 28.dp)
        }
    }
}

/**
 * Skeleton for feature navigation cards
 */
@Composable
fun SkeletonFeatureCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerBox(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(12.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SkeletonText(width = 140.dp, height = 18.dp)
            SkeletonText(width = 200.dp, height = 14.dp)
        }
        ShimmerBox(
            modifier = Modifier.size(24.dp),
            shape = CircleShape
        )
    }
}

/**
 * Full home screen skeleton
 */
@Composable
fun HomeScreenSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Section title
        SkeletonText(width = 140.dp, height = 18.dp)
        
        // House cards
        repeat(2) {
            SkeletonHouseCard()
        }
    }
}

/**
 * Full expense dashboard skeleton
 */
@Composable
fun ExpenseDashboardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonText(width = 180.dp, height = 32.dp)
            SkeletonText(width = 280.dp, height = 18.dp)
        }
        
        // Stat cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SkeletonStatCard(modifier = Modifier.weight(1f))
            SkeletonStatCard(modifier = Modifier.weight(1f))
        }
        
        // Section title
        SkeletonText(width = 100.dp, height = 22.dp)
        
        // Feature cards
        repeat(3) {
            SkeletonFeatureCard()
        }
    }
}

/**
 * List screen skeleton (chores, shopping, expenses list)
 */
@Composable
fun ListScreenSkeleton(
    modifier: Modifier = Modifier,
    itemCount: Int = 5
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(itemCount) {
            SkeletonListItem()
        }
    }
}

/**
 * Chat screen skeleton
 */
@Composable
fun ChatScreenSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(6) { index ->
            val isLeft = index % 2 == 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isLeft) Arrangement.Start else Arrangement.End
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .widthIn(min = 100.dp, max = 250.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

/**
 * Details screen skeleton (house details, expense details)
 */
@Composable
fun DetailScreenSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Hero/header
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(24.dp)
        )
        
        // Content sections
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonText(width = 200.dp, height = 28.dp)
            SkeletonText(width = 300.dp, height = 16.dp)
        }
        
        // List items
        repeat(3) {
            SkeletonListItem()
        }
    }
}
