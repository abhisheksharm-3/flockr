package `in`.xroden.flockr.features.expenses.ui.onetime

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.features.expenses.model.UserBalance
import `in`.xroden.flockr.features.expenses.domain.ExpenseViewModel
import `in`.xroden.flockr.features.expenses.domain.BalanceUiState
import `in`.xroden.flockr.ui.components.loading.ListScreenSkeleton
import java.math.BigDecimal
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalancesScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: ExpenseViewModel = hiltViewModel()
) {
    val uiState by viewModel.balanceState.collectAsState()
    val houseConfig by viewModel.houseConfig.collectAsState()
    val currencySymbol = remember(houseConfig) {
        houseConfig?.getCurrencySymbol() ?: "$"
    }

    LaunchedEffect(houseId) {
        viewModel.loadBalances(houseId)
        viewModel.loadHouseConfig(houseId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Balances & IOUs", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is BalanceUiState.Loading -> {
                    ListScreenSkeleton(modifier = Modifier.fillMaxSize())
                }
                is BalanceUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                        Button(onClick = { viewModel.loadBalances(houseId) }) { Text("Retry") }
                    }
                }
                is BalanceUiState.Success -> {
                    BalancesContent(
                        houseId = houseId,
                        balances = state.balances,
                        currentUserId = viewModel.getCurrentUserId() ?: "",
                        currencySymbol = currencySymbol,
                        onSettle = { userBalance, amount, notes ->
                            viewModel.settleBalance(
                                houseId = houseId,
                                payeeId = userBalance.userId,
                                payeeName = userBalance.fullName ?: "User",
                                amount = amount.toBigDecimal(),
                                notes = notes
                            )
                        },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun BalancesContent(
    houseId: String,
    balances: List<UserBalance>,
    currentUserId: String,
    currencySymbol: String,
    onSettle: (UserBalance, Double, String) -> Unit,
    viewModel: ExpenseViewModel
) {
    val otherBalances = remember(balances, currentUserId) {
        balances.filter { it.userId != currentUserId }
    }
    
    // Calculate totals based on MY balance
    val (totalYouOwe, totalYouAreOwed) = remember(balances, currentUserId) {
        val myBalance = balances.find { it.userId == currentUserId }?.balance?.toDouble() ?: 0.0
        val owe = if (myBalance < 0) abs(myBalance) else 0.0
        val owed = if (myBalance > 0) abs(myBalance) else 0.0
        owe to owed
    }

    LazyColumn(
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Summary Card
        item {
            NetBalanceCard(
                totalYouOwe = totalYouOwe,
                totalYouAreOwed = totalYouAreOwed,
                currencySymbol = currencySymbol
            )
        }

        item {
            Text(
                text = "Individual Balances",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (otherBalances.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.CheckCircle,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Text(
                            "All settled up! 🎉",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "No outstanding balances with housemates",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(
                items = otherBalances,
                key = { it.userId }
            ) { balance ->
                BalanceItemCard(
                    houseId = houseId,
                    balance = balance,
                    currencySymbol = currencySymbol,
                    onSettle = onSettle,
                    viewModel = viewModel,
                    currentUserId = currentUserId
                )
            }
        }
    }
}

@Composable
fun NetBalanceCard(
    totalYouOwe: Double,
    totalYouAreOwed: Double,
    currencySymbol: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.AccountBalance,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    "Net Balance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Balance amounts row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // You Owe Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(28.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.ArrowUpward,
                                        null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                "You Owe",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Text(
                            "$currencySymbol${"%.2f".format(totalYouOwe)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // You are Owed Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(28.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.ArrowDownward,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                "You're Owed",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            "$currencySymbol${"%.2f".format(totalYouAreOwed)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceItemCard(
    houseId: String,
    balance: UserBalance,
    currencySymbol: String,
    onSettle: (UserBalance, Double, String) -> Unit,
    viewModel: ExpenseViewModel,
    currentUserId: String
) {
    var expanded by remember { mutableStateOf(false) }
    var showSettleDialog by remember { mutableStateOf(false) }
    
    // Logic: 
    // balance.balance > 0 -> They have +50 (Owed by house/me) -> I owe them (relative)
    // balance.balance < 0 -> They have -50 (Owe house/me) -> They owe me
    val iOweThem = balance.balance > BigDecimal.ZERO
    val isSettled = balance.balance.compareTo(BigDecimal.ZERO) == 0
    val amount = balance.balance.abs().toDouble()
    
    val color = when {
        isSettled -> MaterialTheme.colorScheme.onSurfaceVariant
        iOweThem -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    
    val statusText = when {
        isSettled -> "Settled"
        iOweThem -> "You owe"
        else -> "Owes you"
    }

    // Breakdown State
    val debtBreakdowns by viewModel.debtBreakdownState.collectAsState()
    val loadingBreakdowns by viewModel.loadingBreakdowns.collectAsState()
    
    val payerId = if (iOweThem) currentUserId else balance.userId
    val payeeId = if (iOweThem) balance.userId else currentUserId
    val breakdownKey = "${payerId}_${payeeId}"
    
    val breakdownItems = debtBreakdowns[breakdownKey]
    val isBreakdownLoading = loadingBreakdowns.contains(breakdownKey)

    LaunchedEffect(expanded) {
        if (expanded && breakdownItems == null && !isBreakdownLoading) {
            viewModel.loadDebtBreakdown(houseId, payerId, payeeId)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = balance.fullName?.firstOrNull()?.toString()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                
                Spacer(Modifier.width(16.dp))
                
                Column(Modifier.weight(1f)) {
                    Text(balance.fullName ?: "User", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(statusText, style = MaterialTheme.typography.bodySmall, color = if (iOweThem) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                }

                Text(
                    text = "$currencySymbol${"%.2f".format(amount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            // Actions & Expansion
            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    
                    if (isSettled) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                             Text("All settled up!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    } else if (isBreakdownLoading) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(24.dp))
                        }
                    } else if (breakdownItems.isNullOrEmpty()) {
                         Text("No details available.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    } else {
                        breakdownItems.forEachIndexed { index, item ->
                            ListItem(
                                headlineContent = { Text(item.expenseName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface) },
                                supportingContent = { Text(item.date.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                trailingContent = {
                                    Text(
                                        text = "$currencySymbol${"%.2f".format(item.amountOwed.toDouble())}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.amountOwed < BigDecimal.ZERO) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            if (index < breakdownItems.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }
            
            // Settle Button only if I owe them
            if (iOweThem) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showSettleDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("Settle Up")
                }
            } else if (!expanded) {
                 Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                      Icon(Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                 }
            }
        }
    }

    if (showSettleDialog) {
        SettleBalanceDialog(
            balance = balance,
            currencySymbol = currencySymbol,
            onDismiss = { showSettleDialog = false },
            onSettle = { settlAmount, note ->
                onSettle(balance, settlAmount, note ?: "Settlement")
                showSettleDialog = false
            }
        )
    }
}

@Composable
fun SettleBalanceDialog(
    balance: UserBalance,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSettle: (Double, String?) -> Unit
) {
    var amount by remember { mutableStateOf(abs(balance.balance.toDouble()).toString()) }
    var description by remember { mutableStateOf("") }
    val isValid = amount.toDoubleOrNull() != null

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Settle Up", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("You are settling with ${balance.fullName ?: "User"}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    prefix = { Text(currencySymbol) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Note (Optional)") },
                    placeholder = { Text("e.g. Cleared all dues") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = { amount.toDoubleOrNull()?.let { onSettle(it, description.takeIf { d -> d.isNotBlank() }) } },
                        enabled = isValid,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Pay Now")
                    }
                }
            }
        }
    }
}
