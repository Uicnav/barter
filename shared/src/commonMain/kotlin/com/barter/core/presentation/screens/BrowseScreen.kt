package com.barter.core.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barter.core.di.AppDI
import com.barter.core.domain.model.Listing
import com.barter.core.domain.model.SortOption
import com.barter.core.presentation.components.ListingImage
import com.barter.core.presentation.components.MatchCelebration
import com.barter.core.presentation.theme.*
import com.barter.core.presentation.vm.BrowseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    onOpenListingDetail: (listingId: String) -> Unit,
    onOpenChat: (matchId: String) -> Unit = {},
) {
    val vm: BrowseViewModel = remember { AppDI.get() }
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.load() }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Search bar — filled style
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                TextField(
                    value = state.query,
                    onValueChange = vm::onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search listings...") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = BarterTeal) },
                    trailingIcon = {
                        if (state.query.isNotBlank()) {
                            TextButton(onClick = { vm.search() }) {
                                Text("Search", color = BarterTeal)
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = BarterTeal,
                    ),
                )
            }

            // Category chips — solid BarterTeal when selected
            LazyRow(
                Modifier.fillMaxWidth(),
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
                            selectedContainerColor = BarterTeal,
                            selectedLabelColor = Color.White,
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
                            selectedContainerColor = BarterTeal,
                            selectedLabelColor = Color.White,
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

            // Sort chips — solid BarterAmber when selected
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SortOption.entries.forEach { sort ->
                    val isSelected = state.sortBy == sort
                    val label = when (sort) {
                        SortOption.NEWEST -> "Newest"
                        SortOption.PRICE_LOW_HIGH -> "Price \u2191"
                        SortOption.PRICE_HIGH_LOW -> "Price \u2193"
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { vm.setSortBy(sort) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BarterAmber,
                            selectedLabelColor = Color.White,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = BarterAmber,
                            enabled = true,
                            selected = isSelected,
                        ),
                    )
                }
            }

            // Grid content
            when {
                state.loading -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = BarterTeal)
                    }
                }
                state.listings.isEmpty() -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("\uD83D\uDD0D", fontSize = 48.sp)
                            Text(
                                "No listings found",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                "Try a different search or category",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.listings.size) { index ->
                            val listing = state.listings[index]
                            BrowseGridCard(
                                listing = listing,
                                isLiked = listing.id in state.likedIds,
                                isLiking = state.likingId == listing.id,
                                onClick = { onOpenListingDetail(listing.id) },
                                onLike = { vm.likeListing(listing.id) },
                            )
                        }
                    }
                }
            }
        }

        // Match overlay
        state.lastMatchId?.let { matchId ->
            MatchCelebration(
                onChat = { onOpenChat(matchId) },
                onDismiss = { vm.dismissMatch() },
            )
        }
    }
}

@Composable
private fun BrowseGridCard(
    listing: Listing,
    isLiked: Boolean,
    isLiking: Boolean,
    onClick: () -> Unit,
    onLike: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(3.dp),
    ) {
        Column {
            // Thumbnail with gradient scrim + price badge + heart
            Box {
                ListingImage(
                    imageUrl = listing.imageUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentScale = ContentScale.Crop,
                )

                // Gradient scrim at bottom of image
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                            )
                        )
                )

                // Price badge (bottom-left, overlaid on image)
                listing.estimatedValue?.let { value ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = BarterGreen.copy(alpha = 0.85f),
                    ) {
                        Text(
                            "EUR ${value.toInt()}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // Heart button (bottom-right, overlaid on image)
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(32.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.9f),
                    shadowElevation = 2.dp,
                    onClick = onLike,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isLiking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = BarterCoral,
                            )
                        } else {
                            Icon(
                                if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isLiked) "Liked" else "Like",
                                modifier = Modifier.size(18.dp),
                                tint = if (isLiked) BarterCoral else Color.Gray,
                            )
                        }
                    }
                }
            }

            // Info
            Column(
                Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    listing.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                // Owner row with mini avatar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(20.dp),
                        shape = CircleShape,
                        color = BarterTeal.copy(alpha = 0.15f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                listing.owner.displayName.first().toString(),
                                color = BarterTeal,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                            )
                        }
                    }
                    Text(
                        "${listing.owner.displayName} \u2022 ${listing.owner.location}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
