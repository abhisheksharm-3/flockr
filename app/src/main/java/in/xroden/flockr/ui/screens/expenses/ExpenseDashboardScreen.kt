package `in`.xroden.flockr.ui.screens.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.ui.viewmodel.ExpenseViewModel
import `in`.xroden.flockr.ui.viewmodel.PerDiemViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDashboardScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    expenseViewModel: ExpenseViewModel = hiltViewModel(),
    perDiemViewModel: PerDiemViewModel = hiltViewModel()
) {
    var selectedMonth by remember {
        mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")))
    }

    val monthlySummary by expenseViewModel.monthlySummary.collectAsState()
    val spendByMember by expenseViewModel.spendByMember.collectAsState()
    val spendByCategory by expenseViewModel.spendByCategory.collectAsState()
    val perDiemItemized by perDiemViewModel.perDiemBillItemized.collectAsState()
    val perDiemByMember by perDiemViewModel.perDiemBillByMember.collectAsState()

    LaunchedEffect(houseId, selectedMonth) {
        expenseViewModel.loadMonthlySummary(houseId, selectedMonth)
        expenseViewModel.loadSpendByMember(houseId, selectedMonth)
        expenseViewModel.loadSpendByCategory(houseId, selectedMonth)
        perDiemViewModel.loadPerDiemReports(houseId, selectedMonth)
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    // Function to generate and share report
    fun generateAndShareReport() {
        val reportText = buildString {
            appendLine("Expense Report - $selectedMonth")
            appendLine("=" .repeat(40))
            appendLine()

            monthlySummary?.let { summary ->
                appendLine("Monthly Summary:")
                appendLine("Total Expenses: $${String.format("%.2f", summary.totalExpenses)}")
                appendLine("Recurring: $${String.format("%.2f", summary.recurringExpenses)}")
                appendLine("One-Time: $${String.format("%.2f", summary.oneTimeExpenses)}")
                appendLine("Per-Diem: $${String.format("%.2f", summary.perDiemExpenses)}")
                appendLine()
            }

            if (spendByMember.isNotEmpty()) {
                appendLine("Spend by Member:")
                spendByMember.forEach { member ->
                    appendLine("  ${member.fullName ?: "Unknown"}: $${String.format("%.2f", member.totalSpent)}")
                }
                appendLine()
            }

            if (spendByCategory.isNotEmpty()) {
                appendLine("Spend by Category:")
                spendByCategory.forEach { category ->
                    appendLine("  ${category.category}: $${String.format("%.2f", category.totalAmount)}")
                }
                appendLine()
            }

            if (perDiemItemized.isNotEmpty()) {
                appendLine("Per-Diem Itemized:")
                perDiemItemized.forEach { item ->
                    appendLine("  ${item.itemName}: ${item.totalQuantity} ${item.unit} × $${item.rate} = $${String.format("%.2f", item.totalAmount)}")
                }
                appendLine()
            }

            if (perDiemByMember.isNotEmpty()) {
                appendLine("Per-Diem by Member:")
                perDiemByMember.forEach { member ->
                    appendLine("  ${member.fullName ?: "Unknown"}: $${String.format("%.2f", member.totalAmount)}")
                }
            }
        }

        val sendIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, reportText)
            type = "text/plain"
        }
        val shareIntent = android.content.Intent.createChooser(sendIntent, "Share Expense Report")
        context.startActivity(shareIntent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { generateAndShareReport() },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Generate Report")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month selector
            item {
                OutlinedTextField(
                    value = selectedMonth,
                    onValueChange = { selectedMonth = it },
                    label = { Text("Month (YYYY-MM)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Monthly Summary
            item {
                monthlySummary?.let { summary ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Monthly Summary",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            SummaryRow("Total Expenses", summary.totalExpenses)
                            SummaryRow("Recurring", summary.recurringExpenses)
                            SummaryRow("One-Time", summary.oneTimeExpenses)
                            SummaryRow("Per-Diem", summary.perDiemExpenses)
                        }
                    }
                }
            }

            // Spend by Member
            item {
                Text(
                    text = "Spend by Member",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            items(spendByMember) { member ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = member.fullName ?: "Unknown",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "$${"%.2f".format(member.totalSpent)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Spend by Category
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Spend by Category",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            items(spendByCategory) { category ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = category.category,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "$${"%.2f".format(category.totalAmount)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Per-Diem Itemized
            if (perDiemItemized.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Per-Diem Itemized",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(perDiemItemized) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = item.itemName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${item.totalQuantity} ${item.unit} × $${item.rate} = $${"%.2f".format(item.totalAmount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Per-Diem by Member
            if (perDiemByMember.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Per-Diem by Member",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(perDiemByMember) { member ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = member.fullName ?: "Unknown",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${member.totalQuantity} items",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "$${"%.2f".format(member.totalAmount)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
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
fun SummaryRow(label: String, amount: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "$${"%.2f".format(amount)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

