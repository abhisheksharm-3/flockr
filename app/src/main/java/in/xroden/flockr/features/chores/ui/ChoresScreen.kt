package `in`.xroden.flockr.features.chores.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import `in`.xroden.flockr.features.chores.model.Chore
import `in`.xroden.flockr.features.chores.domain.ChoreUiState
import `in`.xroden.flockr.features.chores.domain.ChoreViewModel
import kotlinx.datetime.*
import kotlin.time.Clock

// Helper functions using pure kotlinx-datetime
private fun isOverdue(dateString: String): Boolean {
    return try {
        val date = LocalDate.parse(dateString)
        val today = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        date < today
    } catch (_: Exception) {
        false
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString)
        // Manual formatting: "Jan 15"
        val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        "$month ${date.day}"
    } catch (_: Exception) {
        dateString
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoresScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAddChore: () -> Unit,
    onNavigateToProductivity: () -> Unit,
    viewModel: ChoreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val filterOption by viewModel.filterOption.collectAsState()
    
    var showEditDialog by remember { mutableStateOf<Chore?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(houseId) {
        viewModel.loadChores(houseId)
    }

    // Edit Chore Dialog
    showEditDialog?.let { chore ->
        EditChoreDialog(
            chore = chore,
            onDismiss = { showEditDialog = null },
            onSave = { taskName, description, dueDate, assignedTo ->
                viewModel.updateChore(
                    choreId = chore.id,
                    taskName = taskName,
                    description = description,
                    dueDate = dueDate,
                    assignedTo = assignedTo
                )
                showEditDialog = null
                scope.launch {
                    snackbarHostState.showSnackbar("Chore updated successfully")
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Chores", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = onNavigateToProductivity) {
                        Icon(Icons.Default.Assessment, "Productivity", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        floatingActionButton = {
            `in`.xroden.flockr.ui.components.buttons.FlockrExtendedFab(
                text = "Add Chore",
                icon = Icons.Default.Add,
                onClick = onNavigateToAddChore
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (val state = uiState) {
            is ChoreUiState.Loading -> {
                `in`.xroden.flockr.ui.components.loading.ListScreenSkeleton(
                    modifier = Modifier.padding(padding)
                )
            }
            is ChoreUiState.Success -> {
                val filteredChores = when (filterOption) {
                    ChoreFilter.ALL -> state.allChores
                    ChoreFilter.ACTIVE -> state.activeChores
                    ChoreFilter.COMPLETED -> state.completedChores
                }

                if (state.allChores.isEmpty()) {
                    EmptyChoresState(Modifier.fillMaxSize().padding(padding), onAddChore = onNavigateToAddChore)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                var selectedTab by remember { mutableIntStateOf(if (filterOption == ChoreFilter.COMPLETED) 1 else 0) }
                                val tabs = listOf("Active", "Completed")

                                LaunchedEffect(selectedTab) {
                                    viewModel.setFilter(if (selectedTab == 0) ChoreFilter.ACTIVE else ChoreFilter.COMPLETED)
                                }

                                `in`.xroden.flockr.ui.components.inputs.PillSelector(
                                    tabs = tabs,
                                    selectedIndex = selectedTab,
                                    onTabSelected = { selectedTab = it },
                                    counts = listOf(state.activeChores.size, state.completedChores.size)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${filteredChores.size} ${filterOption.label.lowercase()}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (filterOption == ChoreFilter.COMPLETED && filteredChores.isNotEmpty()) {
                                        TextButton(
                                            onClick = { viewModel.clearCompletedChores(houseId) },
                                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Clear Completed")
                                        }
                                    }
                                }
                            }
                        }

                        items(items = filteredChores, key = { it.id }) { chore ->
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
                                        viewModel.deleteChore(chore.id, houseId)
                                        snackbarHostState.showSnackbar("Chore deleted")
                                    }
                                }
                            )
                        }

                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
            is ChoreUiState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Default.Error, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                        Text("Error loading chores", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.loadChores(houseId) }) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
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
    val isOverdueChore = chore.dueDate?.let { isOverdue(it.toString()) && !chore.isCompleted } ?: false

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isOverdueChore) MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Modern Checkbox
                Surface(
                    onClick = onToggleComplete,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    color = if (chore.isCompleted) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.size(32.dp),
                    border = BorderStroke(
                        1.5.dp,
                        if (chore.isCompleted) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (chore.isCompleted) {
                            Icon(
                                Icons.Default.Check,
                                "Completed",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = chore.taskName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (chore.isCompleted) TextDecoration.LineThrough else null,
                        color = if (chore.isCompleted) 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) 
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                    
                    chore.description?.takeIf { it.isNotBlank() }?.let { desc ->
                        Spacer(Modifier.height(2.dp))
                        Text(
                            desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }

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
                                onClick = { showMenu = false; onEdit() },
                                leadingIcon = { Icon(Icons.Default.Edit, null) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = { showMenu = false; onDelete() },
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

            // Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                chore.dueDate?.let { date ->
                    Badge(
                        icon = Icons.Default.CalendarToday,
                        text = formatDate(date.toString()),
                        containerColor = if (isOverdueChore) 
                            MaterialTheme.colorScheme.errorContainer 
                        else if (chore.isCompleted) 
                            MaterialTheme.colorScheme.surfaceVariant 
                        else 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        contentColor = if (isOverdueChore) 
                            MaterialTheme.colorScheme.onErrorContainer 
                        else if (chore.isCompleted) 
                            MaterialTheme.colorScheme.onSurfaceVariant 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                }

                chore.createdByName?.let { name ->
                    Badge(
                        Icons.Default.Add,
                        name,
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                chore.assignedToName?.let { name ->
                    Badge(
                        Icons.Default.PersonOutline,
                        name,
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }

                if (chore.isCompleted && chore.completedByName != null) {
                    Badge(
                        Icons.Default.CheckCircle,
                        chore.completedByName,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun Badge(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, containerColor: Color, contentColor: Color) {
    Surface(shape = MaterialTheme.shapes.extraSmall, color = containerColor) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(14.dp), tint = contentColor)
            Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = contentColor, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
fun EmptyChoresState(modifier: Modifier = Modifier, onAddChore: () -> Unit) {
    Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(80.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.CheckCircle, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(24.dp))
        Text("No Chores Yet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text("Add household tasks to keep everyone organized", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onAddChore, shape = MaterialTheme.shapes.medium) {
            Icon(Icons.Default.Add, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add First Chore", fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditChoreDialog(
    chore: Chore,
    onDismiss: () -> Unit,
    onSave: (String, String?, LocalDate?, String?) -> Unit
) {
    var taskName by remember { mutableStateOf(chore.taskName) }
    var description by remember { mutableStateOf(chore.description ?: "") }
    var dueDate by remember { mutableStateOf<LocalDate?>(chore.dueDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate?.atStartOfDayIn(TimeZone.currentSystemDefault())?.toEpochMilliseconds() ?: Clock.System.now().toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                         dueDate = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).date
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(Modifier.fillMaxWidth().padding(16.dp), shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Edit Chore", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    label = { Text("Task Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Box(Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                    OutlinedTextField(
                        value = dueDate?.let { formatDate(it.toString()) } ?: "",
                        onValueChange = {},
                        label = { Text("Due Date") },
                        placeholder = { Text("Select date") },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                         colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                   TextButton(onClick = onDismiss) { Text("Cancel") }
                   Spacer(Modifier.width(8.dp))
                   Button(
                       onClick = { onSave(taskName, description.takeIf { it.isNotBlank() }, dueDate, null) },
                       enabled = taskName.isNotBlank()
                   ) { Text("Save") }
                }
            }
        }
    }
}
