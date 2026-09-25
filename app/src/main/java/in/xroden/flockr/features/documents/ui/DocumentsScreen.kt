/** The house's shared documents and the viewer's personal ones, in two tabs, with upload, open, download and delete. */
package `in`.xroden.flockr.features.documents.ui

import `in`.xroden.flockr.core.validation.Validators
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.core.storage.StorageRepository
import `in`.xroden.flockr.features.documents.domain.usecase.UploadDocumentUseCase
import `in`.xroden.flockr.features.documents.model.Document
import `in`.xroden.flockr.features.documents.presentation.DocumentEvent
import `in`.xroden.flockr.features.documents.presentation.DocumentUiState
import `in`.xroden.flockr.features.documents.presentation.DocumentViewModel
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.model.nameOf
import `in`.xroden.flockr.features.house.model.timeZone
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.buttons.FlockrExtendedFab
import `in`.xroden.flockr.ui.components.dialogs.ConfirmDialog
import `in`.xroden.flockr.ui.components.states.EmptyState
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.ComponentHeight
import `in`.xroden.flockr.ui.theme.IconSize
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.formatWithHouseConfig
import `in`.xroden.flockr.utils.rememberHaptics
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime

private const val HOUSE_TAB = 0
private const val PERSONAL_TAB = 1
private val TABS = listOf("House", "Personal")

/** The picker offers only what the repository accepts; keep in step with Validators' document types. */

private const val BYTES_PER_MB = 1024 * 1024

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
        topBar = { FlockrTopAppBar(title = "Documents", onNavigateBack = onNavigateBack) },
        floatingActionButton = {
            if (ready != null) {
                FlockrExtendedFab(text = if (ready.isUploading) "Uploading" else "Upload", icon = Icons.Rounded.UploadFile, onClick = pick)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                TABS.forEachIndexed { index, title ->
                    Tab(selected = selectedTab == index, onClick = { haptics.select(); selectedTab = index }, text = { Text(title) })
                }
            }
            if (ready?.isUploading == true) LinearWavyProgressIndicator(Modifier.fillMaxWidth().padding(vertical = Spacing.xs))
            Box(Modifier.fillMaxSize()) {
                when (val current = state) {
                    DocumentUiState.Loading -> LoadingIndicator(Modifier.align(Alignment.Center))
                    is DocumentUiState.Error -> ErrorState(current.message, onRetry = { viewModel.load(houseId) })
                    is DocumentUiState.Ready -> if (selectedTab == HOUSE_TAB) {
                        DocumentList(
                            documents = current.house,
                            state = current,
                            config = config,
                            limitLine = "${current.house.size} of ${UploadDocumentUseCase.MAX_HOUSE_DOCUMENTS} house documents",
                            showUploader = true,
                            empty = {
                                EmptyState(
                                    icon = Icons.Rounded.FolderShared,
                                    title = "No house documents yet",
                                    subtitle = "Keep the lease, bills and house rules where everyone can find them. The house can keep ${UploadDocumentUseCase.MAX_HOUSE_DOCUMENTS}.",
                                    actionText = "Upload a document",
                                    onActionClick = pick,
                                )
                            },
                            onOpen = viewModel::open,
                            onDownload = viewModel::download,
                            onDelete = { deleting = it },
                        )
                    } else {
                        DocumentList(
                            documents = current.personal,
                            state = current,
                            config = config,
                            limitLine = "${current.personal.size} of ${UploadDocumentUseCase.MAX_PERSONAL_DOCUMENTS} personal documents",
                            showUploader = false,
                            empty = {
                                EmptyState(
                                    icon = Icons.Rounded.Folder,
                                    title = "No personal documents yet",
                                    subtitle = "Only you can see these. You can keep ${UploadDocumentUseCase.MAX_PERSONAL_DOCUMENTS}.",
                                    actionText = "Upload a document",
                                    onActionClick = pick,
                                )
                            },
                            onOpen = viewModel::open,
                            onDownload = viewModel::download,
                            onDelete = { deleting = it },
                        )
                    }
                }
            }
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
private fun DocumentList(
    documents: List<Document>,
    state: DocumentUiState.Ready,
    config: HouseConfig?,
    limitLine: String,
    showUploader: Boolean,
    empty: @Composable () -> Unit,
    onOpen: (Document) -> Unit,
    onDownload: (Document) -> Unit,
    onDelete: (Document) -> Unit,
) {
    if (documents.isEmpty()) {
        empty()
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = Spacing.xxxxl * 2)) {
        item(key = "limits") {
            Text(
                "$limitLine · files up to ${StorageRepository.MAX_FILE_SIZE_BYTES / BYTES_PER_MB} MB, images up to ${StorageRepository.MAX_IMAGE_SIZE_BYTES / BYTES_PER_MB} MB",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            )
        }
        items(documents, key = { it.id }) { document ->
            DocumentRow(
                document = document,
                uploader = if (showUploader) state.members.nameOf(document.userId, state.viewerId) else null,
                config = config,
                canDelete = state.canDelete(document),
                onOpen = { onOpen(document) },
                onDownload = { onDownload(document) },
                onDelete = { onDelete(document) },
                modifier = Modifier.animateItem(),
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

    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onOpen).padding(start = Spacing.lg, end = Spacing.xs, top = Spacing.md, bottom = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer) {
            Box(Modifier.size(ComponentHeight.avatar), contentAlignment = Alignment.Center) {
                Icon(typeIcon(document.mimeType), contentDescription = null, modifier = Modifier.size(IconSize.md))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(document.fileName, style = MaterialTheme.typography.bodyLargeEmphasized, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box {
            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "More for ${document.fileName}") }
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
    }
}

private fun typeIcon(mimeType: String?): ImageVector = when {
    mimeType == null -> Icons.Rounded.Description
    mimeType == "application/pdf" -> Icons.Rounded.PictureAsPdf
    mimeType.startsWith("image/") -> Icons.Rounded.Image
    "sheet" in mimeType || "excel" in mimeType -> Icons.Rounded.TableChart
    else -> Icons.Rounded.Description
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
