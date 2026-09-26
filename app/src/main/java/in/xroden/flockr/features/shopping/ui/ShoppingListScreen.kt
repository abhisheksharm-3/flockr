/** The house shopping list: how much is left to buy, the items by aisle, one line at the bottom to add more, and turning what was bought into an expense. */
package `in`.xroden.flockr.features.shopping.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.house.model.nameInSentence
import `in`.xroden.flockr.features.shopping.model.SHOPPING_CATEGORIES
import `in`.xroden.flockr.features.shopping.model.ShoppingItem
import `in`.xroden.flockr.features.shopping.presentation.ShoppingUiState
import `in`.xroden.flockr.features.shopping.presentation.ShoppingViewModel
import `in`.xroden.flockr.ui.components.SkeletonRows
import `in`.xroden.flockr.ui.components.AnimatedGlyph
import `in`.xroden.flockr.ui.components.GlyphMotion
import `in`.xroden.flockr.ui.components.HeroActions
import `in`.xroden.flockr.ui.components.HeroAmount
import `in`.xroden.flockr.ui.components.HeroButton
import `in`.xroden.flockr.ui.components.HeroCaption
import `in`.xroden.flockr.ui.components.HeroHeader
import `in`.xroden.flockr.ui.components.HeroLabel
import `in`.xroden.flockr.ui.components.HeroStatusBarScrim
import `in`.xroden.flockr.ui.components.ListRow
import `in`.xroden.flockr.ui.components.SectionTitle
import `in`.xroden.flockr.ui.components.buttons.FlockrPrimaryButton
import `in`.xroden.flockr.ui.components.inputs.FlockrTextField
import `in`.xroden.flockr.ui.components.isHeroScrolledAway
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
        bottomBar = { QuickAdd(onAdd = { name, category -> viewModel.add(houseId, name, null, category) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (val current = state) {
            ShoppingUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding)) { SkeletonRows() }
            is ShoppingUiState.Error -> Box(Modifier.fillMaxSize().padding(padding)) { ErrorState(current.message, onRetry = { viewModel.load(houseId) }) }
            is ShoppingUiState.Ready -> ShoppingContent(
                state = current,
                contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + Spacing.xxl),
                onToggle = viewModel::setPurchased,
                onEdit = { editing = it },
                onClearBought = { viewModel.clearBought(houseId) },
                onRecordExpense = { onRecordExpense(current.bought.joinToString { it.itemName }) },
            )
        }
    }

    editing?.let { item ->
        EditItemSheet(
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

@Composable
private fun ShoppingContent(
    state: ShoppingUiState.Ready,
    contentPadding: PaddingValues,
    onToggle: (ShoppingItem, Boolean) -> Unit,
    onEdit: (ShoppingItem) -> Unit,
    onClearBought: () -> Unit,
    onRecordExpense: () -> Unit,
) {
    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = contentPadding) {
            item(key = "hero") { ListHero(state, onRecordExpense) }
            state.toBuy.forEach { (aisle, items) ->
                item(key = "aisle_$aisle") { SectionTitle(aisle) }
                items(items, key = { it.id }) { item -> ItemRow(item, state, onToggle = { onToggle(item, it) }, onClick = { onEdit(item) }, modifier = Modifier.animateItem()) }
            }
            if (state.bought.isNotEmpty()) {
                item(key = "bought_heading") {
                    SectionTitle("Bought", action = { TextButton(onClick = onClearBought, shapes = ButtonDefaults.shapes()) { Text("Clear bought") } })
                }
                items(state.bought, key = { it.id }) { item -> ItemRow(item, state, onToggle = { onToggle(item, it) }, onClick = { onEdit(item) }, modifier = Modifier.animateItem()) }
            }
        }
        HeroStatusBarScrim(isHeroGone = listState.isHeroScrolledAway)
    }
}

/** The headline is how much is still to buy; once anything is bought, the sun action records it as an expense. */
@Composable
private fun ListHero(state: ShoppingUiState.Ready, onRecordExpense: () -> Unit) {
    val toBuy = state.toBuy.sumOf { (_, items) -> items.size }
    val bought = state.bought.size
    HeroHeader(title = "Shopping list") {
        when {
            toBuy == 0 && bought == 0 -> {
                HeroLabel("Nothing on the list")
                HeroCaption("Add what the house needs below. Everyone sees it as it changes.")
            }
            toBuy == 0 -> HeroLabel("Everything's bought")
            else -> {
                HeroLabel("Still to buy")
                HeroAmount(if (toBuy == 1) "1 thing" else "$toBuy things")
                HeroCaption(if (state.toBuy.size == 1) "All in ${state.toBuy.single().first}." else "Across ${state.toBuy.size} aisles.")
            }
        }
        if (bought > 0) {
            HeroCaption("${if (bought == 1) "1 thing" else "$bought things"} bought. Record ${if (bought == 1) "it" else "them"} as an expense so the cost gets split.")
            HeroActions { HeroButton("Record expense", onClick = onRecordExpense) }
        }
    }
}

/**
 * The add bar pinned above the keyboard, so adding is one thumb away however long the list is. The
 * aisle stays chosen for the next item, and the add glyph pops each time something goes on the list.
 */
@Composable
private fun QuickAdd(onAdd: (String, String?) -> Unit) {
    val haptics = rememberHaptics()
    var name by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var added by remember { mutableIntStateOf(0) }
    val submit = {
        if (name.isNotBlank()) {
            haptics.tap()
            onAdd(name, category)
            name = ""
            added++
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .navigationBarsPadding()
            .imePadding()
            .padding(vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), contentPadding = PaddingValues(horizontal = Spacing.lg)) {
            items(SHOPPING_CATEGORIES) { aisle ->
                FilterChip(selected = category == aisle, onClick = { haptics.select(); category = if (category == aisle) null else aisle }, label = { Text(aisle) })
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(category?.let { "Add to $it" } ?: "Add an item") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.weight(1f),
            )
            FilledIconButton(onClick = submit, enabled = name.isNotBlank(), shapes = IconButtonDefaults.shapes()) {
                AnimatedGlyph(Icons.Rounded.Add, trigger = added, motion = GlyphMotion.POP, contentDescription = "Add")
            }
        }
    }
}

/** An item to tick off; once bought it dims, says who bought it, and its check pops. */
@Composable
private fun ItemRow(item: ShoppingItem, state: ShoppingUiState.Ready, onToggle: (Boolean) -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val who = if (item.isPurchased) {
        item.purchasedBy?.let { "Bought by ${state.members.nameInSentence(it, state.viewerId)}" }
    } else {
        "Added by ${state.members.nameInSentence(item.addedBy, state.viewerId)}"
    }
    ListRow(
        headline = item.itemName,
        supporting = listOfNotNull(item.quantity?.takeIf { it.isNotBlank() }, who).joinToString(" · ").ifEmpty { null },
        leading = { BoughtCheck(item.isPurchased, onToggle, itemName = item.itemName) },
        headlineColor = if (item.isPurchased) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        onClick = onClick,
        modifier = modifier,
    )
}

/**
 * A round check that pops when the item turns bought. The pop follows [bought] rather than the tap,
 * so it lands when the list confirms the change, and it only plays on the way to bought.
 */
@Composable
private fun BoughtCheck(bought: Boolean, onToggle: (Boolean) -> Unit, itemName: String) {
    val haptics = rememberHaptics()
    var pops by remember { mutableIntStateOf(0) }
    var wasBought by remember { mutableStateOf(bought) }
    LaunchedEffect(bought) {
        if (bought && !wasBought) pops++
        wasBought = bought
    }
    IconToggleButton(
        checked = bought,
        onCheckedChange = { haptics.toggle(it); onToggle(it) },
        shapes = IconButtonDefaults.toggleableShapes(),
    ) {
        AnimatedGlyph(
            if (bought) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            trigger = pops,
            motion = GlyphMotion.POP,
            contentDescription = if (bought) "$itemName bought" else "Mark $itemName bought",
            tint = if (bought) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Changing an item in a sheet: its name, how much, and the aisle as a row of chips. */
@Composable
private fun EditItemSheet(item: ShoppingItem, onSave: (String, String?, String?) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val haptics = rememberHaptics()
    var name by rememberSaveable { mutableStateOf(item.itemName) }
    var quantity by rememberSaveable { mutableStateOf(item.quantity.orEmpty()) }
    var category by rememberSaveable { mutableStateOf(item.category) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text("Edit item", style = MaterialTheme.typography.titleLargeEmphasized)
            FlockrTextField(value = name, onValueChange = { name = it }, label = "Item", modifier = Modifier.fillMaxWidth())
            FlockrTextField(value = quantity, onValueChange = { quantity = it }, label = "How much", placeholder = "2 kg, a dozen", modifier = Modifier.fillMaxWidth())
            Text("Aisle", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                SHOPPING_CATEGORIES.forEach { aisle ->
                    FilterChip(selected = category == aisle, onClick = { haptics.select(); category = if (category == aisle) null else aisle }, label = { Text(aisle) })
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDelete, shapes = ButtonDefaults.shapes()) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                FlockrPrimaryButton(text = "Save", onClick = { onSave(name, quantity, category) }, enabled = name.isNotBlank(), modifier = Modifier.weight(1f))
            }
        }
    }
}
