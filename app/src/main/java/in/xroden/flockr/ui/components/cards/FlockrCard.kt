package `in`.xroden.flockr.ui.components.cards

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Standard Flockr card component using Material 3 design
 */
@Composable
fun FlockrCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    outlined: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    if (outlined) {
        OutlinedCard(
            onClick = onClick ?: {},
            modifier = modifier,
            enabled = onClick != null,
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = CardDefaults.outlinedCardBorder(enabled = true)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    } else {
        Card(
            onClick = onClick ?: {},
            modifier = modifier,
            enabled = onClick != null,
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

