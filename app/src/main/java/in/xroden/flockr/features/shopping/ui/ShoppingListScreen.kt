package `in`.xroden.flockr.features.shopping.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.features.shopping.domain.ShoppingUiState
import kotlinx.coroutines.launch
import `in`.xroden.flockr.features.shopping.model.ShoppingItem
import `in`.xroden.flockr.features.shopping.domain.ShoppingViewModel
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAddExpenseWithData: (String, Int) -> Unit = { _, _ -> },
    viewModel: ShoppingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<ShoppingItem?>(null) }
    var showConvertDialog by remember { mutableStateOf<ShoppingItem?>(null) }
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("To Buy", "Purchased")
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(houseId) {
        viewModel.loadShoppingItems(houseId)
    }

    // Add Item Dialog
    if (showAddDialog) {
        AddShoppingItemDialog(
            houseId = houseId,
            onDismiss = { showAddDialog = false },
            onAdd = { itemName, quantity ->
                scope.launch {
                    viewModel.addItem(houseId, itemName, quantity)
                    showAddDialog = false
                    snackbarHostState.showSnackbar("Item added to list")
                }
            }
        )
    }

    // Edit Item Dialog
    showEditDialog?.let { item ->
        EditShoppingItemDialog(
            item = item,
            onDismiss = { showEditDialog = null },
            onSave = { itemName, quantity ->
                viewModel.updateItem(
                    itemId = item.id,
                    itemName = itemName,
                    quantity = quantity
                )
                showEditDialog = null
                scope.launch {
                    snackbarHostState.showSnackbar("Item updated")
                }
            }
        )
    }

    // Convert to Expense Dialog
    showConvertDialog?.let { item ->
        ConvertToExpenseDialog(
            item = item,
            onDismiss = { showConvertDialog = null },
            onConvert = {
                val itemToConvert = item
                showConvertDialog = null
                val qty = itemToConvert.quantity?.toIntOrNull() ?: 1
                onNavigateToAddExpenseWithData(itemToConvert.itemName, qty)
            },
            onSkip = { showConvertDialog = null }
        )
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets.systemBars,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Shopping List",
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
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.Add, "Add") },
                    text = { Text("Add Item") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.large
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when (val state = uiState) {
                is ShoppingUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is ShoppingUiState.Success -> {
                    val items = state.items
                    val pendingItems = items.filter { !it.isPurchased }
                    val purchasedItems = items.filter { it.isPurchased }
                    val currentItems = if (selectedTab == 0) pendingItems else purchasedItems

                    if (currentItems.isEmpty()) {
                        EmptyShoppingState(
                            modifier = Modifier.fillMaxSize(),
                            onAddItem = { showAddDialog = true },
                            isPurchasedTab = selectedTab == 1
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Header for Purchased Tab
                            if (selectedTab == 1) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${purchasedItems.size} items purchased",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        TextButton(
                                            onClick = { viewModel.clearPurchasedItems(houseId) },
                                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Clear All")
                                        }
                                    }
                                }
                            }

                            items(currentItems, key = { it.id }) { item ->
                                ShoppingItemCard(
                                    item = item,
                                    onChecked = {
                                        if (!item.isPurchased) {
                                            scope.launch {
                                                viewModel.markAsPurchased(item.id, houseId, item.itemName)
                                                showConvertDialog = item
                                            }
                                        }
                                    },
                                    onEdit = { showEditDialog = item },
                                    onDelete = {
                                        scope.launch {
                                            viewModel.deleteItem(item.id)
                                            snackbarHostState.showSnackbar("Item removed")
                                        }
                                    },
                                    isPurchasedTab = selectedTab == 1
                                )
                            }

                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
                is ShoppingUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
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
                                text = "Error loading shopping list",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { viewModel.loadShoppingItems(houseId) },
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShoppingItemCard(
    item: ShoppingItem,
    onChecked: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isPurchasedTab: Boolean
) {
    var isChecked by remember { mutableStateOf(item.isPurchased) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isPurchasedTab) 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!isPurchasedTab) {
                // Custom Checkbox
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { 
                            isChecked = true
                            onChecked() 
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isChecked) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            } else {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Item Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.itemName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPurchasedTab) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isPurchasedTab) TextDecoration.LineThrough else null
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item.quantity?.let { qty ->
                        Text(
                            text = qty,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.shapes.extraSmall
                            ).padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    
                    Text(
                        text = if (isPurchasedTab) 
                            "Purchased by ${item.purchasedByName ?: "Unknown"}" 
                        else 
                            "Added by ${item.addedByName ?: "Unknown"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Actions
            if (!isPurchasedTab) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Delete, "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddShoppingItemDialog(
    houseId: String,
    onDismiss: () -> Unit,
    onAdd: (String, String?) -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Add Item", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onAdd(itemName, quantity.takeIf { it.isNotBlank() }) },
                        enabled = itemName.isNotBlank()
                    ) { Text("Add") }
                }
            }
        }
    }
}

@Composable
fun EditShoppingItemDialog(
    item: ShoppingItem,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit
) {
    var itemName by remember { mutableStateOf(item.itemName) }
    var quantity by remember { mutableStateOf(item.quantity ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Edit Item", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(itemName, quantity.takeIf { it.isNotBlank() }) },
                        enabled = itemName.isNotBlank()
                    ) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun ConvertToExpenseDialog(
    item: ShoppingItem,
    onDismiss: () -> Unit,
    onConvert: () -> Unit,
    onSkip: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.ShoppingCart, null) },
        title = { Text("Item Purchased!") },
        text = { Text("Convert \"${item.itemName}\" to an expense?") },
        confirmButton = { Button(onClick = onConvert) { Text("Convert") } },
        dismissButton = { TextButton(onClick = onSkip) { Text("Skip") } }
    )
}

@Composable
fun EmptyShoppingState(
    modifier: Modifier = Modifier,
    onAddItem: () -> Unit,
    isPurchasedTab: Boolean
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (isPurchasedTab) Icons.Default.ShoppingBag else Icons.Default.ShoppingCart,
                    null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            if (isPurchasedTab) "No purchased items" else "Shopping list empty",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        if (!isPurchasedTab) {
            Text(
                "Add items you need to buy",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onAddItem) {
                Text("Add Item")
            }
        }
    }
}

// Helper function to format date
private fun formatDate(isoDate: String): String {
    return try {
        val instant = kotlinx.datetime.Instant.parse(isoDate)
        val date = instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        "$month ${date.dayOfMonth}"
    } catch (e: Exception) {
        "Recently"
    }
}
