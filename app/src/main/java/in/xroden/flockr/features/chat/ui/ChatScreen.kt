/** The house group chat: messages by day, grouped by sender, with an input that follows the keyboard. */
package `in`.xroden.flockr.features.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import `in`.xroden.flockr.features.chat.model.Message
import `in`.xroden.flockr.features.chat.presentation.ChatEvent
import `in`.xroden.flockr.features.chat.presentation.ChatUiState
import `in`.xroden.flockr.features.chat.presentation.ChatViewModel
import `in`.xroden.flockr.features.house.model.HouseConfig
import `in`.xroden.flockr.features.house.model.nameOf
import `in`.xroden.flockr.features.house.model.timeZone
import `in`.xroden.flockr.features.house.model.today
import `in`.xroden.flockr.features.house.presentation.rememberHouseConfig
import `in`.xroden.flockr.ui.components.FlockrTopAppBar
import `in`.xroden.flockr.ui.components.MemberAvatar
import `in`.xroden.flockr.ui.components.states.EmptyState
import `in`.xroden.flockr.ui.components.states.ErrorState
import `in`.xroden.flockr.ui.theme.ComponentHeight
import `in`.xroden.flockr.ui.theme.IconSize
import `in`.xroden.flockr.ui.theme.Spacing
import `in`.xroden.flockr.utils.formatWithHouseConfig
import `in`.xroden.flockr.utils.rememberHaptics
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toLocalDateTime

private const val TIME_ALPHA = 0.7f

/** How far from the newest row the list may sit and still count as "at the bottom" when a message lands. */
private const val NEAR_BOTTOM_ROWS = 2

@Composable
fun ChatScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val haptics = rememberHaptics()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val config by rememberHouseConfig(houseId)
    val snackbarHostState = remember { SnackbarHostState() }
    var draft by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(houseId) { viewModel.load(houseId) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ChatEvent.SendFailed -> {
                    haptics.error()
                    if (draft.isBlank()) draft = event.content
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        topBar = { FlockrTopAppBar(title = "Chat", onNavigateBack = onNavigateBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (val current = state) {
                    ChatUiState.Loading -> LoadingIndicator(Modifier.align(Alignment.Center))
                    is ChatUiState.Error -> ErrorState(current.message, onRetry = { viewModel.load(houseId) })
                    is ChatUiState.Ready -> if (current.messages.isEmpty()) {
                        EmptyState(icon = Icons.Rounded.Forum, title = "No messages yet", subtitle = "Say hello. Everyone in the house sees what you send here.")
                    } else {
                        MessageList(current, config)
                    }
                }
            }
            Composer(
                text = draft,
                onTextChange = { draft = it },
                onSend = {
                    haptics.tap()
                    viewModel.send(houseId, draft)
                    draft = ""
                },
            )
        }
    }
}

private sealed interface ChatRow {
    val key: String

    data class Day(val label: String, override val key: String) : ChatRow

    /** [startsRun] is true for the first of consecutive messages from one sender on one day. */
    data class Bubble(val message: Message, val startsRun: Boolean) : ChatRow {
        override val key: String get() = message.id
    }
}

@Composable
private fun MessageList(state: ChatUiState.Ready, config: HouseConfig?) {
    val rows = remember(state.messages, config) { chatRows(state.messages, config) }
    val listState = rememberLazyListState()
    val newest = state.messages.last()

    LaunchedEffect(newest.id) {
        if (newest.userId == state.viewerId || listState.firstVisibleItemIndex <= NEAR_BOTTOM_ROWS) listState.animateScrollToItem(0)
    }

    LazyColumn(
        state = listState,
        reverseLayout = true,
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(rows, key = { it.key }, contentType = { it::class }) { row ->
            when (row) {
                is ChatRow.Day -> DaySeparator(row.label)
                is ChatRow.Bubble -> MessageRow(row, state, config)
            }
        }
    }
}

/** The rows newest first, for a list laid out from the bottom, with a day heading above each day's messages. */
private fun chatRows(messages: List<Message>, config: HouseConfig?): List<ChatRow> {
    val zone = config.timeZone()
    val today = config.today()
    val rows = mutableListOf<ChatRow>()
    var previous: Message? = null
    var previousDay: LocalDate? = null
    messages.forEach { message ->
        val day = message.createdAt.toLocalDateTime(zone).date
        val newDay = day != previousDay
        if (newDay) rows += ChatRow.Day(dayLabel(day, today, config), key = "day_$day")
        rows += ChatRow.Bubble(message, startsRun = newDay || previous?.userId != message.userId)
        previous = message
        previousDay = day
    }
    return rows.asReversed()
}

private fun dayLabel(day: LocalDate, today: LocalDate, config: HouseConfig?): String = when (day) {
    today -> "Today"
    today.minus(1, DateTimeUnit.DAY) -> "Yesterday"
    else -> day.formatWithHouseConfig(config)
}

@Composable
private fun DaySeparator(label: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = Spacing.md), contentAlignment = Alignment.Center) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Text(
                label,
                style = MaterialTheme.typography.labelMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
            )
        }
    }
}

@Composable
private fun MessageRow(row: ChatRow.Bubble, state: ChatUiState.Ready, config: HouseConfig?) {
    val message = row.message
    val isOwn = message.userId == state.viewerId
    val topPadding = if (row.startsRun) Spacing.sm else Spacing.xxs
    if (isOwn) {
        Row(Modifier.fillMaxWidth().padding(top = topPadding), horizontalArrangement = Arrangement.End) {
            Spacer(Modifier.width(Spacing.xxxxl))
            Bubble(message, config, isOwn = true, modifier = Modifier.weight(1f, fill = false))
        }
        return
    }
    val member = state.members[message.userId]
    val name = member?.displayName ?: message.senderName ?: state.members.nameOf(message.userId, state.viewerId)
    Row(Modifier.fillMaxWidth().padding(top = topPadding), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        if (row.startsRun) {
            MemberAvatar(name = name, avatarUrl = member?.avatarUrl, size = IconSize.lg)
        } else {
            Spacer(Modifier.width(IconSize.lg))
        }
        Column(Modifier.weight(1f, fill = false)) {
            if (row.startsRun) {
                Text(
                    name,
                    style = MaterialTheme.typography.labelMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = Spacing.sm, bottom = Spacing.xxs),
                )
            }
            Bubble(message, config, isOwn = false)
        }
        Spacer(Modifier.width(Spacing.xxxl))
    }
}

@Composable
private fun Bubble(message: Message, config: HouseConfig?, isOwn: Boolean, modifier: Modifier = Modifier) {
    val time = remember(message.createdAt, config) {
        message.createdAt.toLocalDateTime(config.timeZone()).time.toJavaLocalTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    }
    val container = if (isOwn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val content = if (isOwn) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Surface(
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = content,
        modifier = modifier.semantics(mergeDescendants = true) {},
    ) {
        Column(Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)) {
            Text(message.content, style = MaterialTheme.typography.bodyLarge)
            Row(
                modifier = Modifier.align(Alignment.End).padding(top = Spacing.xxs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(time, style = MaterialTheme.typography.labelSmall, color = content.copy(alpha = TIME_ALPHA))
                if (message.isPending) {
                    Icon(Icons.Rounded.Schedule, contentDescription = "Sending", tint = content.copy(alpha = TIME_ALPHA), modifier = Modifier.size(IconSize.xs))
                }
            }
        }
    }
}

@Composable
private fun Composer(text: String, onTextChange: (String) -> Unit, onSend: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.Bottom,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Message the house") },
                maxLines = 5,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.weight(1f),
            )
            FilledIconButton(onClick = onSend, enabled = text.isNotBlank(), modifier = Modifier.size(ComponentHeight.inputField)) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send")
            }
        }
    }
}
