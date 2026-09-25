package `in`.xroden.flockr.ui.components.forms

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.ui.theme.spatialSpec
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.Switch
import `in`.xroden.flockr.utils.rememberHaptics

private const val IconContainerAlpha = 0.12f

/**
 * Groups related form fields under a shaped, tinted icon and a section title.
 *
 * The content column animates its own height against the theme's spatial spec, so a section that
 * reveals or hides fields as the form is filled in grows into the new size instead of jumping.
 *
 * @param iconTint colours both the icon and its container, so sections can tell themselves apart.
 */
@Composable
fun FormSectionCard(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.largeIncreased
    ) {
        Column(
            modifier = Modifier
                .padding(Spacing.xl)
                .animateContentSize(animationSpec = spatialSpec()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            SectionHeader(icon = icon, title = title, iconTint = iconTint)
            content()
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String, iconTint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Surface(
            shape = MaterialShapes.Cookie4Sided.toShape(),
            color = iconTint.copy(alpha = IconContainerAlpha)
        ) {
            Icon(icon, null, modifier = Modifier.padding(Spacing.sm), tint = iconTint)
        }
        Text(title, style = MaterialTheme.typography.titleMediumEmphasized)
    }
}

/**
 * A labelled switch, for a form option that is either on or off. The whole row is the touch target,
 * and toggling fires the direction-specific haptic.
 */
@Composable
fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    val haptics = rememberHaptics()
    fun toggleTo(value: Boolean) {
        haptics.toggle(value)
        onCheckedChange(value)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = ::toggleTo),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            Text(title, style = MaterialTheme.typography.titleSmallEmphasized)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
