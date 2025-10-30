package `in`.xroden.flockr.ui.screens.chores

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChoreDialogAdvanced(
    onDismiss: () -> Unit,
    onAdd: (String, String?, String?, Boolean, String?) -> Unit
) {
    var taskName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isRecurring by remember { mutableStateOf(false) }
    var recurrencePattern by remember { mutableStateOf("daily") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Chore") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it },
                    label = { Text("Task Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it }
                    )
                    Text("Recurring Chore", modifier = Modifier.padding(start = 8.dp))
                }

                if (isRecurring) {
                    var expanded by remember { mutableStateOf(false) }
                    val patterns = listOf("Daily", "Weekly", "Bi-weekly", "Monthly")

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = recurrencePattern.replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Recurrence Pattern") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            patterns.forEach { pattern ->
                                DropdownMenuItem(
                                    text = { Text(pattern) },
                                    onClick = {
                                        recurrencePattern = pattern.lowercase()
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(
                        taskName,
                        description.takeIf { it.isNotBlank() },
                        null,
                        isRecurring,
                        if (isRecurring) recurrencePattern else null
                    )
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

