/** A house's home: its picture and people, where the viewer stands, and a door to each part of the house. */
package `in`.xroden.flockr.features.house.ui.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import `in`.xroden.flockr.ui.components.balanceColor
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.features.house.model.currency
import `in`.xroden.flockr.features.house.presentation.HouseDetailUiState
import `in`.xroden.flockr.features.house.presentation.HouseDetailsViewModel
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.MemberAvatar
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.ComponentHeight
import `in`.xroden.flockr.ui.theme.IconSize
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.formatMoney
import java.math.BigDecimal

private const val AVATARS_SHOWN = 5

private data class Destination(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
fun HouseDetailsScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToShopping: () -> Unit,
    onNavigateToChores: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToManageMembers: () -> Unit,
    onNavigateToHouseSettings: () -> Unit,
    viewModel: HouseDetailsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val config by rememberHouseConfig(houseId)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(houseId) { viewModel.load(houseId) }

    val destinations = listOf(
        Destination("Expenses", "Spending and who owes whom", Icons.Rounded.AccountBalanceWallet, onNavigateToExpenses),
        Destination("Shopping", "The shared list", Icons.Rounded.ShoppingCart, onNavigateToShopping),
        Destination("Chores", "Whose turn it is", Icons.Rounded.CleaningServices, onNavigateToChores),
        Destination("Chat", "Talk to the house", Icons.Rounded.Forum, onNavigateToChat),
        Destination("Documents", "Leases, bills and files", Icons.Rounded.Description, onNavigateToDocuments),
        Destination("Members", "People and invites", Icons.Rounded.Group, onNavigateToManageMembers),
        Destination("Settings", "Name, money and dates", Icons.Rounded.Settings, onNavigateToHouseSettings),
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            FlockrTopAppBar(
                title = (state as? HouseDetailUiState.Ready)?.house?.name ?: "House",
                onNavigateBack = onNavigateBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val current = state) {
                HouseDetailUiState.Loading -> LoadingIndicator(Modifier.align(Alignment.Center))
                is HouseDetailUiState.Error -> ErrorState(current.message, onRetry = { viewModel.load(houseId) })
                is HouseDetailUiState.Ready -> LazyColumn(
                    contentPadding = PaddingValues(start = Spacing.lg, end = Spacing.lg, top = Spacing.sm, bottom = Spacing.xxxl),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                ) {
                    item(key = "header") { HouseHeader(current, onOpenMembers = onNavigateToManageMembers) }
                    current.viewerNet?.let { net ->
                        item(key = "standing") { StandingCard(net, config.currency(), onClick = onNavigateToExpenses) }
                    }
                    item(key = "destinations_title") {
                        Text("Around the house", style = MaterialTheme.typography.titleMediumEmphasized, modifier = Modifier.padding(top = Spacing.sm))
                    }
                    items(destinations.chunked(2), key = { row -> row.first().title }) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxWidth()) {
                            row.forEachIndexed { index, destination ->
                                DestinationTile(destination, tint = tileTint(destinations.indexOf(destination)), modifier = Modifier.weight(1f))
                                if (row.size == 1 && index == 0) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The house picture, or a tinted placeholder when it has none, with the address and who lives there. */
@Composable
private fun HouseHeader(state: HouseDetailUiState.Ready, onOpenMembers: () -> Unit) {
    val house = state.house
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Box(Modifier.fillMaxWidth().height(ComponentHeight.cardLarge)) {
            if (house.headerImageUrl != null) {
                AsyncImage(
                    model = house.headerImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Home,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(IconSize.xxl),
                        )
                    }
                }
            }
        }
        Column(Modifier.padding(Spacing.xl), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(house.name, style = MaterialTheme.typography.headlineSmallEmphasized, maxLines = 2, overflow = TextOverflow.Ellipsis)
                house.address?.takeIf { it.isNotBlank() }?.let { address ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Icon(
                            Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(IconSize.sm),
                        )
                        Text(address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            MembersRow(state.activeMembers, onClick = onOpenMembers)
        }
    }
}

@Composable
private fun MembersRow(members: List<MemberWithProfile>, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).clickable(onClick = onClick).padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(-Spacing.sm)) {
            members.take(AVATARS_SHOWN).forEach { member ->
                MemberAvatar(name = member.displayName, avatarUrl = member.avatarUrl)
            }
        }
        Text(
            when (members.size) {
                1 -> "Just you so far"
                else -> "${members.size} people live here"
            },
            style = MaterialTheme.typography.bodyMediumEmphasized,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The viewer's overall balance in the house; opening it goes to the expenses. */
@Composable
private fun StandingCard(net: BigDecimal, currencyCode: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(Modifier.padding(Spacing.xl), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                when (net.signum()) {
                    0 -> Text("You're all settled up", style = MaterialTheme.typography.titleLargeEmphasized)
                    else -> {
                        Text(
                            if (net.signum() > 0) "You are owed" else "You owe",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(net.abs().formatMoney(currencyCode), style = MaterialTheme.typography.displaySmallEmphasized, color = balanceColor(net))
                    }
                }
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Open expenses", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DestinationTile(destination: Destination, tint: Color, modifier: Modifier = Modifier) {
    Card(
        onClick = destination.onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.largeIncreased,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.fillMaxWidth().padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Surface(shape = MaterialShapes.Cookie4Sided.toShape(), color = tint) {
                Icon(destination.icon, contentDescription = null, modifier = Modifier.padding(Spacing.sm).size(IconSize.md))
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(destination.title, style = MaterialTheme.typography.titleMediumEmphasized)
                Text(destination.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun tileTint(index: Int): Color = when (index % 3) {
    0 -> MaterialTheme.colorScheme.primaryContainer
    1 -> MaterialTheme.colorScheme.secondaryContainer
    else -> MaterialTheme.colorScheme.tertiaryContainer
}
