/** What has happened in a house, newest first, each event read as a sentence. */
package `in`.xroden.flockr.features.house.ui.settings

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.house.model.HouseAuditLog
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.features.house.model.currency
import `in`.xroden.flockr.features.house.model.nameInSentence
import `in`.xroden.flockr.features.house.model.nameOf
import `in`.xroden.flockr.features.house.model.timeZone
import `in`.xroden.flockr.features.house.presentation.ActivityUiState
import `in`.xroden.flockr.features.house.presentation.ActivityViewModel
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.SkeletonRows
import `in`.xroden.flockr.ui.components.BadgeTone
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.IconBadge
import `in`.xroden.flockr.ui.components.ListRow
import `in`.xroden.flockr.ui.components.SectionTitle
import `in`.xroden.flockr.ui.components.states.EmptyState
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.formatMoney
import `in`.xroden.flockr.utils.relativeDayLabel
import kotlin.time.Clock
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonPrimitive

@Composable
fun HouseAuditLogScreen(houseId: String, onNavigateBack: () -> Unit, viewModel: ActivityViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val config by rememberHouseConfig(houseId)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(houseId) { viewModel.load(houseId) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { FlockrTopAppBar(title = "Activity", scrollBehavior = scrollBehavior) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val current = state) {
                ActivityUiState.Loading -> SkeletonRows()
                is ActivityUiState.Error -> ErrorState(current.message, onRetry = { viewModel.load(houseId) })
                is ActivityUiState.Ready -> if (current.events.isEmpty()) {
                    EmptyState(icon = Icons.Rounded.History, title = "Nothing yet", subtitle = "Expenses, payments, chores and members coming and going show up here.")
                } else {
                    EventList(current, config)
                }
            }
        }
    }
}

/** Events under a heading for the day they happened on, in the house's time zone, newest first. */
@Composable
private fun EventList(state: ActivityUiState.Ready, config: HouseConfig?) {
    val days = remember(state.events, config) {
        val zone = config.timeZone()
        state.events.groupBy { it.createdAt.toLocalDateTime(zone).date }
    }
    val now = Clock.System.now().toEpochMilliseconds()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = Spacing.xxl)) {
        days.forEach { (day, events) ->
            item(key = "day_$day") { SectionTitle(day.relativeDayLabel(config)) }
            items(events, key = { it.id }) { event ->
                ListRow(
                    headline = describe(event, state.members, state.viewerId, config.currency()),
                    supporting = DateUtils.getRelativeTimeSpanString(event.createdAt.toEpochMilliseconds(), now, DateUtils.MINUTE_IN_MILLIS).toString(),
                    leading = { IconBadge(eventIcon(event.action), eventTone(event.action)) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
        item(key = "inset") { Spacer(Modifier.navigationBarsPadding()) }
    }
}

private fun eventTone(action: String): BadgeTone = when (action) {
    "member_joined", "role_changed" -> BadgeTone.SUN
    "member_left", "member_removed", "expense_deleted", "payment_deleted", "chore_deleted" -> BadgeTone.ROSE
    "payment_recorded" -> BadgeTone.SLATE
    "chore_added", "chore_completed" -> BadgeTone.JADE
    else -> BadgeTone.COBALT
}

private fun eventIcon(action: String): ImageVector = when (action) {
    "member_joined" -> Icons.Rounded.PersonAdd
    "member_left", "member_removed" -> Icons.Rounded.PersonRemove
    "role_changed" -> Icons.Rounded.AdminPanelSettings
    "payment_recorded", "payment_deleted" -> Icons.Rounded.Handshake
    "chore_added", "chore_completed", "chore_deleted" -> Icons.Rounded.CleaningServices
    else -> Icons.AutoMirrored.Rounded.ReceiptLong
}

/** "Riya added Groceries · ₹100", "Karan left the house", "You finished Bins". */
private fun describe(event: HouseAuditLog, members: Map<String, MemberWithProfile>, viewerId: String, currencyCode: String): String {
    val actor = event.userId?.let { members.nameOf(it, viewerId) } ?: "Flockr"
    val subject = event.targetUserId?.let { members.nameInSentence(it, viewerId) } ?: "someone"
    val name = event.detail("name")
    val amount = event.detail("amount")?.toBigDecimalOrNull()?.formatMoney(currencyCode)
    val priced = listOfNotNull(name, amount).joinToString(" · ")
    return when (event.action) {
        "member_joined" -> "${members.nameOf(event.targetUserId, viewerId)} joined the house"
        "member_left" -> "${members.nameOf(event.targetUserId, viewerId)} left the house"
        "member_removed" -> "$actor removed $subject"
        "role_changed" -> "$actor made $subject ${event.detail("role")?.lowercase() ?: "a member"}"
        "expense_added" -> "$actor added $priced"
        "expense_updated" -> "$actor changed $priced"
        "expense_deleted" -> "$actor deleted $priced"
        "payment_recorded" -> "$actor recorded a payment of ${amount ?: "money"}"
        "payment_deleted" -> "$actor deleted a payment of ${amount ?: "money"}"
        "chore_added" -> "$actor added the chore ${name ?: ""}".trim()
        "chore_completed" -> "$actor finished ${name ?: "a chore"}"
        "chore_deleted" -> "$actor deleted the chore ${name ?: ""}".trim()
        else -> "$actor: ${event.action.replace('_', ' ')}"
    }
}

private fun HouseAuditLog.detail(key: String): String? = (details[key] as? JsonPrimitive)?.content
