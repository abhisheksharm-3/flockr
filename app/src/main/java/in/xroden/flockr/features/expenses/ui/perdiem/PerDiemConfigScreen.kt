package `in`.xroden.flockr.features.expenses.ui.perdiem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import `in`.xroden.flockr.features.expenses.model.PerDiemConfig
import `in`.xroden.flockr.features.expenses.presentation.PerDiemViewModel
import `in`.xroden.flockr.features.expenses.presentation.PerDiemConfigUiState
import `in`.xroden.flockr.utils.getCurrencySymbol
import `in`.xroden.flockr.ui.components.loading.ListScreenSkeleton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.utils.rememberHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerDiemConfigScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAddEntry: (String) -> Unit,
    onNavigateToAddConfig: () -> Unit,
    onNavigateToEditConfig: (PerDiemConfig) -> Unit,
    viewModel: PerDiemViewModel = hiltViewModel()
) {
    val haptics = rememberHaptics()
    val configsState by viewModel.configState.collectAsStateWithLifecycle()
    val houseConfig by viewModel.houseConfig.collectAsStateWithLifecycle()
    val currencySymbol = getCurrencySymbol(houseConfig?.currencyCode ?: "USD")

    var showDeleteDialog by remember { mutableStateOf<PerDiemConfig?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(houseId) {
        viewModel.loadConfigs(houseId)
        viewModel.loadHouseConfig(houseId)
    }

    showDeleteDialog?.let { config ->
        DeletePerDiemConfigDialog(
            config = config,
            onDismiss = { showDeleteDialog = null },
            onConfirm = { deleteUsage ->
                viewModel.deleteConfig(houseId, config.id, deleteUsage)
                showDeleteDialog = null
                scope.launch {
                    snackbarHostState.showSnackbar(
                        if (deleteUsage) "Item and usage deleted" else "Item deleted"
                    )
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Per-Diem Items",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { haptics.tap(); onNavigateToAddConfig() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val state = configsState) {
            is PerDiemConfigUiState.Loading -> {
                ListScreenSkeleton(modifier = Modifier.padding(padding))
            }
            is PerDiemConfigUiState.Error -> {
                ErrorState(
                    message = state.message,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
            is PerDiemConfigUiState.Success -> {
                if (state.configs.isEmpty()) {
                    EmptyPerDiemState(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        onAddItem = onNavigateToAddConfig
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Info Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Track daily shared items and calculate usage costs automatically",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        items(state.configs, key = { it.id }) { config ->
                            PerDiemConfigCard(
                                config = config,
                                currencySymbol = currencySymbol,
                                onAddEntry = { onNavigateToAddEntry(config.id) },
                                onEdit = { onNavigateToEditConfig(config) },
                                onDelete = { showDeleteDialog = config }
                            )
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PerDiemConfigCard(
    config: PerDiemConfig,
    currencySymbol: String,
    onAddEntry: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val haptics = rememberHaptics()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.CalendarMonth,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Column {
                        Text(
                            text = config.itemName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = config.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Rate Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "$currencySymbol${String.format(Locale.getDefault(), "%.2f", config.rate)}/${config.unit}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { haptics.tap(); onAddEntry() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log Usage", fontWeight = FontWeight.SemiBold)
                }

                FilledTonalIconButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(20.dp))
                }

                FilledTonalIconButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun EmptyPerDiemState(
    modifier: Modifier = Modifier,
    onAddItem: () -> Unit
) {
    val haptics = rememberHaptics()
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                "No Per-Diem Items",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Set up daily shared items like milk or coffee to track usage costs automatically",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = { haptics.tap(); onAddItem() },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add First Item", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Error,
                null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(message, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun DeletePerDiemConfigDialog(
    config: PerDiemConfig,
    onDismiss: () -> Unit,
    onConfirm: (deleteUsage: Boolean) -> Unit
) {
    val haptics = rememberHaptics()
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Outlined.Delete,
                null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                "Delete \"${config.itemName}\"?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                "Choose whether to keep the logged usage or delete everything.",
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            TextButton(onClick = { haptics.error(); onConfirm(false) }) {
                Text("Keep Usage", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = { haptics.error(); onConfirm(true) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete All", fontWeight = FontWeight.SemiBold)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
