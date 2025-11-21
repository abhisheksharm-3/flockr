package `in`.xroden.flockr.features.chores.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import `in`.xroden.flockr.features.chores.model.Chore
import `in`.xroden.flockr.features.chores.domain.ChoreUiState
import `in`.xroden.flockr.features.chores.domain.CreateChoreUiState
import `in`.xroden.flockr.features.chores.domain.ChoreViewModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
fun ChoresScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: ChoreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val createState by viewModel.createState.collectAsState()
    val filterOption by viewModel.filterOption.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Chore?>(null) }
    var showProductivityDialog by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(houseId) {
        viewModel.loadChores(houseId)
    }

    // Handle create state changes
    LaunchedEffect(createState) {
        when (val state = createState) {
            is CreateChoreUiState.Success -> {
                showAddDialog = false
                snackbarHostState.showSnackbar("Chore added successfully")
                viewModel.resetCreateState()
            }
            is CreateChoreUiState.Error -> {
                snackbarHostState.showSnackbar("Failed to add chore: ${state.message}")
                viewModel.resetCreateState()
            }
            else -> {}
        }
    }

    // Add Chore Dialog
    if (showAddDialog) {
        AddChoreDialog(
            houseId = houseId,
            onDismiss = {
                showAddDialog = false
                viewModel.resetCreateState()
            },
            onAdd = { taskName, description, dueDate, assignedTo ->
                viewModel.createChore(
                    houseId = houseId,
                    taskName = taskName,
                    description = description,
                    dueDate = dueDate?.let {
                        try { LocalDate.parse(it) } catch (_: Exception) { null }
                    },
                    recurrencePattern = null,
                    assignedTo = assignedTo
                )
            }
        )
    }

    // Edit Chore Dialog
    showEditDialog?.let { chore ->
        EditChoreDialog(
            chore = chore,
            houseId = houseId,
            onDismiss = { showEditDialog = null },
            onSave = { taskName, description, dueDate, assignedTo ->
                viewModel.updateChore(
                    choreId = chore.id,
                    taskName = taskName,
                    description = description,
                    dueDate = dueDate?.let {
                        try { LocalDate.parse(it) } catch (_: Exception) { null }
                    },
                    assignedTo = assignedTo
                )
                showEditDialog = null
                scope.launch {
                    snackbarHostState.showSnackbar("Chore updated successfully")
                }
            }
        )
    }

    if (showProductivityDialog) {
        val allChores = (uiState as? ChoreUiState.Success)?.allChores ?: emptyList()
        ProductivityDialog(
            chores = allChores,
            onDismiss = { showProductivityDialog = false }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
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
                ),
                actions = {
                    IconButton(onClick = { showProductivityDialog = true }) {
                        Icon(
                            Icons.Default.Assessment,
                            "Productivity",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, "Add") },
                text = { Text("Add Chore") },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.large
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
                    ChoreFilter.ALL -> state.allChores
                    ChoreFilter.ACTIVE -> state.activeChores
                    ChoreFilter.COMPLETED -> state.completedChores
                }

                if (state.allChores.isEmpty()) {
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
                                    ChoreFilter.entries.forEach { filter ->
                                        FilterChip(
                                            selected = filterOption == filter,
                                            onClick = { viewModel.setFilter(filter) },
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

                                if (filterOption == ChoreFilter.COMPLETED && filteredChores.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = { viewModel.clearCompletedChores(houseId) },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Clear Completed")
                                    }
                                }
                            }
                        }

                        // Chore Items
                        items(filteredChores, key = { it.id }) { chore ->
                            ChoreCard(
                                chore = chore,
                                onToggleComplete = {
                                    scope.launch {
                                        if (!chore.isCompleted) {
                                            viewModel.completeChore(chore.id, houseId, chore.taskName)
                                            snackbarHostState.showSnackbar("Chore completed!")
                                        }
                                    }
                                },
                                onEdit = { showEditDialog = chore },
                                onDelete = {
                                    scope.launch {
                                        viewModel.deleteChore(chore.id)
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
                            shape = MaterialTheme.shapes.medium
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
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (chore.isCompleted) {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: Checkbox + Title + Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Custom Checkbox with larger hit area
                Surface(
                    onClick = onToggleComplete,
                    shape = MaterialTheme.shapes.extraSmall,
                    color = if (chore.isCompleted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.size(28.dp),
                    border = BorderStroke(
                        2.dp,
                        if (chore.isCompleted) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        }
                    )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (chore.isCompleted) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Title
                Text(
                    text = chore.taskName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (chore.isCompleted) TextDecoration.LineThrough else null,
                    color = if (chore.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f)
                )

                // More Options Menu
                Box {
                    IconButton(
                        onClick = { showMenu = !showMenu },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (!chore.isCompleted) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, null)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }

            // Description
            chore.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Metadata Row (Due Date + User Info)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Due Date Badge
                chore.dueDate?.let { date ->
                    val isOverdue = isOverdue(date.toString()) && !chore.isCompleted
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = when {
                            isOverdue -> MaterialTheme.colorScheme.errorContainer
                            chore.isCompleted -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = when {
                                    isOverdue -> MaterialTheme.colorScheme.onErrorContainer
                                    chore.isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                            Text(
                                text = formatDate(date.toString()),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = when {
                                    isOverdue -> MaterialTheme.colorScheme.onErrorContainer
                                    chore.isCompleted -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                        }
                    }
                }

                // Created By Badge
                chore.createdByName?.let { name ->
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                // Assigned To Badge
                chore.assignedToName?.let { name ->
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PersonOutline,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // Completed Badge
                if (chore.isCompleted && chore.completedByName != null) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = chore.completedByName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
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
            shape = MaterialTheme.shapes.large,
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
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Add any details...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due Date (Optional)") },
                    placeholder = { Text("YYYY-MM-DD") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
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
                        shape = MaterialTheme.shapes.medium
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
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(
                    2.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    MaterialTheme.shapes.large
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
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add First Chore")
        }
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
fun EditChoreDialog(
    chore: Chore,
    houseId: String,
    onDismiss: () -> Unit,
    onSave: (String, String?, String?, String?) -> Unit
) {
    var taskName by remember { mutableStateOf(chore.taskName) }
    var description by remember { mutableStateOf(chore.description ?: "") }
    var dueDate by remember { mutableStateOf(chore.dueDate?.toString() ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
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
                    text = "Edit Chore",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    label = { Text("Task Name *") },
                    placeholder = { Text("e.g., Clean kitchen") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    placeholder = { Text("Add details...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due Date (Optional)") },
                    placeholder = { Text("YYYY-MM-DD") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onSave(
                                taskName,
                                description.takeIf { it.isNotBlank() },
                                dueDate.takeIf { it.isNotBlank() },
                                chore.assignedTo  // Keep existing assignment
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = taskName.isNotBlank(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Save")
                    }
                }
            }
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
        // Handle short dates safely
        if (dateString.length < 10) return dateString
        val date = LocalDate.parse(dateString.take(10))
        // Format as "MMM dd, yyyy" manually
        val monthName = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")[date.monthNumber]
        "$monthName ${date.dayOfMonth}, ${date.year}"
    } catch (_: Exception) {
        // Return safe default if parsing fails
        dateString.take(10)
    }
}

private fun isOverdue(dateString: String): Boolean {
    return try {
        val dueDate = LocalDate.parse(dateString.take(10))
        val now = Clock.System.now()
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        dueDate < today
    } catch (_: Exception) {
        false
    }
}
