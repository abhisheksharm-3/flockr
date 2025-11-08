package `in`.xroden.flockr.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun JoinHouseDialog(
    onDismiss: () -> Unit,
    onJoinHouse: (String) -> Unit
) {
    var inviteCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Title
                Text(
                    text = "Join Household",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Description
                Text(
                    text = "Enter the 6-character invite code to join an existing household",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Invite Code Input
                OutlinedTextField(
                    value = inviteCode,
                    onValueChange = {
                        if (it.length <= 6) {
                            inviteCode = it.uppercase()
                        }
                    },
                    label = { Text("Invite Code") },
                    placeholder = { Text("ABC123") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (inviteCode.length == 6 && !isLoading) {
                                isLoading = true
                                onJoinHouse(inviteCode)
                            }
                        }
                    ),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp)
                )

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (!isLoading) {
                                isLoading = true
                                onJoinHouse(inviteCode)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = inviteCode.length == 6 && !isLoading,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Join", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

