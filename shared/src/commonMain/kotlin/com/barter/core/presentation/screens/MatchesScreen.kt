package com.barter.core.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barter.core.di.AppDI
import com.barter.core.domain.model.EnrichedMatch
import com.barter.core.presentation.theme.*
import com.barter.core.presentation.vm.MatchesViewModel

@Composable
fun MatchesScreen(
    onOpenChat: (matchId: String) -> Unit,
    onOpenDeal: (matchId: String) -> Unit,
) {
    val vm: MatchesViewModel = remember { AppDI.get() }
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.load() }

    Column(Modifier.fillMaxSize()) {
        // Header with gradient text
        Text(
            "Matches",
            style = MaterialTheme.typography.headlineLarge.copy(
                brush = Brush.linearGradient(
                    colors = listOf(BarterTeal, BarterAmber),
                )
            ),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )

        if (state.loading) {
            LinearProgressIndicator(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                color = BarterTeal,
            )
        }

        state.error?.let {
            Text(
                "Error: $it",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        if (state.matches.isEmpty() && !state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("\uD83D\uDCAB", fontSize = 48.sp)
                    Text("No matches yet", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Like some listings to find\nyour barter partners!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.matches.size) { index ->
                    val enriched = state.matches[index]
                    MatchCard(
                        enrichedMatch = enriched,
                        onChat = { onOpenChat(enriched.match.id) },
                        onDeal = { onOpenDeal(enriched.match.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MatchCard(
    enrichedMatch: EnrichedMatch,
    onChat: () -> Unit,
    onDeal: () -> Unit,
) {
    val match = enrichedMatch.match

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(3.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Gradient accent strip (left edge)
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(BarterTeal, BarterAmber),
                        )
                    )
            )
        Column(Modifier.weight(1f).padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Avatar with colored ring
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .border(
                                width = 2.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(BarterTeal, BarterAmber),
                                ),
                                shape = CircleShape,
                            ),
                    )
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = BarterTeal.copy(alpha = 0.15f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                match.userB.displayName.first().toString(),
                                color = BarterTeal,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                            )
                        }
                    }
                }

                // Info
                Column(Modifier.weight(1f)) {
                    Text(
                        match.userB.displayName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "${match.userB.location} \u2022 \u2605 ${match.userB.rating}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Unread badge
                if (enrichedMatch.unreadCount > 0) {
                    Badge(
                        containerColor = BarterTeal,
                    ) {
                        Text("${enrichedMatch.unreadCount}")
                    }
                }

                // Actions
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onChat,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BarterTeal),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        Text("Chat", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = onDeal,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        Text("Deal", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Last message preview
            enrichedMatch.lastMessage?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(
                    msg.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 72.dp),
                )
            }
        } // end Column
        } // end Row
    }
}
