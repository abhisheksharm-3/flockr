/** The house's shared documents and the viewer's personal ones, in two tabs, with upload, open, download and delete. */
package `in`.xroden.flockr.features.documents.ui

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderShared
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.core.storage.StorageRepository
import `in`.xroden.flockr.core.validation.Validators
import `in`.xroden.flockr.features.documents.domain.usecase.UploadDocumentUseCase
import `in`.xroden.flockr.features.documents.model.Document
import `in`.xroden.flockr.features.documents.presentation.DocumentEvent
import `in`.xroden.flockr.features.documents.presentation.DocumentUiState
import `in`.xroden.flockr.features.documents.presentation.DocumentViewModel
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.model.nameOf
import `in`.xroden.flockr.features.house.model.timeZone
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.AnimatedGlyph
import `in`.xroden.flockr.ui.components.BadgeTone
import `in`.xroden.flockr.ui.components.GlyphMotion
import `in`.xroden.flockr.ui.components.HeroAmount
import `in`.xroden.flockr.ui.components.HeroCaption
import `in`.xroden.flockr.ui.components.HeroHeader
import `in`.xroden.flockr.ui.components.HeroLabel
import `in`.xroden.flockr.ui.components.HeroStatusBarScrim
import `in`.xroden.flockr.ui.components.IconBadge
import `in`.xroden.flockr.ui.components.ListRow
import `in`.xroden.flockr.ui.components.SectionTitle
import `in`.xroden.flockr.ui.components.SkeletonHeroScreen
import `in`.xroden.flockr.ui.components.buttons.FlockrExtendedFab
import `in`.xroden.flockr.ui.components.dialogs.ConfirmDialog
import `in`.xroden.flockr.ui.components.inputs.PillSelector
import `in`.xroden.flockr.ui.components.isHeroScrolledAway
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.IconSize
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.ui.theme.flockrColors
import `in`.xroden.flockr.utils.formatWithHouseConfig
import `in`.xroden.flockr.utils.monthYearLabel
import `in`.xroden.flockr.utils.rememberHaptics
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime

private const val HOUSE_TAB = 0
private val TABS = listOf("House", "Personal")

private const val BYTES_PER_MB = 1024 * 1024
private const val TRACK_ALPHA = 0.24f

/** What one tab shows: its files, how many it may hold, and the words that frame it. */
private class Shelf(
    val documents: List<Document>,
    val limit: Int,
    val label: String,
    val icon: ImageVector,
    val emptyHeadline: String,
    val emptyHint: String,
    val isHouse: Boolean,
)

private fun DocumentUiState.Ready.shelf(tab: Int): Shelf = if (tab == HOUSE_TAB) {
    Shelf(
        documents = house,
        limit = UploadDocumentUseCase.MAX_HOUSE_DOCUMENTS,
        label = "Files shared with the house",
        icon = Icons.Rounded.FolderShared,
        emptyHeadline = "Add the lease or the house rules",
        emptyHint = "Everyone in the house can open what you put here, so nobody has to ask for it again.",
        isHouse = true,
    )
} else {
    Shelf(
        documents = personal,
        limit = UploadDocumentUseCase.MAX_PERSONAL_DOCUMENTS,
        label = "Files only you can see",
        icon = Icons.Rounded.Folder,
        emptyHeadline = "Keep your ID or rent receipts",
        emptyHint = "Nobody else in the house can see these, not even an admin.",
        isHouse = false,
    )
}

@Composable
fun DocumentsScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: DocumentViewModel = hiltViewModel()
) {
    val haptics = rememberHaptics()
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val config by rememberHouseConfig(houseId)
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableIntStateOf(HOUSE_TAB) }
    var deleting by remember { mutableStateOf<Document?>(null) }
    val ready = state as? DocumentUiState.Ready
    val listState = rememberLazyListState()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.upload(houseId, uri, toHouse = selectedTab == HOUSE_TAB)
    }
    val pick = { if (ready?.isUploading == false) picker.launch(Validators.DOCUMENT_MIME_TYPES.toTypedArray()) }

    LaunchedEffect(houseId) { viewModel.load(houseId) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is DocumentEvent.Show -> {
                    if (event.notice.isError) haptics.error() else haptics.success()
                    event.notice.message
                }
                is DocumentEvent.Open -> if (context.openLink(event.url)) null else "No app on this phone can open that file"
                is DocumentEvent.Download -> if (context.enqueueDownload(event)) "Downloading ${event.fileName}" else "Couldn't start the download"
            }
            if (message != null) launch { snackbarHostState.showSnackbar(message) }
        }
    }

    Scaffold(
        floatingActionButton = {
            if (ready != null) {
                FlockrExtendedFab(text = if (ready.isUploading) "Uploading" else "Upload", icon = Icons.Rounded.UploadFile, onClick = pick)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            when (val current = state) {
                DocumentUiState.Loading -> SkeletonHeroScreen()
                is DocumentUiState.Error -> Box(Modifier.fillMaxSize().padding(padding)) {
                    ErrorState(current.message, onRetry = { viewModel.load(houseId) })
                }
                is DocumentUiState.Ready -> DocumentsContent(
                    state = current,
                    shelf = current.shelf(selectedTab),
                    selectedTab = selectedTab,
                    config = config,
                    listState = listState,
                    onTabSelected = { selectedTab = it },
                    onUpload = pick,
                    onOpen = viewModel::open,
                    onDownload = viewModel::download,
                    onDelete = { deleting = it },
                )
            }
            if (ready != null) HeroStatusBarScrim(isHeroGone = listState.isHeroScrolledAway)
        }
    }

    deleting?.let { document ->
        ConfirmDialog(
            title = "Delete ${document.fileName}?",
            message = if (document.houseId != null) "It's removed for everyone in the house." else "It's removed from your documents.",
            confirmText = "Delete",
            onConfirm = {
                deleting = null
                viewModel.delete(document)
            },
            onDismiss = { deleting = null },
            isDestructive = true,
        )
    }
}

@Composable
private fun DocumentsContent(
    state: DocumentUiState.Ready,
    shelf: Shelf,
    selectedTab: Int,
    config: HouseConfig?,
    listState: LazyListState,
    onTabSelected: (Int) -> Unit,
    onUpload: () -> Unit,
    onOpen: (Document) -> Unit,
    onDownload: (Document) -> Unit,
    onDelete: (Document) -> Unit,
) {
    val timeZone = config.timeZone()
    val byMonth = shelf.documents.groupBy { it.createdAt.toLocalDateTime(timeZone).date.let { date -> date.year to date.month } }
    LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = Spacing.xxxxl * 2)) {
        item(key = "hero") { ShelfHero(shelf, isUploading = state.isUploading) }
        item(key = "tabs") {
            PillSelector(
                tabs = TABS,
                selectedIndex = selectedTab,
                onTabSelected = onTabSelected,
                counts = listOf(state.house.size, state.personal.size),
                modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.lg),
            )
        }
        if (shelf.documents.isEmpty()) {
            item(key = "empty_title") { SectionTitle("Nothing here yet") }
            item(key = "empty") {
                ListRow(
                    headline = shelf.emptyHeadline,
                    supporting = shelf.emptyHint,
                    leading = { IconBadge(Icons.Rounded.UploadFile, BadgeTone.SUN) },
                    onClick = onUpload,
                )
            }
        }
        byMonth.forEach { (month, documents) ->
            item(key = "month_${month.first}_${month.second}") {
                SectionTitle(documents.first().createdAt.toLocalDateTime(timeZone).date.monthYearLabel())
            }
            items(documents, key = { it.id }) { document ->
                DocumentRow(
                    document = document,
                    uploader = if (shelf.isHouse) state.members.nameOf(document.userId, state.viewerId) else null,
                    config = config,
                    canDelete = state.canDelete(document),
                    onOpen = { onOpen(document) },
                    onDownload = { onDownload(document) },
                    onDelete = { onDelete(document) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
        item(key = "inset") { Spacer(Modifier.navigationBarsPadding()) }
    }
}

/** The cobalt headline: how full this tab is and how much it holds, with the upload's progress while one runs. */
@Composable
private fun ShelfHero(shelf: Shelf, isUploading: Boolean) {
    val context = LocalContext.current
    val count = shelf.documents.size
    val used = Formatter.formatShortFileSize(context, shelf.documents.sumOf { it.fileSize ?: 0L })
    HeroHeader(title = "Documents") {
        HeroLabel(shelf.label)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            AnimatedGlyph(shelf.icon, trigger = count, motion = GlyphMotion.POP, contentDescription = null, modifier = Modifier.size(IconSize.lg))
            HeroAmount("$count of ${shelf.limit}")
        }
        HeroCaption(
            when {
                isUploading -> "Uploading your file."
                count >= shelf.limit -> "Full at $used. Delete one to make room."
                else -> "$used used. Files up to ${StorageRepository.MAX_FILE_SIZE_BYTES / BYTES_PER_MB} MB, photos up to ${StorageRepository.MAX_IMAGE_SIZE_BYTES / BYTES_PER_MB} MB."
            },
        )
        if (isUploading) {
            val colors = MaterialTheme.flockrColors
            LinearWavyProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.md),
                color = colors.onHero,
                trackColor = colors.onHero.copy(alpha = TRACK_ALPHA),
            )
        }
    }
}

@Composable
private fun DocumentRow(
    document: Document,
    uploader: String?,
    config: HouseConfig?,
    canDelete: Boolean,
    onOpen: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    val details = listOfNotNull(
        uploader,
        document.createdAt.toLocalDateTime(config.timeZone()).date.formatWithHouseConfig(config),
        document.fileSize?.let { Formatter.formatShortFileSize(context, it) },
    ).joinToString(" · ")

    ListRow(
        headline = document.fileName,
        supporting = details,
        leading = { IconBadge(typeIcon(document.mimeType), typeTone(document.mimeType)) },
        trailing = {
            Box {
                IconButton(onClick = { menuOpen = true }, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "More for ${document.fileName}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Download") },
                        leadingIcon = { Icon(Icons.Rounded.Download, contentDescription = null) },
                        onClick = {
                            haptics.tap()
                            menuOpen = false
                            onDownload()
                        },
                    )
                    if (canDelete) {
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            },
                        )
                    }
                }
            }
        },
        onClick = onOpen,
        modifier = modifier,
    )
}

private fun typeIcon(mimeType: String?): ImageVector = when {
    mimeType == null -> Icons.Rounded.Description
    mimeType == "application/pdf" -> Icons.Rounded.PictureAsPdf
    mimeType.startsWith("image/") -> Icons.Rounded.Image
    "sheet" in mimeType || "excel" in mimeType -> Icons.Rounded.TableChart
    else -> Icons.Rounded.Description
}

private fun typeTone(mimeType: String?): BadgeTone = when {
    mimeType == null -> BadgeTone.SLATE
    mimeType == "application/pdf" -> BadgeTone.ROSE
    mimeType.startsWith("image/") -> BadgeTone.SUN
    "sheet" in mimeType || "excel" in mimeType -> BadgeTone.JADE
    else -> BadgeTone.COBALT
}

/** Opens a signed link in whichever app handles it, false when none can. */
private fun Context.openLink(url: String): Boolean =
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess

/** Hands the file to the system downloader, which saves it to Downloads with no storage permission. */
private fun Context.enqueueDownload(download: DocumentEvent.Download): Boolean = runCatching {
    val fileName = download.fileName.substringAfterLast('/').ifBlank { "document" }
    val request = DownloadManager.Request(Uri.parse(download.url))
        .setTitle(fileName)
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
    download.mimeType?.let { request.setMimeType(it) }
    getSystemService(DownloadManager::class.java).enqueue(request)
}.isSuccess
