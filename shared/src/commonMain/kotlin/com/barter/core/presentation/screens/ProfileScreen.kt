package com.barter.core.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barter.core.di.AppDI
import com.barter.core.domain.model.PredefinedCategories
import com.barter.core.domain.model.RENEWAL_COST_MDL
import com.barter.core.presentation.theme.*
import com.barter.core.presentation.vm.AuthViewModel
import com.barter.core.presentation.vm.ProfileStatsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onNavigateToMyListings: () -> Unit,
    onNavigateToEditInterests: () -> Unit,
    onLogout: () -> Unit,
) {
    val authVm: AuthViewModel = remember { AppDI.get() }
    val user by authVm.currentUser.collectAsState()

    val statsVm: ProfileStatsViewModel = remember { AppDI.get() }
    val stats by statsVm.stats.collectAsState()

    var showTopUpDialog by remember { mutableStateOf(false) }
    var topUpAmount by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { statsVm.load() }

    // Top-up dialog
    if (showTopUpDialog) {
        AlertDialog(
            onDismissRequest = {
                showTopUpDialog = false
                topUpAmount = ""
            },
            title = { Text("Top Up Balance") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Add funds to your account to renew listings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = topUpAmount,
                        onValueChange = { topUpAmount = it.filter { c -> c.isDigit() || c == '.' } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Amount (MDL)") },
                        placeholder = { Text("e.g. 50") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BarterGreen,
                            focusedLabelColor = BarterGreen,
                            cursorColor = BarterGreen,
                        ),
                    )
                    // Quick amount chips
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10, 50, 100).forEach { amount ->
                            FilterChip(
                                selected = topUpAmount == amount.toString(),
                                onClick = { topUpAmount = amount.toString() },
                                label = { Text("$amount MDL") },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BarterGreen.copy(alpha = 0.15f),
                                    selectedLabelColor = BarterGreen,
                                ),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = topUpAmount.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            authVm.topUpBalance(amount)
                            showTopUpDialog = false
                            topUpAmount = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BarterGreen),
                ) {
                    Text("Top Up")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTopUpDialog = false
                    topUpAmount = ""
                }) {
                    Text("Cancel")
                }
            },
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Gradient header background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BarterTeal.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.background,
                        )
                    )
                )
                .padding(horizontal = 24.dp)
                .padding(top = 32.dp, bottom = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Avatar with gradient ring
        Box(
            modifier = Modifier.size(104.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Gradient ring
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .border(
                        width = 3.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(BarterTeal, BarterAmber, BarterTealLight),
                        ),
                        shape = CircleShape,
                    ),
            )
            Surface(
                modifier = Modifier.size(92.dp),
                shape = CircleShape,
                color = BarterTeal.copy(alpha = 0.15f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        user.displayName.firstOrNull()?.toString() ?: "?",
                        color = BarterTeal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 40.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            user.displayName,
            style = MaterialTheme.typography.headlineMedium,
        )

        if (user.location.isNotBlank()) {
            Text(
                user.location,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (user.email.isNotBlank()) {
            Text(
                user.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(8.dp))

        // Rating
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = BarterAmber.copy(alpha = 0.12f),
        ) {
            Text(
                "\u2605 ${user.rating}",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = BarterAmber,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

            } // end Column inside gradient Box
        } // end gradient Box

        // Content below gradient — with horizontal padding
        Column(
            Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

        // Balance card
        Spacer(Modifier.height(16.dp))
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BarterGreen.copy(alpha = 0.08f)),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        null,
                        modifier = Modifier.size(28.dp),
                        tint = BarterGreen,
                    )
                    Column {
                        Text(
                            "Balance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "${"%.0f".format(user.balance)} MDL",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = BarterGreen,
                        )
                    }
                }
                FilledTonalButton(
                    onClick = { showTopUpDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = BarterGreen.copy(alpha = 0.15f),
                        contentColor = BarterGreen,
                    ),
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Top Up")
                }
            }
        }
        Text(
            "Renewal cost: ${"%.0f".format(RENEWAL_COST_MDL)} MDL per listing",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Stats cards with icons
        Spacer(Modifier.height(20.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                icon = Icons.Default.Inventory2,
                value = stats.activeListingsCount.toString(),
                label = "Listings",
                color = BarterTeal,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Default.Handshake,
                value = stats.completedDealsCount.toString(),
                label = "Deals",
                color = BarterAmber,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Default.People,
                value = stats.matchesCount.toString(),
                label = "Matches",
                color = BarterGreen,
                modifier = Modifier.weight(1f),
            )
        }

        // Interests
        if (user.interests.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text(
                "My Interests",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                user.interests.forEach { catId ->
                    val cat = PredefinedCategories.all.firstOrNull { it.id == catId }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = BarterTeal.copy(alpha = 0.1f),
                    ) {
                        Text(
                            "${cat?.emoji ?: ""} ${cat?.label ?: catId}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = BarterTeal,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Spacer(Modifier.height(16.dp))

        // Action cards
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                ProfileMenuItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, null, tint = BarterTeal) },
                    label = "My Listings",
                    onClick = onNavigateToMyListings,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                ProfileMenuItem(
                    icon = { Icon(Icons.Default.Favorite, null, tint = BarterAmber) },
                    label = "My Interests",
                    onClick = onNavigateToEditInterests,
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                ProfileMenuItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = BarterCoral) },
                    label = "Logout",
                    onClick = {
                        authVm.logout()
                        onLogout()
                    },
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        } // end inner padded Column
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.06f)),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Colored circle behind icon
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = color.copy(alpha = 0.12f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, modifier = Modifier.size(20.dp), tint = color)
                }
            }
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        icon()
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
