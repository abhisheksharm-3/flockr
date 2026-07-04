package `in`.xroden.flockr.features.documents.ui

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.features.documents.model.Document
import `in`.xroden.flockr.features.documents.presentation.DocumentViewModel
import `in`.xroden.flockr.features.documents.presentation.DocumentUiState
import `in`.xroden.flockr.features.documents.presentation.UploadDocumentUiState
import `in`.xroden.flockr.utils.rememberHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: DocumentViewModel = hiltViewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Personal, 1 = House
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = rememberHapticFeedback()

    LaunchedEffect(houseId) { viewModel.loadDocuments(houseId) }

    LaunchedEffect(Unit) {
        viewModel.downloadEvent.collect { request ->
            val safeName = request.fileName.substringAfterLast('/').ifBlank { "document" }
            val downloadManager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
            val dmRequest = android.app.DownloadManager.Request(android.net.Uri.parse(request.url))
                .setTitle(safeName)
                .setDescription("Downloading document")
                .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, safeName)
            request.mimeType?.let { dmRequest.setMimeType(it) }
            runCatching { downloadManager.enqueue(dmRequest) }
                .onSuccess { snackbarHostState.showSnackbar("Downloading $safeName") }
                .onFailure { snackbarHostState.showSnackbar("Could not start download") }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messageEvent.collect { snackbarHostState.showSnackbar(it) }
    }

     val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = getFileNameFromUri(context, uri)
            if (selectedTab == 0) {
                viewModel.uploadPersonalDocument(uri, fileName, context)
            } else {
                viewModel.uploadHouseDocument(houseId, uri, fileName, context)
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Documents", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { haptics.performClick(); onNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { haptics.performClick(); filePickerLauncher.launch("*/*") },
                text = { Text("Upload File", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.CloudUpload, null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Upload Progress
            val uploadState by viewModel.uploadState.collectAsStateWithLifecycle()
            
             when (val state = uploadState) {
                is UploadDocumentUiState.Error -> {
                    LaunchedEffect(state) {
                        snackbarHostState.showSnackbar(state.message)
                        viewModel.resetUploadState()
                    }
                }
                is UploadDocumentUiState.Success -> {
                    LaunchedEffect(state) {
                        snackbarHostState.showSnackbar("Uploaded successfully")
                        viewModel.resetUploadState()
                    }
                }
                else -> {}
            }
            
            if (uploadState is UploadDocumentUiState.Uploading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            
            // Custom Segmented Control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TabButton("Personal", selectedTab == 0) { haptics.performSelection(); selectedTab = 0 }
                TabButton("House", selectedTab == 1) { haptics.performSelection(); selectedTab = 1 }
            }
            
            // Content
            val docs = if (uiState is DocumentUiState.Success) {
                 if (selectedTab == 0) (uiState as DocumentUiState.Success).personalDocuments
                 else (uiState as DocumentUiState.Success).houseDocuments
            } else emptyList()

            val total = docs.size
            val limit = if (selectedTab == 0) 2 else 3
            
            // Storage Bar (Pill style)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            ) {
                 Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                 ) {
                     Column {
                        Text(
                            "Storage Usage",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "$total / $limit files used",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                     }
                     
                    CircularProgressIndicator(
                        progress = { total.toFloat() / limit.toFloat() },
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                 }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp) // Tighter list
            ) {
                if (docs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                             Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                 Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                                 Spacer(modifier = Modifier.height(8.dp))
                                 Text("No files found", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
                             }
                        }
                    }
                }
                
                items(items = docs, key = { it.id }) { doc ->
                    FileListItem(doc, 
                        onDownload = { viewModel.downloadDocument(doc) }, 
                        onDelete = { viewModel.deleteDocument(doc.id, doc.storagePath, doc.houseId) }
                    )
                }
                
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun RowScope.TabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .padding(4.dp)
            .clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FileListItem(doc: Document, onDownload: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    
    ListItem(
        headlineContent = { 
            Text(doc.fileName, fontWeight = FontWeight.SemiBold, maxLines = 1) 
        },
        supportingContent = { 
            Text(formatFileSize(doc.fileSize ?: 0)) 
        },
        leadingContent = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val icon = if (doc.mimeType?.contains("image") == true) Icons.Default.Image else Icons.Default.Description
                    Icon(icon, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "More")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Download") },
                        onClick = { showMenu = false; onDownload() },
                        leadingIcon = { Icon(Icons.Default.Download, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Default.Delete, null) }
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.clip(RoundedCornerShape(16.dp))
    )
}

fun formatFileSize(size: Long): String {
    val kb = size / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1 -> "%.1f MB".format(mb)
        kb >= 1 -> "%.1f KB".format(kb)
        else -> "$size B"
    }
}

private fun getFileNameFromUri(context: Context, uri: Uri): String {
    var fileName = "unknown"
    if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }
    }
    return fileName
}
