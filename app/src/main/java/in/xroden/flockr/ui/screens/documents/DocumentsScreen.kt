package `in`.xroden.flockr.ui.screens.documents

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.data.model.Document
import `in`.xroden.flockr.ui.components.cards.FlockrCard
import `in`.xroden.flockr.ui.viewmodel.DocumentViewModel
import `in`.xroden.flockr.ui.viewmodel.DocumentUiState

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

    val personalDocsState by viewModel.personalDocuments.collectAsState()
    val houseDocsState by viewModel.houseDocuments.collectAsState()

    LaunchedEffect(houseId) {
        viewModel.loadPersonalDocuments()
        viewModel.loadHouseDocuments(houseId)
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
            TopAppBar(
                title = { Text("Documents") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "*/*"
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }
                    filePickerLauncher.launch(intent)
                }
            ) {
                Icon(Icons.Default.Add, "Upload Document")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> DocumentList(
                    state = personalDocsState,
                    onDownload = { doc -> viewModel.downloadDocument(doc, context) },
                    onDelete = { doc -> viewModel.deleteDocument(doc.id, doc.storagePath) }
                )
                1 -> DocumentList(
                    state = houseDocsState,
                    onDownload = { doc -> viewModel.downloadDocument(doc, context) },
                    onDelete = { doc -> viewModel.deleteDocument(doc.id, doc.storagePath) }
                )
            }
        }
    }
}

@Composable
private fun DocumentList(
    state: DocumentUiState,
    onDownload: (Document) -> Unit,
    onDelete: (Document) -> Unit
) {
    when (state) {
        is DocumentUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is DocumentUiState.Success -> {
            if (state.documents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No documents yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.documents) { document ->
                        DocumentCard(
                            document = document,
                            onDownload = { onDownload(document) },
                            onDelete = { onDelete(document) }
                        )
                    }
                }
            }
        }
        is DocumentUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun DocumentCard(
    document: Document,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    FlockrCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = document.createdAt.substring(0, 10),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = onDownload) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Download",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun getFileNameFromUri(context: android.content.Context, uri: Uri): String {
    var fileName = "unknown"
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val displayNameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (displayNameIndex != -1) {
                fileName = it.getString(displayNameIndex)
            }
        }
    }
    return fileName
}
