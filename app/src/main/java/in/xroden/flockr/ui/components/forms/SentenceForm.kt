/**
 * The pieces every form is built from: a cobalt header holding the one value the form is about, a
 * sentence of tappable words for everything else, and the save bar.
 */
package `in`.xroden.flockr.ui.components.forms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditNote
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import `in`.xroden.flockr.ui.components.HeroBackdrop
import `in`.xroden.flockr.ui.components.LightStatusBarIcons
import `in`.xroden.flockr.ui.components.buttons.FlockrPrimaryButton
import `in`.xroden.flockr.ui.theme.IconSize
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.ui.theme.flockrColors
import `in`.xroden.flockr.utils.currencySymbol
import `in`.xroden.flockr.utils.rememberHaptics

private const val PLACEHOLDER_ALPHA = 0.45f

/**
 * The top of a form: cobalt, running up under the status bar, with a small [title] naming what is
 * being done ("New expense") and [content] holding the value the form is about, typed large.
 */
@Composable
fun FormHero(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    LightStatusBarIcons()
    val colors = MaterialTheme.flockrColors
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.hero,
        contentColor = colors.onHero,
        shape = RoundedCornerShape(bottomStart = Spacing.xxxl, bottomEnd = Spacing.xxxl),
    ) {
        Box {
            HeroBackdrop(imageUrl = null, modifier = Modifier.matchParentSize())
            Column(
                modifier = Modifier.statusBarsPadding().padding(start = Spacing.xl, end = Spacing.xl, top = Spacing.lg, bottom = Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(title, style = MaterialTheme.typography.titleMediumEmphasized, color = colors.onHeroVariant)
                content()
            }
        }
    }
}

/**
 * An amount typed straight onto the hero at display size, after the currency's symbol. [autoFocus]
 * raises the keyboard on arrival, which is right for a new entry and wrong for an edit.
 */
@Composable
fun HeroAmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    currencyCode: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    autoFocus: Boolean = false,
) {
    val colors = MaterialTheme.flockrColors
    val style = MaterialTheme.typography.displayLargeEmphasized.copy(color = colors.onHero)
    HeroField(value, onValueChange, style, KeyboardType.Decimal, KeyboardCapitalization.None, "0", modifier, enabled, autoFocus, label = "Amount") { field ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(currencySymbol(currencyCode), style = MaterialTheme.typography.displaySmallEmphasized, color = colors.onHeroVariant)
            field()
        }
    }
}

/** A line of text typed straight onto the hero, such as what an expense was for or a chore's name. */
@Composable
fun HeroTextInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    autoFocus: Boolean = false,
    style: TextStyle = MaterialTheme.typography.headlineMediumEmphasized,
) {
    HeroField(
        value, onValueChange, style.copy(color = MaterialTheme.flockrColors.onHero), KeyboardType.Text, KeyboardCapitalization.Sentences,
        placeholder, modifier, enabled, autoFocus, label = placeholder,
    ) { field -> field() }
}

@Composable
private fun HeroField(
    value: String,
    onValueChange: (String) -> Unit,
    style: TextStyle,
    keyboardType: KeyboardType,
    capitalization: KeyboardCapitalization,
    placeholder: String,
    modifier: Modifier,
    enabled: Boolean,
    autoFocus: Boolean,
    label: String,
    frame: @Composable (field: @Composable () -> Unit) -> Unit,
) {
    val focus = remember { FocusRequester() }
    if (autoFocus) LaunchedEffect(Unit) { focus.requestFocus() }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(MaterialTheme.flockrColors.sun),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = capitalization),
        modifier = modifier.fillMaxWidth().focusRequester(focus).semantics { contentDescription = label },
        decorationBox = { inner ->
            frame {
                Box {
                    if (value.isEmpty()) Text(placeholder, style = style, color = style.color.copy(alpha = PLACEHOLDER_ALPHA), maxLines = 1)
                    inner()
                }
            }
        },
    )
}

/** A quieter line under the hero's input: the split preview, or why the amount doesn't parse. */
@Composable
fun HeroNote(text: String, isError: Boolean = false) {
    val colors = MaterialTheme.flockrColors
    Text(text, style = MaterialTheme.typography.bodyMediumEmphasized, color = if (isError) colors.sun else colors.onHeroVariant)
}

/**
 * The rest of the form as a sentence that wraps like prose: plain [SentenceWords] joined by
 * [SentenceToken]s the user taps to change, such as "Paid by **you** on **today**".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Sentence(modifier: Modifier = Modifier, content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        itemVerticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** The fixed words between a sentence's tokens. */
@Composable
fun SentenceWords(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/**
 * A value in the sentence, as a pill that opens its picker. [isUnset] shows it as a prompt, such as
 * "pick a category", in a quieter fill.
 */
@Composable
fun SentenceToken(text: String, onClick: () -> Unit, enabled: Boolean = true, icon: ImageVector? = null, isUnset: Boolean = false) {
    val haptics = rememberHaptics()
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = { haptics.select(); onClick() },
        enabled = enabled,
        shape = CircleShape,
        color = if (isUnset) colors.surfaceContainerHigh else colors.primaryContainer,
        contentColor = if (isUnset) colors.onSurfaceVariant else colors.onPrimaryContainer,
        modifier = Modifier.semantics { role = Role.DropdownList },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(IconSize.sm)) }
            Text(text, style = MaterialTheme.typography.titleLargeEmphasized)
        }
    }
}

/**
 * An optional note: an "Add a note" token until it is tapped or already has text, then a text field
 * in its place.
 */
@Composable
fun SentenceNote(value: String, onValueChange: (String) -> Unit, placeholder: String, enabled: Boolean = true) {
    var isOpen by remember { mutableStateOf(false) }
    if (!isOpen && value.isBlank()) {
        Sentence { SentenceToken("Add a note", onClick = { isOpen = true }, enabled = enabled, icon = Icons.Rounded.EditNote, isUnset = true) }
        return
    }
    FlockrTextField(
        value = value,
        onValueChange = onValueChange,
        label = "Note",
        placeholder = placeholder,
        singleLine = false,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
    )
}

/**
 * The save bar pinned under a form, above the keyboard when it is up. [secondary] adds a quieter
 * action beside the main one, such as "Save and add another".
 */
@Composable
fun FormSubmitBar(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    isLoading: Boolean,
    secondary: Pair<String, () -> Unit>? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = Spacing.lg, vertical = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        secondary?.let { (label, action) ->
            OutlinedButton(
                onClick = action,
                enabled = enabled && !isLoading,
                shapes = ButtonDefaults.shapesFor(ButtonDefaults.MediumContainerHeight),
                contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
            ) { Text(label, style = MaterialTheme.typography.titleSmallEmphasized, maxLines = 1) }
        }
        FlockrPrimaryButton(text = text, onClick = onClick, enabled = enabled, isLoading = isLoading, modifier = Modifier.weight(1f))
    }
}
