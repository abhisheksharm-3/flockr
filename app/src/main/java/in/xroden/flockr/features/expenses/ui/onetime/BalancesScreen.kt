package `in`.xroden.flockr.features.expenses.ui.onetime

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.features.expenses.model.UserBalance
import `in`.xroden.flockr.features.expenses.domain.BalanceViewModel
import `in`.xroden.flockr.features.expenses.domain.BalanceUiState
import `in`.xroden.flockr.features.house.domain.HouseManagementViewModel
import `in`.xroden.flockr.ui.components.loading.ListScreenSkeleton
import java.math.BigDecimal
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalancesScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: BalanceViewModel = hiltViewModel(),
    houseManagementViewModel: HouseManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.balanceState.collectAsState()
    val houseConfig by viewModel.houseConfig.collectAsState()
    val currencySymbol = remember(houseConfig) {
        houseConfig?.getCurrencySymbol() ?: "$"
    }
    
    val currentUserId = houseManagementViewModel.getCurrentUserId() ?: ""

    LaunchedEffect(houseId) {
        viewModel.loadBalances(houseId)
        viewModel.loadHouseConfig(houseId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Balances & IOUs",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
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
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Warning,
                                    null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Text(
                            state.message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(
                            onClick = { viewModel.loadBalances(houseId) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Retry", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                is BalanceUiState.Success -> {
                    // Get current user's name from balances
                    val currentUserName = state.balances.find { it.userId == currentUserId }?.fullName ?: "You"
                    
                    BalancesContent(
                        houseId = houseId,
                        balances = state.balances,
                        currentUserId = currentUserId,
                        currentUserName = currentUserName,
                        currencySymbol = currencySymbol,
                        onSettle = { userBalance, amount, notes ->
                            viewModel.settleBalance(
                                houseId = houseId,
                                currentUserId = currentUserId,
                                payeeId = userBalance.userId,
                                payerName = currentUserName,
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
    currentUserName: String,
    currencySymbol: String,
    onSettle: (UserBalance, Double, String) -> Unit,
    viewModel: BalanceViewModel
) {
    val otherBalances = remember(balances, currentUserId) {
        balances.filter { it.userId != currentUserId }
    }

    val (totalYouOwe, totalYouAreOwed) = remember(balances, currentUserId) {
        val myBalance = balances.find { it.userId == currentUserId }?.balance?.toDouble() ?: 0.0
        val owe = if (myBalance < 0) abs(myBalance) else 0.0
        val owed = if (myBalance > 0) abs(myBalance) else 0.0
        owe to owed
    }

    val netBalance = totalYouAreOwed - totalYouOwe

    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Hero Card with gradient
        item {
            BalanceHeroCard(
                netBalance = netBalance,
                totalYouOwe = totalYouOwe,
                totalYouAreOwed = totalYouAreOwed,
                currencySymbol = currencySymbol
            )
        }

        // Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "With Housemates",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "${otherBalances.size} people",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (otherBalances.isEmpty()) {
            item {
                AllSettledCard()
            }
        } else {
            items(items = otherBalances, key = { it.userId }) { balance ->
                BalancePersonCard(
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
private fun BalanceHeroCard(
    netBalance: Double,
    totalYouOwe: Double,
    totalYouAreOwed: Double,
    currencySymbol: String
) {
    val isPositive = netBalance >= 0
    val cardColor = if (isPositive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
    val contentColor = if (isPositive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Net Balance Header
            Text(
                "Net Balance",
                style = MaterialTheme.typography.labelLarge,
                color = contentColor.copy(alpha = 0.8f)
            )

            // Large Amount
            Text(
                "${if (isPositive) "+" else "-"}$currencySymbol${"%.2f".format(kotlin.math.abs(netBalance))}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )

            // Status Text
            Text(
                if (isPositive) "You're in credit" else "You owe overall",
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.8f)
            )

            Spacer(Modifier.height(8.dp))

            // Breakdown row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // You Owe
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = contentColor.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.ArrowUpward,
                                null,
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$currencySymbol${"%.2f".format(totalYouOwe)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    Text(
                        "You owe",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(60.dp)
                        .background(contentColor.copy(alpha = 0.3f))
                )

                // You're Owed
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = contentColor.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.ArrowDownward,
                                null,
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$currencySymbol${"%.2f".format(totalYouAreOwed)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    Text(
                        "You're owed",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AllSettledCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Celebration,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Text(
                "All settled up!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "No outstanding balances with housemates",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BalancePersonCard(
    houseId: String,
    balance: UserBalance,
    currencySymbol: String,
    onSettle: (UserBalance, Double, String) -> Unit,
    viewModel: BalanceViewModel,
    currentUserId: String
) {
    var expanded by remember { mutableStateOf(false) }
    var showSettleDialog by remember { mutableStateOf(false) }

    val iOweThem = balance.balance > BigDecimal.ZERO
    val isSettled = balance.balance.compareTo(BigDecimal.ZERO) == 0
    val amount = balance.balance.abs().toDouble()

    val statusColor = when {
        isSettled -> MaterialTheme.colorScheme.tertiary
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
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            balance.fullName?.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }

                // Name and Status
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        balance.fullName ?: "User",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = statusColor.copy(alpha = 0.1f)
                        ) {
                            Text(
                                statusText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Amount
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$currencySymbol${"%.2f".format(amount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    Icon(
                        Icons.Default.ExpandMore,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expandable Section
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )

                    when {
                        isSettled -> {
                            Text(
                                "All settled up!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                        isBreakdownLoading -> {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                        breakdownItems.isNullOrEmpty() -> {
                            Text(
                                "No breakdown available",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> {
                            breakdownItems.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            item.expenseName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            item.date.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        "$currencySymbol${"%.2f".format(item.amountOwed.toDouble().let { abs(it) })}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (item.amountOwed < BigDecimal.ZERO)
                                            MaterialTheme.colorScheme.error
                                        else
                                            MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // Settle Button
                    if (iOweThem && !isSettled) {
                        Button(
                            onClick = { showSettleDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                Icons.Outlined.Payments,
                                null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Settle Up", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    if (showSettleDialog) {
        SettleBalanceDialog(
            balance = balance,
            currencySymbol = currencySymbol,
            onDismiss = { showSettleDialog = false },
            onSettle = { settleAmount, note ->
                onSettle(balance, settleAmount, note ?: "Settlement")
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
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Payments,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Settle Up",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Pay ${balance.fullName ?: "User"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    prefix = { Text(currencySymbol) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Note (Optional)") },
                    placeholder = { Text("e.g. Paid via UPI") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            amount.toDoubleOrNull()?.let {
                                onSettle(it, description.takeIf { d -> d.isNotBlank() })
                            }
                        },
                        enabled = isValid,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Pay Now", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
