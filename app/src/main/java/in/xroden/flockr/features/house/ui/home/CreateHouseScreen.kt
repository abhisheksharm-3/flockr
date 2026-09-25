/** Creating a house in four steps: name and photo, address, money and dates, then a review. */
package `in`.xroden.flockr.features.house.ui.home

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import `in`.xroden.flockr.core.network.userMessage
import `in`.xroden.flockr.core.validation.Validators
import `in`.xroden.flockr.features.house.model.DEFAULT_CURRENCY_CODE
import `in`.xroden.flockr.features.house.model.DateLayout
import `in`.xroden.flockr.features.house.presentation.CreateHouseUiState
import `in`.xroden.flockr.features.house.presentation.HomeViewModel
import `in`.xroden.flockr.features.house.presentation.HouseEvent
import `in`.xroden.flockr.features.house.ui.HouseLocaleFields
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.buttons.FlockrPrimaryButton
import `in`.xroden.flockr.ui.components.forms.FormSectionCard
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.ui.theme.ComponentHeight
import `in`.xroden.flockr.ui.theme.IconSize
import `in`.xroden.flockr.ui.theme.Motion
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.ui.theme.spatialSpec
import `in`.xroden.flockr.utils.SUPPORTED_CURRENCIES
import `in`.xroden.flockr.utils.example
import `in`.xroden.flockr.utils.rememberHaptics
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val STEP_TITLES = listOf("Name and photo", "Address", "Money and dates", "Review")
private val LAST_STEP = STEP_TITLES.lastIndex
private const val DEFAULT_DATE_FORMAT = "dd/MM/yyyy"

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

    var step by rememberSaveable { mutableIntStateOf(0) }
    var houseName by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var imageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var currency by rememberSaveable { mutableStateOf(defaultCurrency()) }
    var dateFormat by rememberSaveable { mutableStateOf(DEFAULT_DATE_FORMAT) }
    var firstDayOfWeek by rememberSaveable { mutableIntStateOf(defaultFirstDayOfWeek()) }
    var timezone by rememberSaveable { mutableStateOf(kotlinx.datetime.TimeZone.currentSystemDefault().id) }

    val isCreating = createState is CreateHouseUiState.Creating
    val nameCheck = Validators.validateHouseName(houseName)
    val canContinue = step != 0 || nameCheck.isSuccess
    val goBack: () -> Unit = { if (step > 0) step-- else onNavigateBack() }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) imageUri = uri
    }

    BackHandler(enabled = step > 0 && !isCreating) { step-- }

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
        topBar = {
            FlockrTopAppBar(
                title = "Create a house",
                subtitle = "Step ${step + 1} of ${STEP_TITLES.size}: ${STEP_TITLES[step]}",
                onNavigateBack = goBack,
            )
        },
        bottomBar = {
            FlockrPrimaryButton(
                text = if (step == LAST_STEP) "Create house" else "Next",
                onClick = {
                    if (step < LAST_STEP) {
                        step++
                    } else {
                        viewModel.createHouse(
                            houseName.trim(),
                            address.trim().ifBlank { null },
                            null,
                            null,
                            currency,
                            dateFormat,
                            firstDayOfWeek,
                            timezone,
                            headerImageBytes = imageBytes
                        )
                    }
                },
                enabled = canContinue,
                isLoading = isCreating,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Spacing.xl, vertical = Spacing.lg),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val progress by animateFloatAsState(targetValue = (step + 1f) / STEP_TITLES.size, animationSpec = Motion.effects)
        val slideSpec = spatialSpec<IntOffset>()
        val fadeSpec = Motion.effects
        Column(Modifier.fillMaxSize().padding(padding)) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xl),
            )
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(slideSpec) { it * direction } + fadeIn(fadeSpec)) togetherWith
                        (slideOutHorizontally(slideSpec) { -it * direction } + fadeOut(fadeSpec))
                },
                label = "create house step",
            ) { current ->
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.xl, vertical = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                ) {
                    when (current) {
                        0 -> FormSectionCard(icon = Icons.Rounded.Home, title = "Name and photo") {
                            FlockrTextField(
                                value = houseName,
                                onValueChange = { houseName = it },
                                label = "House name",
                                placeholder = "Maple Street flat",
                                isError = houseName.isNotBlank() && nameCheck.isFailure,
                                supportingText = nameCheck.exceptionOrNull()?.takeIf { houseName.isNotBlank() }?.userMessage(),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            PhotoPicker(
                                imageUri = imageUri,
                                onPick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                onRemove = { imageUri = null },
                            )
                        }
                        1 -> FormSectionCard(icon = Icons.Rounded.LocationOn, title = "Address") {
                            Text(
                                "Optional. Leave it empty if you'd rather not share it.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            FlockrTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = "Address",
                                singleLine = false,
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        2 -> FormSectionCard(icon = Icons.Rounded.Payments, title = "Money and dates") {
                            Text(
                                "Every amount is shown in this currency, and \"today\" and \"overdue\" follow this time zone. The currency is fixed once money is recorded.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            HouseLocaleFields(
                                currencyCode = currency,
                                onCurrencyChange = { currency = it },
                                dateFormat = dateFormat,
                                onDateFormatChange = { dateFormat = it },
                                firstDayOfWeek = firstDayOfWeek,
                                onFirstDayOfWeekChange = { firstDayOfWeek = it },
                                timezone = timezone,
                                onTimezoneChange = { timezone = it },
                            )
                        }
                        else -> ReviewStep(
                            houseName = houseName.trim(),
                            address = address.trim(),
                            imageUri = imageUri,
                            currency = currency,
                            dateFormat = dateFormat,
                            firstDayOfWeek = firstDayOfWeek,
                            timezone = timezone,
                        )
                    }
                }
            }
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

@Composable
private fun PhotoPicker(imageUri: Uri?, onPick: () -> Unit, onRemove: () -> Unit) {
    val haptics = rememberHaptics()
    Surface(
        onClick = { haptics.tap(); onPick() },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth().height(ComponentHeight.cardSmall),
    ) {
        if (imageUri != null) {
            AsyncImage(
                model = imageUri,
                contentDescription = "Header photo. Tap to change it.",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm, Alignment.CenterVertically),
            ) {
                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(IconSize.lg))
                Text("Add a header photo (optional)", style = MaterialTheme.typography.labelLargeEmphasized)
            }
        }
    }
    if (imageUri != null) {
        TextButton(onClick = { haptics.tap(); onRemove() }) { Text("Remove photo") }
    }
}

@Composable
private fun ReviewStep(
    houseName: String,
    address: String,
    imageUri: Uri?,
    currency: String,
    dateFormat: String,
    firstDayOfWeek: Int,
    timezone: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        val headerModifier = Modifier.fillMaxWidth().height(ComponentHeight.cardMedium)
        if (imageUri != null) {
            AsyncImage(model = imageUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = headerModifier)
        } else {
            HouseImage(imageUrl = null, seed = houseName, modifier = headerModifier)
        }
        Column(Modifier.padding(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(houseName, style = MaterialTheme.typography.headlineSmallEmphasized)
            ReviewRow("Address", address.ifBlank { "None" })
            ReviewRow("Currency", currency)
            ReviewRow("Dates", DateLayout.fromPattern(dateFormat)?.example() ?: dateFormat)
            ReviewRow("Weeks start on", dayName(firstDayOfWeek))
            ReviewRow("Time zone", timezone)
        }
    }
    Text(
        "Once it's made you'll get an invite code to share with your housemates.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMediumEmphasized, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
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
        confirmButton = { TextButton(onClick = onOpen) { Text("Open house") } },
    )
}

private fun dayName(firstDayOfWeek: Int): String =
    DayOfWeek.of(if (firstDayOfWeek == 0) 7 else firstDayOfWeek).getDisplayName(TextStyle.FULL, Locale.getDefault())

/** The device's currency when the app supports it, so most houses need not change it. */
private fun defaultCurrency(): String =
    runCatching { java.util.Currency.getInstance(java.util.Locale.getDefault()).currencyCode }.getOrNull()
        ?.takeIf { it in SUPPORTED_CURRENCIES } ?: DEFAULT_CURRENCY_CODE

/** The device locale's first day of the week, as `house_config` numbers it: 0 is Sunday. */
private fun defaultFirstDayOfWeek(): Int =
    java.time.temporal.WeekFields.of(java.util.Locale.getDefault()).firstDayOfWeek.value % 7
