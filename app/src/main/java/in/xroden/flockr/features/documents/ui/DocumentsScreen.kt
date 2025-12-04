package `in`.xroden.flockr.features.documents.ui

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.features.documents.model.Document
import `in`.xroden.flockr.features.documents.domain.DocumentViewModel
import `in`.xroden.flockr.features.documents.domain.DocumentUiState
import `in`.xroden.flockr.features.documents.domain.UploadDocumentUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: DocumentViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Personal", "House")
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(houseId) {
        viewModel.loadDocuments(houseId)
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val fileName = getFileNameFromUri(context, uri)
                if (selectedTab == 0) {
                    viewModel.uploadPersonalDocument(uri, fileName, context)
                } else {
                    viewModel.uploadHouseDocument(houseId, uri, fileName, context)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Documents",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "*/*"
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }
                    filePickerLauncher.launch(intent)
                },
                icon = { Icon(Icons.Default.Upload, "Upload") },
                text = { Text("Upload", fontWeight = FontWeight.SemiBold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.medium
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val uploadState by viewModel.uploadState.collectAsState()

            // Show upload progress/error
            when (val state = uploadState) {
                is UploadDocumentUiState.Uploading -> {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                is UploadDocumentUiState.Error -> {
                    LaunchedEffect(state) {
                        snackbarHostState.showSnackbar(state.message)
                        viewModel.resetUploadState()
                    }
                }
                is UploadDocumentUiState.Success -> {
                    LaunchedEffect(state) {
                        snackbarHostState.showSnackbar("Document uploaded successfully")
                        viewModel.resetUploadState()
                    }
                }
                else -> {}
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // Custom Tab Row
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = {
                        TabRowDefaults.PrimaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(selectedTab),
                            width = Dp.Unspecified,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            },
                            selectedContentColor = MaterialTheme.colorScheme.primary,
                            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                when (val currentState = uiState) {
                    is DocumentUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is DocumentUiState.Success -> {
                        val docs = if (selectedTab == 0) currentState.personalDocuments else currentState.houseDocuments

                        // Usage Stats
                        val limit = if (selectedTab == 0) 2 else 3
                        val count = docs.size

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Usage Banner
                            item {
                                Card(
                                    shape = MaterialTheme.shapes.large,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(20.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "Storage Usage",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Text(
                                                text = "$count / $limit documents",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                        Box(contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(
                                                progress = { count.toFloat() / limit.toFloat() },
                                                modifier = Modifier.size(48.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                                strokeWidth = 4.dp
                                            )
                                            Text(
                                                text = "${(count.toFloat() / limit.toFloat() * 100).toInt()}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            if (docs.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.size(80.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Default.Description,
                                                        null,
                                                        modifier = Modifier.size(32.dp),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Text(
                                                "No documents yet",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "Upload important files to keep them handy",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(docs) { document ->
                                    DocumentCard(
                                        document = document,
                                        onDownload = { viewModel.downloadDocument(document) },
                                        onDelete = { viewModel.deleteDocument(document.id, document.storagePath, document.houseId) }
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(80.dp)) }
                            }
                        }
                    }
                    is DocumentUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Error: ${currentState.message}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentCard(
    document: Document,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Description,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Size: ${formatFileSize(document.fileSize ?: 0L)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Actions
            Row {
                IconButton(onClick = onDownload) {
                    Icon(
                        Icons.Default.Download,
                        "Download",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

fun formatFileSize(size: Long): String {
    val kb = size / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1 -> String.format("%.1f MB", mb)
        kb >= 1 -> String.format("%.1f KB", kb)
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
