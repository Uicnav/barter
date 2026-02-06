package com.barter.core.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barter.core.di.AppDI
import com.barter.core.domain.model.Notification
import com.barter.core.domain.model.NotificationType
import com.barter.core.presentation.theme.*
import com.barter.core.presentation.vm.NotificationsViewModel

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
) {
    val vm: NotificationsViewModel = remember { AppDI.get() }
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.load() }

    Column(Modifier.fillMaxSize()) {
        // Top bar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text(
                "Notifications",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
        }

        if (state.loading) {
            LinearProgressIndicator(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                color = BarterTeal,
            )
        }

        if (state.notifications.isEmpty() && !state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("\uD83D\uDD14", fontSize = 48.sp)
                    Text("No notifications", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "You're all caught up!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.notifications.size) { index ->
                    val notification = state.notifications[index]
                    NotificationCard(
                        notification = notification,
                        onClick = {
                            if (!notification.isRead) {
                                vm.markAsRead(notification.id)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: Notification,
    onClick: () -> Unit,
) {
    val (icon, iconColor) = when (notification.type) {
        NotificationType.LIKE_RECEIVED -> Icons.Default.Favorite to BarterCoral
        NotificationType.MATCH_CREATED -> Icons.Default.People to BarterTeal
    }

    val containerColor = if (notification.isRead) {
        MaterialTheme.colorScheme.surface
    } else {
        BarterTeal.copy(alpha = 0.06f)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(if (notification.isRead) 1.dp else 2.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon circle
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.12f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = iconColor,
                    )
                }
            }

            Column(Modifier.weight(1f)) {
                Text(
                    notification.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                )
                Text(
                    notification.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Unread dot
            if (!notification.isRead) {
                Surface(
                    modifier = Modifier.size(8.dp),
                    shape = CircleShape,
                    color = BarterTeal,
                ) {}
            }
        }
    }
}
