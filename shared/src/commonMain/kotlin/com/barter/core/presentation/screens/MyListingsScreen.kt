package com.barter.core.presentation.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barter.core.di.AppDI
import com.barter.core.domain.model.Listing
import com.barter.core.domain.model.ListingKind
import com.barter.core.domain.model.ListingStatus
import com.barter.core.domain.model.RENEWAL_COST_MDL
import com.barter.core.domain.model.isExpired
import com.barter.core.domain.model.status
import com.barter.core.presentation.components.AvailabilityBadge
import com.barter.core.presentation.components.ListingImage
import com.barter.core.presentation.components.ListingStatusBadge
import com.barter.core.presentation.components.ValidityIndicator
import com.barter.core.presentation.theme.*
import com.barter.core.presentation.vm.MyListingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListingsScreen(
    onBack: () -> Unit,
    onCreateListing: () -> Unit,
    onEditListing: (listingId: String) -> Unit,
) {
    val vm: MyListingsViewModel = remember { AppDI.get() }
    val state by vm.state.collectAsState()

    var deleteConfirmId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { vm.load() }

    // Renewal error dialog (insufficient balance)
    state.renewalError?.let { errorMsg ->
        AlertDialog(
            onDismissRequest = { vm.dismissRenewalError() },
            title = { Text("Renewal Failed") },
            text = { Text(errorMsg) },
            confirmButton = {
                Button(
                    onClick = { vm.dismissRenewalError() },
                    colors = ButtonDefaults.buttonColors(containerColor = BarterTeal),
                ) {
                    Text("OK")
                }
            },
        )
    }

    // Delete confirmation dialog
    deleteConfirmId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteConfirmId = null },
            title = { Text("Delete Listing") },
            text = { Text("Are you sure you want to delete this listing? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.deleteListing(id)
                        deleteConfirmId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BarterCoral),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmId = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Listings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateListing,
                containerColor = BarterAmber,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, "Add listing")
            }
        },
    ) { padding ->
        when {
            state.loading -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = BarterTeal)
                }
            }
            state.listings.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("\uD83D\uDCE6", fontSize = 48.sp)
                        Text(
                            "No listings yet",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            "Tap + to create your first listing",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.listings.size) { index ->
                        val listing = state.listings[index]
                        MyListingCard(
                            listing = listing,
                            isDeleting = state.deletingId == listing.id,
                            onEdit = { onEditListing(listing.id) },
                            onDelete = { deleteConfirmId = listing.id },
                            onToggleVisibility = { vm.toggleVisibility(listing.id) },
                            onRenew = { vm.renewListing(listing.id) },
                            onCycleAvailability = { vm.cycleAvailability(listing.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MyListingCard(
    listing: Listing,
    isDeleting: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleVisibility: () -> Unit,
    onRenew: () -> Unit,
    onCycleAvailability: () -> Unit,
) {
    val listingStatus = listing.status
    val cardAlpha = if (listingStatus == ListingStatus.HIDDEN) 0.6f else 1f
    val borderColor = when (listingStatus) {
        ListingStatus.EXPIRED -> BarterAmber.copy(alpha = 0.6f)
        ListingStatus.HIDDEN -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .alpha(cardAlpha)
            .then(
                if (listingStatus == ListingStatus.EXPIRED) {
                    Modifier.border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
                } else Modifier
            ),
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Thumbnail
                ListingImage(
                    imageUrl = listing.imageUrl,
                    modifier = Modifier.size(72.dp),
                )

                // Info column
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        listing.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ListingStatusBadge(listingStatus)
                        AvailabilityBadge(listing.availability)
                        ValidityIndicator(listing)
                    }

                    Text(
                        listing.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Value badge
                listing.estimatedValue?.let { value ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BarterGreen.copy(alpha = 0.12f),
                    ) {
                        Text(
                            "EUR ${value.toInt()}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = BarterGreen,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            if (listing.tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listing.tags.take(4).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BarterAmber.copy(alpha = 0.1f),
                        ) {
                            Text(
                                tag,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = BarterAmber,
                            )
                        }
                    }
                }
            }

            // Actions
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = BarterCoral,
                        strokeWidth = 2.dp,
                    )
                } else {
                    // Cycle availability
                    TextButton(onClick = onCycleAvailability) {
                        Icon(Icons.Default.SwapHoriz, null, Modifier.size(16.dp), tint = BarterTeal)
                        Spacer(Modifier.width(2.dp))
                        Text("Status", color = BarterTeal, style = MaterialTheme.typography.labelSmall)
                    }

                    // Hide/Show toggle
                    IconButton(onClick = onToggleVisibility, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (listingStatus == ListingStatus.HIDDEN)
                                Icons.Default.Visibility
                            else
                                Icons.Default.VisibilityOff,
                            "Toggle visibility",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Renew button (expired only) — costs money
                    if (listing.isExpired) {
                        TextButton(onClick = onRenew) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(16.dp), tint = BarterAmber)
                            Spacer(Modifier.width(4.dp))
                            Text("Renew (${RENEWAL_COST_MDL.toInt()} MDL)", color = BarterAmber)
                        }
                    }

                    TextButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, null, Modifier.size(16.dp), tint = BarterTeal)
                        Spacer(Modifier.width(4.dp))
                        Text("Edit", color = BarterTeal)
                    }
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, null, Modifier.size(16.dp), tint = BarterCoral)
                        Spacer(Modifier.width(4.dp))
                        Text("Delete", color = BarterCoral)
                    }
                }
            }
        }
    }
}
