/** The house shopping list: add in one line, grouped by aisle, tick off, and turn what was bought into an expense. */
package `in`.xroden.flockr.features.shopping.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.house.model.nameInSentence
import `in`.xroden.flockr.features.shopping.model.SHOPPING_CATEGORIES
import `in`.xroden.flockr.features.shopping.model.ShoppingItem
import `in`.xroden.flockr.features.shopping.presentation.ShoppingUiState
import `in`.xroden.flockr.features.shopping.presentation.ShoppingViewModel
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.ui.components.states.EmptyState
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.rememberHaptics

@Composable
fun ShoppingListScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onRecordExpense: (itemName: String) -> Unit,
    viewModel: ShoppingViewModel = hiltViewModel(),
) {
    val haptics = rememberHaptics()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<ShoppingItem?>(null) }

    LaunchedEffect(houseId) { viewModel.load(houseId) }
    LaunchedEffect(Unit) {
        viewModel.notices.collect { notice ->
            haptics.error()
            snackbarHostState.showSnackbar(notice.message)
        }
    }

    Scaffold(
        topBar = { FlockrTopAppBar(title = "Shopping list", onNavigateBack = onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            QuickAdd(onAdd = { name, category -> viewModel.add(houseId, name, null, category) })
            Box(Modifier.fillMaxSize()) {
                when (val current = state) {
                    ShoppingUiState.Loading -> LoadingIndicator(Modifier.align(Alignment.Center))
                    is ShoppingUiState.Error -> ErrorState(current.message, onRetry = { viewModel.load(houseId) })
                    is ShoppingUiState.Ready -> if (current.toBuy.isEmpty() && current.bought.isEmpty()) {
                        EmptyState(icon = Icons.Rounded.ShoppingCart, title = "The list is empty", subtitle = "Add what the house needs above. Everyone sees it as it changes.")
                    } else {
                        ShoppingContent(
                            state = current,
                            onToggle = { item, bought -> viewModel.setPurchased(item, bought) },
                            onEdit = { editing = it },
                            onClearBought = { viewModel.clearBought(houseId) },
                            onRecordExpense = { onRecordExpense(current.bought.joinToString { it.itemName }) },
                        )
                    }
                }
            }
        }
    }

    editing?.let { item ->
        EditItemDialog(
            item = item,
            onSave = { name, quantity, category ->
                editing = null
                viewModel.update(item, name, quantity, category)
            },
            onDelete = {
                editing = null
                viewModel.delete(item)
            },
            onDismiss = { editing = null },
        )
    }
}

/** One line to add an item, with the aisle it goes in. The aisle stays chosen for the next item. */
@Composable
private fun QuickAdd(onAdd: (String, String?) -> Unit) {
    val haptics = rememberHaptics()
    var name by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    val submit = {
        if (name.isNotBlank()) {
            haptics.tap()
            onAdd(name, category)
            name = ""
        }
    }
    Column(Modifier.padding(horizontal = Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("Add an item") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            trailingIcon = { IconButton(onClick = submit, enabled = name.isNotBlank()) { Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Add") } },
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            items(SHOPPING_CATEGORIES) { aisle ->
                FilterChip(selected = category == aisle, onClick = { haptics.select(); category = if (category == aisle) null else aisle }, label = { Text(aisle) })
            }
        }
    }
}

@Composable
private fun ShoppingContent(
    state: ShoppingUiState.Ready,
    onToggle: (ShoppingItem, Boolean) -> Unit,
    onEdit: (ShoppingItem) -> Unit,
    onClearBought: () -> Unit,
    onRecordExpense: () -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(bottom = Spacing.xxxxl)) {
        state.toBuy.forEach { (aisle, items) ->
            item(key = "aisle_$aisle") { Heading(aisle) }
            items(items, key = { it.id }) { item -> ItemRow(item, state, onToggle = { onToggle(item, it) }, onClick = { onEdit(item) }, modifier = Modifier.animateItem()) }
        }
        if (state.bought.isNotEmpty()) {
            item(key = "bought_heading") {
                Row(Modifier.fillMaxWidth().padding(end = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { Heading("Bought") }
                    TextButton(onClick = onRecordExpense) { Text("Record as expense") }
                    TextButton(onClick = onClearBought) { Text("Clear") }
                }
            }
            items(state.bought, key = { it.id }) { item -> ItemRow(item, state, onToggle = { onToggle(item, it) }, onClick = { onEdit(item) }, modifier = Modifier.animateItem()) }
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmallEmphasized, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = Spacing.lg, top = Spacing.lg, bottom = Spacing.xs))
}

@Composable
private fun ItemRow(item: ShoppingItem, state: ShoppingUiState.Ready, onToggle: (Boolean) -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptics = rememberHaptics()
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = item.isPurchased, onCheckedChange = { haptics.toggle(it); onToggle(it) })
        Column(Modifier.weight(1f).padding(vertical = Spacing.sm)) {
            Text(
                listOfNotNull(item.itemName, item.quantity?.let { "· $it" }).joinToString(" "),
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (item.isPurchased) TextDecoration.LineThrough else null,
            )
            val who = if (item.isPurchased) item.purchasedBy?.let { "bought by ${state.members.nameInSentence(it, state.viewerId)}" } else "added by ${state.members.nameInSentence(item.addedBy, state.viewerId)}"
            who?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun EditItemDialog(item: ShoppingItem, onSave: (String, String?, String?) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(item.itemName) }
    var quantity by rememberSaveable { mutableStateOf(item.quantity.orEmpty()) }
    var category by rememberSaveable { mutableStateOf(item.category) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                FlockrTextField(value = name, onValueChange = { name = it }, label = "Item", modifier = Modifier.fillMaxWidth())
                FlockrTextField(value = quantity, onValueChange = { quantity = it }, label = "Quantity", placeholder = "2 kg, a dozen…", modifier = Modifier.fillMaxWidth())
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    items(SHOPPING_CATEGORIES) { aisle ->
                        FilterChip(selected = category == aisle, onClick = { category = if (category == aisle) null else aisle }, label = { Text(aisle) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, quantity, category) }, enabled = name.isNotBlank()) { Text("Save") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
