package `in`.xroden.flockr.ui.screens.chores

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.data.model.Chore
import `in`.xroden.flockr.ui.viewmodel.ChoreUiState
import `in`.xroden.flockr.ui.viewmodel.ChoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoresScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: ChoreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(houseId) {
        viewModel.loadChores(houseId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chores") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Add Chore")
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is ChoreUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is ChoreUiState.Success -> {
                if (state.chores.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No chores yet")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.chores.filter { !it.isCompleted }) { chore ->
                            ChoreCard(
                                chore = chore,
                                onComplete = {
                                    viewModel.completeChore(chore.id, houseId, chore.taskName)
                                }
                            )
                        }
                    }
                }
            }
            is ChoreUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showAddDialog) {
        AddChoreDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { taskName, description, dueDate ->
                viewModel.createChore(houseId, taskName, description, dueDate, false, null)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ChoreCard(
    chore: Chore,
    onComplete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chore.taskName,
                    style = MaterialTheme.typography.titleMedium
                )
                chore.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                chore.dueDate?.let {
                    Text(
                        text = "Due: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            IconButton(onClick = onComplete) {
                Icon(Icons.Default.Check, "Mark complete")
            }
        }
    }
}

@Composable
fun AddChoreDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String?, String?) -> Unit
) {
    var taskName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Chore") },
        text = {
            Column {
                OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    label = { Text("Task Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(taskName, description.takeIf { it.isNotBlank() }, null)
                },
                enabled = taskName.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

