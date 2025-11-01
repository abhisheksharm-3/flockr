package `in`.xroden.flockr.ui.screens.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.data.model.UserBalance
import `in`.xroden.flockr.ui.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalancesScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    val balances by viewModel.balances.collectAsState()
    var showSettleDialog by remember { mutableStateOf(false) }
    var selectedBalance by remember { mutableStateOf<UserBalance?>(null) }

    LaunchedEffect(houseId) {
        viewModel.loadBalances(houseId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Balances") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                `in`.xroden.flockr.ui.components.headers.FlockrSectionHeader(
                    text = "Who Owes Whom"
                )
            }

            items(balances) { balance ->
                BalanceCard(
                    balance = balance,
                    onSettleClick = {
                        selectedBalance = balance
                        showSettleDialog = true
                    }
                )
            }

            if (balances.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "All balances are settled!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showSettleDialog && selectedBalance != null) {
        SettleBalanceDialog(
            balance = selectedBalance!!,
            houseId = houseId,
            onDismiss = { showSettleDialog = false },
            onSettle = { amount, description ->
                viewModel.settleBalance(
                    houseId = houseId,
                    payeeId = selectedBalance!!.userId,
                    amount = amount,
                    description = description
                ) {
                    showSettleDialog = false
                    viewModel.loadBalances(houseId)
                }
            }
        )
    }
}

@Composable
fun BalanceCard(
    balance: UserBalance,
    onSettleClick: () -> Unit
) {
    `in`.xroden.flockr.ui.components.cards.FlockrCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = balance.fullName ?: "Unknown User",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (balance.balance > 0) "Owes you" else "You owe",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${kotlin.math.abs(balance.balance)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (balance.balance > 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
                if (balance.balance < 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onSettleClick,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Settle")
                    }
                }
            }
        }
    }
}

@Composable
fun SettleBalanceDialog(
    balance: UserBalance,
    houseId: String,
    onDismiss: () -> Unit,
    onSettle: (Double, String?) -> Unit
) {
    var amount by remember { mutableStateOf(kotlin.math.abs(balance.balance).toString()) }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settle Balance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Settling with ${balance.fullName}")

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Note (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    amount.toDoubleOrNull()?.let { amt ->
                        onSettle(amt, description.takeIf { it.isNotBlank() })
                    }
                },
                enabled = amount.toDoubleOrNull() != null && amount.toDoubleOrNull()!! > 0
            ) {
                Text("Settle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

