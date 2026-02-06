package com.barter.core.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.barter.core.di.AppDI
import com.barter.core.domain.model.ListingKind
import com.barter.core.domain.model.status
import com.barter.core.presentation.components.AvailabilityBadge
import com.barter.core.presentation.components.ListingStatusBadge
import com.barter.core.presentation.components.MatchCelebration
import com.barter.core.presentation.components.ValidityIndicator
import com.barter.core.presentation.theme.*
import com.barter.core.presentation.vm.ListingDetailViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ListingDetailScreen(
    listingId: String,
    onBack: () -> Unit,
    onOpenChat: (matchId: String) -> Unit = {},
) {
    val vm: ListingDetailViewModel = remember { AppDI.get() }
    val state by vm.state.collectAsState()

    LaunchedEffect(listingId) { vm.load(listingId) }

    Box(Modifier.fillMaxSize()) {
        // Main content
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "DETAILS",
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = NeonCyan)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = FutureDark.copy(alpha = 0.85f),
                        titleContentColor = NeonCyan,
                    ),
                )
            },
            bottomBar = {
                // Like / Pass action bar
                val listing = state.listing
                if (listing != null && !state.isPassed && !state.isLiked) {
                    Surface(
                        color = FutureSurface.copy(alpha = 0.95f),
                        shadowElevation = 16.dp,
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            // Pass button
                            OutlinedButton(
                                onClick = { vm.passListing() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                    brush = Brush.linearGradient(listOf(FutureBorder, Color(0xFF4A5568))),
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF94A3B8),
                                ),
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "PASS",
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                )
                            }

                            // Like button
                            Button(
                                onClick = { vm.likeListing() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCyan,
                                    contentColor = FutureDark,
                                ),
                                enabled = !state.isLiking,
                            ) {
                                if (state.isLiking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = FutureDark,
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.FavoriteBorder,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "LIKE",
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                )
                            }
                        }
                    }
                }

                // Result feedback bar
                if (state.isLiked && state.lastMatchId == null) {
                    Surface(
                        color = NeonCyan.copy(alpha = 0.15f),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Liked! Waiting for a match...",
                                color = NeonCyan,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                if (state.isPassed) {
                    Surface(
                        color = FutureBorder.copy(alpha = 0.3f),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                "Passed",
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            },
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(FutureDark, FutureSurface),
                        )
                    )
            ) {
                when {
                    state.loading -> {
                        Box(
                            Modifier.fillMaxSize().padding(padding),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = NeonCyan)
                        }
                    }
                    state.listing == null -> {
                        Box(
                            Modifier.fillMaxSize().padding(padding),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                state.error ?: "Listing not found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF94A3B8),
                            )
                        }
                    }
                    else -> {
                        val listing = state.listing!!
                        val gradientColors = when (listing.kind) {
                            ListingKind.GOODS -> listOf(NeonCyan, Color(0xFF0077B6))
                            ListingKind.SERVICES -> listOf(NeonPurple, Color(0xFFE040FB))
                            ListingKind.BOTH -> listOf(NeonTeal, NeonCyan)
                        }
                        val kindEmoji = when (listing.kind) {
                            ListingKind.GOODS -> "\uD83D\uDCE6"
                            ListingKind.SERVICES -> "\u2728"
                            ListingKind.BOTH -> "\uD83D\uDD04"
                        }

                        val hasPhoto = listing.imageUrl.isNotBlank()

                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            // Hero image / gradient header
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                            ) {
                                if (hasPhoto) {
                                    AsyncImage(
                                        model = listing.imageUrl,
                                        contentDescription = listing.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    colors = gradientColors,
                                                    start = Offset.Zero,
                                                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                                                )
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(kindEmoji, fontSize = 80.sp)
                                    }
                                }

                                // Gradient overlay at bottom of image
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .align(Alignment.BottomCenter)
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(Color.Transparent, FutureDark)
                                            )
                                        )
                                )

                                // Value badge — neon style
                                listing.estimatedValue?.let { value ->
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(16.dp),
                                        shape = RoundedCornerShape(20.dp),
                                        color = NeonTeal.copy(alpha = 0.2f),
                                    ) {
                                        Text(
                                            "EUR ${value.toInt()}",
                                            modifier = Modifier
                                                .border(
                                                    width = 1.dp,
                                                    brush = Brush.linearGradient(listOf(NeonTeal, NeonCyan)),
                                                    shape = RoundedCornerShape(20.dp),
                                                )
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            color = NeonTeal,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }

                                // Title overlay at bottom of image
                                Text(
                                    listing.title,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(20.dp),
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }

                            // Content
                            Column(
                                Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                // Status + availability + validity + kind row
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    ListingStatusBadge(listing.status)
                                    AvailabilityBadge(listing.availability)
                                    ValidityIndicator(listing)
                                    Spacer(Modifier.weight(1f))
                                    // Kind chip with neon border
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = NeonCyan.copy(alpha = 0.08f),
                                    ) {
                                        Text(
                                            "$kindEmoji ${listing.kind.name}",
                                            modifier = Modifier
                                                .border(
                                                    width = 1.dp,
                                                    color = NeonCyan.copy(alpha = 0.3f),
                                                    shape = RoundedCornerShape(12.dp),
                                                )
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = NeonCyan,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }

                                // Description
                                Text(
                                    listing.description,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFFCBD5E1),
                                )

                                // Tags — neon chips
                                if (listing.tags.isNotEmpty()) {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        listing.tags.forEach { tag ->
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = NeonPurple.copy(alpha = 0.1f),
                                            ) {
                                                Text(
                                                    tag,
                                                    modifier = Modifier
                                                        .border(
                                                            width = 1.dp,
                                                            color = NeonPurple.copy(alpha = 0.3f),
                                                            shape = RoundedCornerShape(12.dp),
                                                        )
                                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = NeonPurple,
                                                )
                                            }
                                        }
                                    }
                                }

                                // Divider with glow
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color.Transparent,
                                                    NeonCyan.copy(alpha = 0.4f),
                                                    Color.Transparent,
                                                )
                                            )
                                        )
                                )

                                // Owner card — glass style
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = FutureCard,
                                    ),
                                    elevation = CardDefaults.cardElevation(0.dp),
                                    modifier = Modifier.border(
                                        width = 1.dp,
                                        brush = Brush.linearGradient(
                                            listOf(FutureBorder, NeonCyan.copy(alpha = 0.2f))
                                        ),
                                        shape = RoundedCornerShape(16.dp),
                                    ),
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        // Avatar with neon ring
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(listOf(NeonCyan, NeonTeal))
                                                ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(FutureCard),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    listing.owner.displayName.first().toString(),
                                                    color = NeonCyan,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp,
                                                )
                                            }
                                        }

                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                listing.owner.displayName,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = Color.White,
                                            )
                                            Text(
                                                listing.owner.location,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF94A3B8),
                                            )
                                        }

                                        // Rating with neon accent
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = BarterAmberLight.copy(alpha = 0.12f),
                                        ) {
                                            Text(
                                                "\u2605 ${listing.owner.rating}",
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                color = BarterAmberLight,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }

                                // Bottom spacer for action bar clearance
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }

        // Match celebration overlay
        val matchId = state.lastMatchId
        if (matchId != null) {
            MatchCelebration(
                onChat = {
                    vm.dismissMatch()
                    onOpenChat(matchId)
                },
                onDismiss = { vm.dismissMatch() },
            )
        }
    }
}
