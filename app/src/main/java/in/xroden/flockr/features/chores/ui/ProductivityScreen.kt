package `in`.xroden.flockr.features.chores.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import `in`.xroden.flockr.features.chores.domain.ChoreUiState
import `in`.xroden.flockr.features.chores.domain.ChoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductivityScreen(
    houseId: String,
    onNavigateBack: () -> Unit,
    viewModel: ChoreViewModel = hiltViewModel()
) {
    LaunchedEffect(houseId) {
        viewModel.loadChores(houseId)
    }
    val uiState by viewModel.uiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val chores = (uiState as? ChoreUiState.Success)?.allChores ?: emptyList()
    // Use stable inputs for remember
    val completedChores = remember(chores) { chores.filter { it.isCompleted && it.completedByName != null } }
    
    val stats = remember(completedChores) {
        completedChores
            .groupBy { it.completedByName!! }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
    }
    
    val topPerformer = stats.firstOrNull()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Productivity") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero / Podium
            item(key = "podium") {
                if (topPerformer != null) {
                    PodiumCard(topPerformer.first, topPerformer.second)
                } else {
                    EmptyStateCard()
                }
            }
            
            item(key = "leaderboard_header") {
                Text(
                    "Leaderboard",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                )
            }

            // Stable list of items
            itemsIndexed(items = stats, key = { _, item -> item.first }) { index, (name, count) ->
                RankItem(index + 1, name, count)
            }
            
            if (stats.isEmpty() && topPerformer == null) {
                item(key = "empty_message") {
                    Text(
                        "No stats available yet. Complete some chores!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PodiumCard(name: String, count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                 Box(Modifier.size(110.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)))
                 Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(80.dp)) {
                     Box(contentAlignment = Alignment.Center) {
                         Text(
                             name.take(1).uppercase(),
                             style = MaterialTheme.typography.displayMedium,
                             color = MaterialTheme.colorScheme.onPrimaryContainer,
                             fontWeight = FontWeight.Bold
                         )
                     }
                 }
                 Icon(
                     Icons.Default.EmojiEvents,
                     null,
                     tint = Color(0xFFFFD700),
                     modifier = Modifier
                         .size(40.dp)
                         .align(Alignment.TopEnd)
                         .offset(x = 8.dp, y = (-8).dp)
                         .background(MaterialTheme.colorScheme.surface, CircleShape)
                         .padding(4.dp)
                 )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Keep it up, $name!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Text("$count chores completed", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun EmptyStateCard() {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Star, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(8.dp))
            Text("No chores completed yet", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun RankItem(rank: Int, name: String, count: Int) {
    val containerColor = when (rank) {
        1 -> Color(0xFFFFD700).copy(alpha = 0.15f)
        2 -> Color(0xFFC0C0C0).copy(alpha = 0.15f)
        3 -> Color(0xFFCD7F32).copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    
    val rankColor = when (rank) {
         1 -> Color(0xFFD4AF37)
         2 -> Color(0xFF757575) 
         3 -> Color(0xFFA0522D)
         else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(colors = CardDefaults.cardColors(containerColor = containerColor), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("#$rank", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = rankColor, modifier = Modifier.width(64.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Surface(color = MaterialTheme.colorScheme.surface, shape = CircleShape, modifier = Modifier.height(32.dp)) {
                 Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                    Text("$count", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                 }
            }
        }
    }
}
