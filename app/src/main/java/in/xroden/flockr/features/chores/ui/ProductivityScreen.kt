package `in`.xroden.flockr.features.chores.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.features.chores.domain.ChoreUiState
import `in`.xroden.flockr.features.chores.domain.ChoreViewModel
import `in`.xroden.flockr.features.house.domain.HouseManagementViewModel
import `in`.xroden.flockr.features.house.model.MemberWithProfile
import `in`.xroden.flockr.ui.components.inputs.MonthSelector
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductivityScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: ChoreViewModel = hiltViewModel(),
    houseManagementViewModel: HouseManagementViewModel = hiltViewModel()
) {
    var members by remember { mutableStateOf<List<MemberWithProfile>>(emptyList()) }
    val scope = rememberCoroutineScope()
    
    // Month selection state - using first day of month as LocalDate
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    var selectedMonth by remember { mutableStateOf(LocalDate(now.year, now.monthNumber, 1)) }
    
    LaunchedEffect(houseId) {
        viewModel.loadChores(houseId)
        scope.launch {
            members = houseManagementViewModel.getHouseMembers(houseId)
        }
    }
    val uiState by viewModel.uiState.collectAsState()
    
    // Create name -> avatarUrl map
    val avatarMap = remember(members) {
        members.associate { (it.fullName ?: it.email) to it.avatarUrl }
    }

    val chores = (uiState as? ChoreUiState.Success)?.allChores ?: emptyList()
    val completedChores = remember(chores) { chores.filter { it.isCompleted && it.completedByName != null && it.completedAt != null } }

    // Monthly stats - filter by selected month
    val monthlyStats = remember(completedChores, selectedMonth) {
        completedChores
            .filter { chore ->
                chore.completedAt?.let { completedAt ->
                    val completedDate = completedAt.toLocalDateTime(TimeZone.currentSystemDefault())
                    completedDate.year == selectedMonth.year && completedDate.monthNumber == selectedMonth.monthNumber
                } ?: false
            }
            .groupBy { it.completedByName!! }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
    }
    
    // Yearly stats - all chores completed this year
    val yearlyStats = remember(completedChores, selectedMonth) {
        completedChores
            .filter { chore ->
                chore.completedAt?.let { completedAt ->
                    val completedDate = completedAt.toLocalDateTime(TimeZone.currentSystemDefault())
                    completedDate.year == selectedMonth.year
                } ?: false
            }
            .groupBy { it.completedByName!! }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(3)
    }

    val monthlyTopPerformer = monthlyStats.firstOrNull()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Productivity",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Month Selector
            item(key = "month_selector") {
                MonthSelector(
                    selectedMonth = selectedMonth,
                    onMonthChange = { selectedMonth = it }
                )
            }
            
            // Monthly Hero Card
            item(key = "podium") {
                if (monthlyTopPerformer != null) {
                    TopPerformerCard(
                        name = monthlyTopPerformer.first, 
                        count = monthlyTopPerformer.second, 
                        avatarUrl = avatarMap[monthlyTopPerformer.first],
                        title = "${selectedMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} Champion"
                    )
                } else {
                    EmptyProductivityCard()
                }
            }

            // Monthly Leaderboard Header
            item(key = "leaderboard_header") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Monthly Leaderboard",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "${monthlyStats.size} people",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Monthly Leaderboard Items
            itemsIndexed(items = monthlyStats, key = { _, item -> "monthly_${item.first}" }) { index, (name, count) ->
                LeaderboardCard(index + 1, name, count, avatarMap[name])
            }

            if (monthlyStats.isEmpty()) {
                item(key = "empty_monthly") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.Assignment,
                                null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "No completed chores this month",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Yearly Top 3 Section
            if (yearlyStats.isNotEmpty()) {
                item(key = "yearly_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "${selectedMonth.year} Top Performers",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                
                item(key = "yearly_top3") {
                    YearlyTop3Card(yearlyStats, avatarMap)
                }
            }
        }
    }
}

@Composable
private fun TopPerformerCard(name: String, count: Int, avatarUrl: String? = null, title: String = "Top Performer") {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Trophy + Avatar
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                ) {}
                
                // Avatar or initial
                if (!avatarUrl.isNullOrBlank()) {
                    androidx.compose.foundation.Image(
                        painter = coil.compose.rememberAsyncImagePainter(
                            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                .data(avatarUrl)
                                .crossfade(true)
                                .build()
                        ),
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                name.take(1).uppercase(),
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-8).dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Name and stats
            Text(
                "🏆 $title",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Text(
                name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
            ) {
                Text(
                    "$count chores completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyProductivityCard() {
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
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Star,
                        null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Text(
                "No chores completed yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Complete chores to see productivity stats",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LeaderboardCard(rank: Int, name: String, count: Int, avatarUrl: String? = null) {
    val isTopThree = rank <= 3

    val medalColor = when (rank) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val containerColor = when (rank) {
        1 -> Color(0xFFFFD700).copy(alpha = 0.1f)
        2 -> Color(0xFFC0C0C0).copy(alpha = 0.1f)
        3 -> Color(0xFFCD7F32).copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (!isTopThree) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar or Rank Badge
            if (!avatarUrl.isNullOrBlank()) {
                Box(modifier = Modifier.size(40.dp)) {
                    androidx.compose.foundation.Image(
                        painter = coil.compose.rememberAsyncImagePainter(
                            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                .data(avatarUrl)
                                .crossfade(true)
                                .build()
                        ),
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    // Medal badge overlay for top 3
                    if (isTopThree) {
                        Surface(
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.BottomEnd),
                            shape = CircleShape,
                            color = medalColor
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "$rank",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = if (isTopThree) medalColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "#$rank",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = medalColor
                        )
                    }
                }
            }

            // Name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isTopThree) {
                    Text(
                        when (rank) {
                            1 -> "🥇 Gold"
                            2 -> "🥈 Silver"
                            else -> "🥉 Bronze"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = medalColor
                    )
                }
            }

            // Count Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Text(
                    "$count",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun YearlyTop3Card(yearlyStats: List<Pair<String, Int>>, avatarMap: Map<String, String?>) {
    val medalColors = listOf(
        Color(0xFFFFD700), // Gold
        Color(0xFFC0C0C0), // Silver
        Color(0xFFCD7F32)  // Bronze
    )
    val medalEmojis = listOf("🥇", "🥈", "🥉")
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            yearlyStats.forEachIndexed { index, (name, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Medal
                    Text(
                        medalEmojis.getOrElse(index) { "" },
                        style = MaterialTheme.typography.titleLarge
                    )
                    
                    // Avatar
                    val avatar = avatarMap[name]
                    if (!avatar.isNullOrBlank()) {
                        androidx.compose.foundation.Image(
                            painter = coil.compose.rememberAsyncImagePainter(
                                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                    .data(avatar)
                                    .crossfade(true)
                                    .build()
                            ),
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = medalColors.getOrElse(index) { MaterialTheme.colorScheme.surfaceVariant }.copy(alpha = 0.2f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    name.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = medalColors.getOrElse(index) { MaterialTheme.colorScheme.onSurfaceVariant }
                                )
                            }
                        }
                    }
                    
                    // Name
                    Text(
                        name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Count
                    Text(
                        "$count chores",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (index < yearlyStats.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}
