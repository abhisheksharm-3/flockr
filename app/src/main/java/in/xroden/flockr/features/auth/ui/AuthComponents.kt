/** The fields, buttons and messages the sign-in, sign-up and onboarding screens share. */
package `in`.xroden.flockr.features.auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import `in`.xroden.flockr.R
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics

private val GoogleButtonHeight = ButtonDefaults.MediumContainerHeight

/**
 * A text field for the auth forms, shaped like the app's other fields. [onDone] makes the
 * keyboard's action submit the form; without it the action moves to the next field. [autofill]
 * tells password managers what to fill in. [error] replaces [supportingText] while it is set.
 */
@Composable
internal fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    autofill: ContentType,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    error: String? = null,
    supportingText: String? = null,
    enabled: Boolean = true,
    onDone: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().semantics { contentType = autofill },
        enabled = enabled,
        label = { Text(label, fontWeight = MaterialTheme.typography.labelLargeEmphasized.fontWeight) },
        leadingIcon = { Icon(leadingIcon, contentDescription = null) },
        trailingIcon = trailingIcon,
        isError = error != null,
        supportingText = (error ?: supportingText)?.let { { Text(it) } },
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            keyboardType = keyboardType,
            imeAction = if (onDone != null) ImeAction.Done else ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        singleLine = true,
        shape = MaterialTheme.shapes.large,
    )
}

/** A password field with a show and hide toggle, hidden by default. */
@Composable
internal fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    autofill: ContentType,
    modifier: Modifier = Modifier,
    error: String? = null,
    supportingText: String? = null,
    enabled: Boolean = true,
    onDone: (() -> Unit)? = null,
) {
    val haptics = rememberHaptics()
    var isVisible by rememberSaveable { mutableStateOf(false) }
    AuthTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        leadingIcon = Icons.Rounded.Lock,
        autofill = autofill,
        modifier = modifier,
        keyboardType = KeyboardType.Password,
        error = error,
        supportingText = supportingText,
        enabled = enabled,
        onDone = onDone,
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { isVisible = !isVisible; haptics.toggle(isVisible) }) {
                Icon(
                    imageVector = if (isVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (isVisible) "Hide password" else "Show password",
                )
            }
        },
    )
}

/** Sized off the same Expressive medium button scale as the primary button it sits under. */
@Composable
internal fun GoogleSignInButton(onClick: () -> Unit, enabled: Boolean, isLoading: Boolean, modifier: Modifier = Modifier) {
    val haptics = rememberHaptics()
    val iconSize = ButtonDefaults.iconSizeFor(GoogleButtonHeight)
    OutlinedButton(
        onClick = { haptics.tap(); onClick() },
        shapes = ButtonDefaults.shapesFor(GoogleButtonHeight),
        modifier = modifier.fillMaxWidth().heightIn(min = GoogleButtonHeight),
        enabled = enabled && !isLoading,
        contentPadding = ButtonDefaults.contentPaddingFor(GoogleButtonHeight),
    ) {
        if (isLoading) {
            LoadingIndicator(modifier = Modifier.size(iconSize), color = LocalContentColor.current)
        } else {
            Image(painter = painterResource(R.drawable.ic_google), contentDescription = null, modifier = Modifier.size(iconSize))
        }
        Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(GoogleButtonHeight)))
        Text("Continue with Google", style = MaterialTheme.typography.titleMediumEmphasized)
    }
}

@Composable
internal fun OrDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        HorizontalDivider(Modifier.weight(1f))
        Text("or", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider(Modifier.weight(1f))
    }
}

/** A failure from the auth server, announced to screen readers as soon as it appears. */
@Composable
internal fun AuthErrorMessage(message: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = null)
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
