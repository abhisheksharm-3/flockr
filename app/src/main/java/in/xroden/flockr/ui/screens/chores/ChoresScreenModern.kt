package `in`.xroden.flockr.ui.screens.chores

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import `in`.xroden.flockr.data.model.Chore
import `in`.xroden.flockr.ui.theme.PositiveGreen
import `in`.xroden.flockr.ui.viewmodel.ChoreUiState
import `in`.xroden.flockr.ui.viewmodel.ChoreViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoresScreenModern(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: ChoreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var filterOption by remember { mutableStateOf(ChoreFilter.ALL) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(houseId) {
        viewModel.loadChores(houseId)
    }

    // Add Chore Dialog
    if (showAddDialog) {
        AddChoreDialog(
            houseId = houseId,
            onDismiss = { showAddDialog = false },
            onAdd = { taskName, description, dueDate, assignedTo ->
                scope.launch {
                    // TODO: Add chore via viewmodel
                    showAddDialog = false
                    snackbarHostState.showSnackbar("Chore added")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Chores",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, "Add") },
                text = { Text("Add Chore") },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val state = uiState) {
            is ChoreUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is ChoreUiState.Success -> {
                val filteredChores = when (filterOption) {
                    ChoreFilter.ALL -> state.chores
                    ChoreFilter.ACTIVE -> state.chores.filter { !it.isCompleted }
                    ChoreFilter.COMPLETED -> state.chores.filter { it.isCompleted }
                }

                if (state.chores.isEmpty()) {
                    EmptyChoresState(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        onAddChore = { showAddDialog = true }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header & Filters
                        item {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Household Tasks",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                // Filter Chips
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ChoreFilter.values().forEach { filter ->
                                        FilterChip(
                                            selected = filterOption == filter,
                                            onClick = { filterOption = filter },
                                            label = { Text(filter.label) },
                                            leadingIcon = if (filterOption == filter) {
                                                { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                                            } else null
                                        )
                                    }
                                }

                                Text(
                                    text = "${filteredChores.size} ${filterOption.label.lowercase()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Chore Items
                        items(filteredChores, key = { it.id }) { chore ->
                            ChoreCard(
                                chore = chore,
                                onToggleComplete = {
                                    scope.launch {
                                        // TODO: Toggle complete via viewmodel
                                        viewModel.loadChores(houseId)
                                        val message = if (chore.isCompleted) {
                                            "Chore marked as incomplete"
                                        } else {
                                            "Chore completed!"
                                        }
                                        snackbarHostState.showSnackbar(message)
                                    }
                                },
                                onDelete = {
                                    scope.launch {
                                        // TODO: Delete via viewmodel
                                        snackbarHostState.showSnackbar("Chore deleted")
                                    }
                                }
                            )
                        }

                        // Bottom Spacer
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
            is ChoreUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Error loading chores",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = { viewModel.loadChores(houseId) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChoreCard(
    chore: Chore,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (chore.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (chore.isCompleted) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Checkbox
            Checkbox(
                checked = chore.isCompleted,
                onCheckedChange = { onToggleComplete() }
            )

            // Chore Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = chore.taskName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (chore.isCompleted) TextDecoration.LineThrough else null,
                    color = if (chore.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                chore.description?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Due Date Badge
                chore.dueDate?.let { date ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isOverdue(date) && !chore.isCompleted) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isOverdue(date) && !chore.isCompleted) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                }
                            )
                            Text(
                                text = formatDate(date),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = if (isOverdue(date) && !chore.isCompleted) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                }
                            )
                        }
                    }
                }
            }

            // Delete Button
            IconButton(
                onClick = onDelete,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Delete, "Delete")
            }
        }
    }
}

@Composable
fun AddChoreDialog(
    houseId: String,
    onDismiss: () -> Unit,
    onAdd: (String, String?, String?, String?) -> Unit
) {
    var taskName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add Chore",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    label = { Text("Task Name *") },
                    placeholder = { Text("e.g., Take out trash") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Add any details...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due Date (Optional)") },
                    placeholder = { Text("YYYY-MM-DD") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onAdd(
                                taskName,
                                description.takeIf { it.isNotBlank() },
                                dueDate.takeIf { it.isNotBlank() },
                                null // assignedTo
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = taskName.isNotBlank(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyChoresState(
    modifier: Modifier = Modifier,
    onAddChore: () -> Unit
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(
                    2.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No Chores Yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Add household tasks to keep everyone organized",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAddChore,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add First Chore")
        }
    }
}

enum class ChoreFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    COMPLETED("Completed")
}

private fun formatDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString.substring(0, 10))
        date.format(DateTimeFormatter.ofPattern("MMM d"))
    } catch (e: Exception) {
        dateString.substring(0, 10)
    }
}

private fun isOverdue(dateString: String): Boolean {
    return try {
        val dueDate = LocalDate.parse(dateString.substring(0, 10))
        dueDate.isBefore(LocalDate.now())
    } catch (e: Exception) {
        false
    }
}

