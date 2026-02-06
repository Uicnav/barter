package com.barter.core.presentation.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.barter.core.di.AppDI
import com.barter.core.domain.model.AvailabilityStatus
import com.barter.core.domain.model.Listing
import com.barter.core.domain.model.ListingKind
import com.barter.core.presentation.components.AvailabilityBadge
import com.barter.core.presentation.components.MatchCelebration
import com.barter.core.presentation.theme.*
import com.barter.core.presentation.vm.DiscoveryViewModel
import kotlinx.coroutines.launch

@Composable
fun DiscoveryScreen(
    onOpenMatches: () -> Unit,
    onOpenChat: (matchId: String) -> Unit,
    onCreateListing: () -> Unit,
    onOpenListingDetail: (listingId: String) -> Unit,
) {
    val vm: DiscoveryViewModel = remember { AppDI.get() }
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.load() }

    Box(Modifier.fillMaxSize()) {
        when {
            state.loading -> {
                CircularProgressIndicator(
                    Modifier.align(Alignment.Center),
                    color = BarterTeal,
                )
            }
            state.cards.isEmpty() && state.allCards.isEmpty() -> {
                EmptyDiscovery(
                    onReload = { vm.load() },
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            else -> {
                Column(Modifier.fillMaxSize()) {
                    // Category filter chips
                    if (state.categories.isNotEmpty()) {
                        LazyRow(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                        ) {
                            item {
                                FilterChip(
                                    selected = state.selectedCategory == null,
                                    onClick = { vm.selectCategory(null) },
                                    label = { Text("All") },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BarterTeal.copy(alpha = 0.15f),
                                        selectedLabelColor = BarterTeal,
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = MaterialTheme.colorScheme.outline,
                                        selectedBorderColor = BarterTeal,
                                        enabled = true,
                                        selected = state.selectedCategory == null,
                                    ),
                                )
                            }
                            items(state.categories.size) { index ->
                                val cat = state.categories[index]
                                FilterChip(
                                    selected = state.selectedCategory == cat.id,
                                    onClick = { vm.selectCategory(cat.id) },
                                    label = { Text("${cat.emoji} ${cat.label}") },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BarterTeal.copy(alpha = 0.15f),
                                        selectedLabelColor = BarterTeal,
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = MaterialTheme.colorScheme.outline,
                                        selectedBorderColor = BarterTeal,
                                        enabled = true,
                                        selected = state.selectedCategory == cat.id,
                                    ),
                                )
                            }
                        }
                    }

                    if (state.cards.isEmpty()) {
                        // Filtered to empty
                        Box(
                            Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("No listings in this category", style = MaterialTheme.typography.titleMedium)
                                TextButton(onClick = { vm.selectCategory(null) }) {
                                    Text("Show All")
                                }
                            }
                        }
                    } else {
                        // Card stack
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            SwipeableCardStack(
                                cards = state.cards,
                                onSwipeRight = { vm.likeTopCard() },
                                onSwipeLeft = { vm.passTopCard() },
                                onCardTap = { onOpenListingDetail(it.id) },
                            )
                        }

                        // Action buttons
                        ActionButtons(
                            onPass = { vm.passTopCard() },
                            onLike = { vm.likeTopCard() },
                            onUndo = if (state.lastSwipedCard != null) {
                                { vm.undoLastSwipe() }
                            } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                        )

                        // Card counter
                        Text(
                            "${state.cards.size} listings left",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // FAB for creating new listing
        FloatingActionButton(
            onClick = onCreateListing,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp),
            containerColor = BarterAmber,
            contentColor = Color.White,
        ) {
            Icon(Icons.Default.Add, "Create listing")
        }

        // Match celebration overlay
        state.lastMatchId?.let { matchId ->
            MatchCelebration(
                onChat = { onOpenChat(matchId) },
                onDismiss = { vm.dismissMatch() },
            )
        }
    }
}

// ── Swipeable card stack ───────────────────────────────────
@Composable
private fun SwipeableCardStack(
    cards: List<Listing>,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    onCardTap: (Listing) -> Unit,
) {
    val scope = rememberCoroutineScope()

    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Background cards (show peek of next 2)
        val visible = cards.take(3)
        visible.asReversed().forEachIndexed { idx, listing ->
            val depth = visible.size - 1 - idx
            if (depth > 0) {
                ListingCard(
                    listing = listing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .graphicsLayer {
                            scaleX = 1f - depth * 0.05f
                            scaleY = 1f - depth * 0.05f
                            translationY = depth * 28f
                            alpha = 1f - depth * 0.15f
                        },
                )
            }
        }

        // Top card — draggable
        cards.firstOrNull()?.let { top ->
            key(top.id) {
                val offsetX = remember { Animatable(0f) }
                val offsetY = remember { Animatable(0f) }

                val rotation = (offsetX.value / 30f).coerceIn(-12f, 12f)
                val likeAlpha = (offsetX.value / 250f).coerceIn(0f, 1f)
                val passAlpha = (-offsetX.value / 250f).coerceIn(0f, 1f)

                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .graphicsLayer {
                            translationX = offsetX.value
                            translationY = offsetY.value
                            rotationZ = rotation
                        }
                        .pointerInput(top.id) {
                            detectDragGestures(
                                onDragEnd = {
                                    scope.launch {
                                        when {
                                            offsetX.value > 220f -> {
                                                offsetX.animateTo(1500f, tween(300))
                                                onSwipeRight()
                                            }
                                            offsetX.value < -220f -> {
                                                offsetX.animateTo(-1500f, tween(300))
                                                onSwipeLeft()
                                            }
                                            else -> {
                                                // Small drag = tap → open detail
                                                if (kotlin.math.abs(offsetX.value) < 10f &&
                                                    kotlin.math.abs(offsetY.value) < 10f
                                                ) {
                                                    onCardTap(top)
                                                }
                                                launch {
                                                    offsetX.animateTo(
                                                        0f,
                                                        spring(stiffness = Spring.StiffnessMediumLow)
                                                    )
                                                }
                                                launch {
                                                    offsetY.animateTo(
                                                        0f,
                                                        spring(stiffness = Spring.StiffnessMediumLow)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                onDrag = { change, drag ->
                                    change.consume()
                                    scope.launch {
                                        offsetX.snapTo(offsetX.value + drag.x)
                                        offsetY.snapTo(offsetY.value + drag.y)
                                    }
                                },
                            )
                        },
                ) {
                    ListingCard(listing = top, modifier = Modifier.fillMaxSize())

                    // LIKE overlay
                    if (likeAlpha > 0f) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(24.dp))
                                .background(BarterGreen.copy(alpha = likeAlpha * 0.2f)),
                            contentAlignment = Alignment.TopStart,
                        ) {
                            Text(
                                "LIKE",
                                color = BarterGreen.copy(alpha = likeAlpha),
                                fontSize = 40.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier
                                    .padding(32.dp)
                                    .graphicsLayer(rotationZ = -15f)
                                    .border(
                                        3.dp,
                                        BarterGreen.copy(alpha = likeAlpha),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }

                    // PASS overlay
                    if (passAlpha > 0f) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(24.dp))
                                .background(BarterCoral.copy(alpha = passAlpha * 0.2f)),
                            contentAlignment = Alignment.TopEnd,
                        ) {
                            Text(
                                "PASS",
                                color = BarterCoral.copy(alpha = passAlpha),
                                fontSize = 40.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier
                                    .padding(32.dp)
                                    .graphicsLayer(rotationZ = 15f)
                                    .border(
                                        3.dp,
                                        BarterCoral.copy(alpha = passAlpha),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Single listing card (photo-backed or gradient fallback) ──
@Composable
private fun ListingCard(
    listing: Listing,
    modifier: Modifier = Modifier,
) {
    val gradientColors = when (listing.kind) {
        ListingKind.GOODS -> listOf(Color(0xFF667eea), Color(0xFF764ba2))
        ListingKind.SERVICES -> listOf(Color(0xFFf093fb), Color(0xFFf5576c))
        ListingKind.BOTH -> listOf(Color(0xFF4facfe), Color(0xFF00f2fe))
    }

    val kindEmoji = when (listing.kind) {
        ListingKind.GOODS -> "\uD83D\uDCE6"
        ListingKind.SERVICES -> "\u2728"
        ListingKind.BOTH -> "\uD83D\uDD04"
    }

    val hasPhoto = listing.imageUrl.isNotBlank()

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (hasPhoto) {
                // Photo background (full-bleed)
                AsyncImage(
                    model = listing.imageUrl,
                    contentDescription = listing.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                // Gradient background fallback
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = gradientColors,
                                start = Offset.Zero,
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                            )
                        )
                )

                // Category emoji (only for gradient fallback)
                Text(
                    kindEmoji,
                    fontSize = 72.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-60).dp),
                )
            }

            // Kind badge + Value badge (top row)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Value badge
                listing.estimatedValue?.let { value ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF27AE60).copy(alpha = 0.85f),
                    ) {
                        Text(
                            "EUR ${value.toInt()}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.25f),
                ) {
                    Text(
                        listing.kind.name,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Tags + availability
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (listing.availability != AvailabilityStatus.AVAILABLE) {
                    AvailabilityBadge(listing.availability)
                }
                listing.tags.take(3).forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.2f),
                    ) {
                        Text(
                            tag,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }

            // Dark gradient overlay at bottom
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.45f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                        )
                    )
            )

            // Info
            Column(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    listing.title,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Avatar circle
                    Surface(
                        modifier = Modifier.size(28.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.3f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                listing.owner.displayName.first().toString(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                            )
                        }
                    }

                    Text(
                        "${listing.owner.displayName} \u2022 ${listing.owner.location}",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Text(
                        "\u2605 ${listing.owner.rating}",
                        color = BarterAmberLight,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Text(
                    listing.description,
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ── Action buttons ─────────────────────────────────────────
@Composable
private fun ActionButtons(
    onPass: () -> Unit,
    onLike: () -> Unit,
    onUndo: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Pass
        FloatingActionButton(
            onClick = onPass,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = BarterCoral,
            shape = CircleShape,
            modifier = Modifier.size(56.dp),
            elevation = FloatingActionButtonDefaults.elevation(4.dp),
        ) {
            Icon(Icons.Default.Close, "Pass", Modifier.size(28.dp))
        }

        Spacer(Modifier.width(20.dp))

        // Undo
        if (onUndo != null) {
            FloatingActionButton(
                onClick = onUndo,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = BarterAmber,
                shape = CircleShape,
                modifier = Modifier.size(44.dp),
                elevation = FloatingActionButtonDefaults.elevation(4.dp),
            ) {
                Icon(Icons.Default.Refresh, "Undo", Modifier.size(20.dp))
            }
            Spacer(Modifier.width(20.dp))
        }

        // Like
        FloatingActionButton(
            onClick = onLike,
            containerColor = BarterGreen,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.size(64.dp),
            elevation = FloatingActionButtonDefaults.elevation(6.dp),
        ) {
            Icon(Icons.Default.Favorite, "Like", Modifier.size(32.dp))
        }
    }
}

// ── Empty state ────────────────────────────────────────────
@Composable
private fun EmptyDiscovery(
    onReload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("\uD83D\uDD0D", fontSize = 56.sp)
        Text(
            "No more listings",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            "Check back soon for new barter opportunities!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onReload,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BarterTeal),
        ) {
            Text("Reload", Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }
    }
}

