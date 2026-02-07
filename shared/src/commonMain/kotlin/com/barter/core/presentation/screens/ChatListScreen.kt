package com.barter.core.presentation.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barter.core.di.AppDI
import com.barter.core.domain.model.EnrichedMatch
import com.barter.core.presentation.theme.*
import com.barter.core.presentation.vm.ChatListViewModel
import com.barter.core.util.currentTimeMillis

@Composable
fun ChatListScreen(
    onOpenChat: (matchId: String) -> Unit,
) {
    val vm: ChatListViewModel = remember { AppDI.get() }
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.load() }

    Column(Modifier.fillMaxSize()) {
        // Header
        Text(
            "Messages",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )

        if (state.loading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = BarterTeal,
            )
        }

        state.error?.let { err ->
            Text(
                err,
                color = BarterCoral,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        if (!state.loading && state.conversations.isEmpty()) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "\uD83D\uDCAC",
                        fontSize = 48.sp,
                    )
                    Text(
                        "No conversations yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "Start swiping to find matches!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(state.conversations.size) { index ->
                    ConversationRow(
                        enrichedMatch = state.conversations[index],
                        onClick = { onOpenChat(state.conversations[index].match.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(
    enrichedMatch: EnrichedMatch,
    onClick: () -> Unit,
) {
    val match = enrichedMatch.match
    val partner = match.userB // the other person
    val lastMsg = enrichedMatch.lastMessage
    val unread = enrichedMatch.unreadCount

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unread > 0) {
                BarterTeal.copy(alpha = 0.04f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(if (unread > 0) 2.dp else 0.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Avatar with gradient ring
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
                            partner.displayName.firstOrNull()?.toString() ?: "?",
                            color = BarterTeal,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                        )
                    }
                }
            }

            // Name + last message
            Column(Modifier.weight(1f)) {
                Text(
                    partner.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (unread > 0) FontWeight.Bold else FontWeight.Medium,
                )
                if (lastMsg != null) {
                    Text(
                        lastMsg.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (unread > 0) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (unread > 0) FontWeight.Medium else FontWeight.Normal,
                    )
                } else {
                    Text(
                        "Tap to say hello!",
                        style = MaterialTheme.typography.bodySmall,
                        color = BarterTeal,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            // Time + unread badge
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (lastMsg != null) {
                    Text(
                        timeAgoShort(lastMsg.timestampMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (unread > 0) {
                    Badge(containerColor = BarterTeal) {
                        Text("$unread")
                    }
                }
            }
        }
    }
}

private fun timeAgoShort(timestampMs: Long): String {
    val diff = currentTimeMillis() - timestampMs
    val minutes = diff / 60_000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        days < 7 -> "${days}d"
        else -> "${days / 7}w"
    }
}
