package `in`.xroden.flockr.features.chores.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.features.chores.domain.ChoreViewModel
import `in`.xroden.flockr.features.chores.domain.CreateChoreUiState
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChoreScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: ChoreViewModel = hiltViewModel()
) {
    var taskName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val createState by viewModel.createState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(createState) {
        when (createState) {
            is CreateChoreUiState.Success -> {
                onNavigateBack()
                viewModel.resetCreateState()
            }
            is CreateChoreUiState.Error -> {
                snackbarHostState.showSnackbar("Error adding chore")
                viewModel.resetCreateState()
            }
            else -> {}
        }
    }

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
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Add Chore") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.createChore(
                                houseId = houseId,
                                taskName = taskName,
                                description = description.takeIf { it.isNotBlank() },
                                dueDate = dueDate,
                                recurrencePattern = null,
                                assignedTo = null
                            )
                        },
                        enabled = taskName.isNotBlank()
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
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
                placeholder = { Text("Add any details...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = MaterialTheme.shapes.medium
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    // Format: Jan 15, 2025
                    value = dueDate?.let { 
                        val month = it.month.name.take(3).lowercase().replaceFirstChar { char -> char.uppercase() }
                        "$month ${it.dayOfMonth}, ${it.year}"
                    } ?: "",
                    onValueChange = { },
                    label = { Text("Due Date (Optional)") },
                    placeholder = { Text("Select date") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false, 
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                // Overlay to catch clicks
                Box(Modifier.matchParentSize().clickable { showDatePicker = true })
            }
        }
    }
}
