package `in`.xroden.flockr.features.shopping.ui

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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import `in`.xroden.flockr.features.shopping.model.ShoppingItem
import `in`.xroden.flockr.features.shopping.domain.ShoppingViewModel
import `in`.xroden.flockr.features.shopping.domain.ShoppingUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAddItem: () -> Unit,
    onNavigateToAddExpenseWithData: (String, Int) -> Unit = { _, _ -> },
    viewModel: ShoppingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showEditDialog by remember { mutableStateOf<ShoppingItem?>(null) }
    var showConvertDialog by remember { mutableStateOf<ShoppingItem?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = remember { listOf("To Buy", "Purchased") }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(houseId) {
        viewModel.loadShoppingItems(houseId)
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
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Shopping List", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToAddItem,
                    icon = { Icon(Icons.Default.Add, "Add") },
                    text = { Text("Add Item") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Pill Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tabs.forEachIndexed { index, title ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(if (selectedTab == index) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { selectedTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            title,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            when (val state = uiState) {
                is ShoppingUiState.Loading -> {
                    `in`.xroden.flockr.ui.components.loading.ListScreenSkeleton()
                }
                is ShoppingUiState.Success -> {
                    val items = state.items
                    // Optimize filtering with remember
                    val pendingItems = remember(items) { items.filter { !it.isPurchased } }
                    val purchasedItems = remember(items) { items.filter { it.isPurchased } }
                    val currentItems = if (selectedTab == 0) pendingItems else purchasedItems

                    if (currentItems.isEmpty()) {
                        EmptyShoppingState(
                            modifier = Modifier.fillMaxSize(),
                            onAddItem = onNavigateToAddItem,
                            isPurchasedTab = selectedTab == 1
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Header for Purchased Tab
                            if (selectedTab == 1) {
                                item(key = "header_purchased") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${purchasedItems.size} items purchased",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        TextButton(
                                            onClick = { viewModel.clearPurchasedItems(houseId) },
                                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Icon(Icons.Default.DeleteSweep, null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Clear All")
                                        }
                                    }
                                }
                            }

                            items(items = currentItems, key = { it.id }) { item ->
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
                            item(key = "spacer_bottom") { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
                is ShoppingUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Icon(Icons.Default.Error, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                            Text("Error loading shopping list", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                            Button(onClick = { viewModel.loadShoppingItems(houseId) }) {
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
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isPurchasedTab) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, if (isPurchasedTab) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!isPurchasedTab) {
                Surface(
                    onClick = { isChecked = true; onChecked() },
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(28.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {}
            } else {
                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), modifier = Modifier.size(28.dp))
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.itemName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (!isPurchasedTab) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isPurchasedTab) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isPurchasedTab) TextDecoration.LineThrough else null
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    item.quantity?.let { qty ->
                        Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)) {
                            Text(qty, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Text(
                        if (isPurchasedTab) "Purchased by ${item.purchasedByName ?: "Unknown"}" else "Added by ${item.addedByName ?: "Unknown"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = !showMenu }, Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (!isPurchasedTab) {
                        DropdownMenuItem(text = { Text("Edit") }, onClick = { showMenu = false; onEdit() }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                    }
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; onDelete() }, leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) })
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
        Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Edit Item", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(value = itemName, onValueChange = { itemName = it }, label = { Text("Item Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Quantity (Optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSave(itemName, quantity.takeIf { it.isNotBlank() }) }, enabled = itemName.isNotBlank()) { Text("Save") }
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
        icon = { Icon(Icons.Default.ShoppingCart, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Item Purchased!", style = MaterialTheme.typography.headlineSmall) },
        text = { Text("Convert \"${item.itemName}\" to an expense?", style = MaterialTheme.typography.bodyLarge) },
        confirmButton = { Button(onClick = onConvert) { Text("Convert") } },
        dismissButton = { TextButton(onClick = onSkip) { Text("Skip") } }
    )
}

@Composable
fun EmptyShoppingState(modifier: Modifier = Modifier, onAddItem: () -> Unit, isPurchasedTab: Boolean) {
    Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(96.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(if (isPurchasedTab) Icons.Default.ShoppingBag else Icons.Default.ShoppingCart, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(if (isPurchasedTab) "No purchased items" else "Shopping list empty", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        if (!isPurchasedTab) {
            Spacer(Modifier.height(8.dp))
            Text("Add items you need to buy", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
            Button(onClick = onAddItem, contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)) { Text("Add Item") }
        }
    }
}
