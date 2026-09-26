/** Creating a house: its name typed large on cobalt, then how money and dates read as a sentence, then the address and a photo. */
package `in`.xroden.flockr.features.house.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.core.validation.Validators
import `in`.xroden.flockr.features.house.model.DEFAULT_CURRENCY_CODE
import `in`.xroden.flockr.features.house.presentation.CreateHouseUiState
import `in`.xroden.flockr.features.house.presentation.HomeViewModel
import `in`.xroden.flockr.features.house.presentation.HouseEvent
import `in`.xroden.flockr.features.house.ui.HouseLocaleSentence
import `in`.xroden.flockr.features.house.ui.HouseLocationRow
import `in`.xroden.flockr.ui.components.HeroColumn
import `in`.xroden.flockr.ui.components.forms.FormHero
import `in`.xroden.flockr.ui.components.forms.FormSubmitBar
import `in`.xroden.flockr.ui.components.forms.HeroNote
import `in`.xroden.flockr.ui.components.forms.HeroTextInput
import `in`.xroden.flockr.ui.components.forms.Sentence
import `in`.xroden.flockr.ui.components.forms.SentenceToken
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.ui.theme.ComponentHeight
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.SUPPORTED_CURRENCIES
import `in`.xroden.flockr.utils.rememberHaptics
import java.time.temporal.WeekFields
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone

private const val DEFAULT_DATE_FORMAT = "dd/MM/yyyy"

/**
 * The new-house form on one page: every choice reads back in the sentence, so there is nothing to
 * review before creating. The invite code arrives in the dialog once the house exists.
 */
@Composable
fun CreateHouseScreen(
    onHouseCreated: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val haptics = rememberHaptics()
    val createState by viewModel.createState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val contentResolver = LocalContext.current.contentResolver

    var houseName by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var latitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var longitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var imageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var currency by rememberSaveable { mutableStateOf(defaultCurrency()) }
    var dateFormat by rememberSaveable { mutableStateOf(DEFAULT_DATE_FORMAT) }
    var firstDayOfWeek by rememberSaveable { mutableIntStateOf(defaultFirstDayOfWeek()) }
    var timezone by rememberSaveable { mutableStateOf(TimeZone.currentSystemDefault().id) }

    val isCreating = createState is CreateHouseUiState.Creating
    val nameCheck = Validators.validateHouseName(houseName)
    val nameError = nameCheck.exceptionOrNull()?.takeIf { houseName.isNotBlank() }?.userMessage()

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) imageUri = uri
    }
    val pickPhoto = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    LaunchedEffect(imageUri) {
        val uri = imageUri ?: run { imageBytes = null; return@LaunchedEffect }
        imageBytes = withContext(Dispatchers.IO) {
            runCatching { contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
        }
        if (imageBytes == null) {
            imageUri = null
            haptics.error()
            snackbarHostState.showSnackbar("That photo couldn't be opened. Try another one.")
        }
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is HouseEvent.Failed) {
                haptics.error()
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }
    LaunchedEffect(createState) {
        if (createState is CreateHouseUiState.Created) haptics.success()
    }

    Scaffold(
        bottomBar = {
            FormSubmitBar(
                text = "Create house",
                onClick = {
                    viewModel.createHouse(
                        houseName.trim(),
                        address.trim().ifBlank { null },
                        latitude,
                        longitude,
                        currency,
                        dateFormat,
                        firstDayOfWeek,
                        timezone,
                        headerImageBytes = imageBytes
                    )
                },
                enabled = nameCheck.isSuccess,
                isLoading = isCreating,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        HeroColumn(
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
            hero = {
                FormHero("New house") {
                    HeroTextInput(
                        value = houseName,
                        onValueChange = { houseName = it },
                        placeholder = "Name your house",
                        enabled = !isCreating,
                        autoFocus = true,
                        style = MaterialTheme.typography.headlineLargeEmphasized,
                    )
                    HeroNote(nameError ?: "Everyone in it sees this name on their home screen", isError = nameError != null)
                }
            },
        ) {
            HouseLocaleSentence(
                currencyCode = currency,
                onCurrencyChange = { currency = it },
                dateFormat = dateFormat,
                onDateFormatChange = { dateFormat = it },
                firstDayOfWeek = firstDayOfWeek,
                onFirstDayOfWeekChange = { firstDayOfWeek = it },
                timezone = timezone,
                onTimezoneChange = { timezone = it },
                enabled = !isCreating,
            )
            FlockrTextField(
                value = address,
                onValueChange = { address = it },
                label = "Address",
                placeholder = "Optional. Leave it empty to keep it private",
                singleLine = false,
                maxLines = 3,
                enabled = !isCreating,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
            )
            HouseLocationRow(latitude, longitude, address, enabled = !isCreating) { place ->
                latitude = place.latitude
                longitude = place.longitude
                if (address.isBlank()) address = place.address.orEmpty()
            }
            HousePhoto(imageUri = imageUri, enabled = !isCreating, onPick = pickPhoto, onRemove = { imageUri = null })
        }
    }

    (createState as? CreateHouseUiState.Created)?.let { created ->
        HouseCreatedDialog(
            name = created.house.name,
            inviteCode = created.house.inviteCode,
            photoUploaded = created.photoUploaded,
            onOpen = { onHouseCreated(created.house.id) },
        )
    }
}

/**
 * An "Add a photo" token until one is chosen, then the photo at the size its header shows it, which
 * is itself the target for changing it.
 */
@Composable
private fun HousePhoto(imageUri: Uri?, enabled: Boolean, onPick: () -> Unit, onRemove: () -> Unit) {
    val haptics = rememberHaptics()
    if (imageUri == null) {
        Sentence { SentenceToken("Add a photo", onClick = onPick, enabled = enabled, icon = Icons.Rounded.AddPhotoAlternate, isUnset = true) }
        return
    }
    Column(Modifier.padding(horizontal = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        AsyncImage(
            model = imageUri,
            contentDescription = "Header photo. Tap to change it.",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(ComponentHeight.cardMedium)
                .clip(MaterialTheme.shapes.extraLarge)
                .clickable(enabled = enabled) { haptics.tap(); onPick() },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            TextButton(onClick = { haptics.tap(); onPick() }, enabled = enabled, shapes = ButtonDefaults.shapes()) { Text("Change photo") }
            TextButton(onClick = { haptics.tap(); onRemove() }, enabled = enabled, shapes = ButtonDefaults.shapes()) { Text("Remove photo") }
        }
    }
}

/** Shown once the house exists, with the code to share, before opening it. */
@Composable
private fun HouseCreatedDialog(name: String, inviteCode: String?, photoUploaded: Boolean, onOpen: () -> Unit) {
    AlertDialog(
        onDismissRequest = onOpen,
        title = { Text("$name is ready") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                if (inviteCode != null) {
                    Text("Share this code so your housemates can join.", style = MaterialTheme.typography.bodyMedium)
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(inviteCode, style = MaterialTheme.typography.headlineMediumEmphasized, color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Text("You can invite housemates from the house's settings.", style = MaterialTheme.typography.bodyMedium)
                }
                if (!photoUploaded) {
                    Text(
                        "The header photo didn't upload. You can add it again in the house's settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onOpen, shapes = ButtonDefaults.shapes()) { Text("Open house") } },
    )
}

/** The device's currency when the app supports it, so most houses need not change it. */
private fun defaultCurrency(): String =
    runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrNull()
        ?.takeIf { it in SUPPORTED_CURRENCIES } ?: DEFAULT_CURRENCY_CODE

/** The device locale's first day of the week, as `house_config` numbers it: 0 is Sunday. */
private fun defaultFirstDayOfWeek(): Int =
    WeekFields.of(Locale.getDefault()).firstDayOfWeek.value % 7
