package com.barter.core.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.barter.core.di.AppDI
import com.barter.core.domain.model.ListingKind
import com.barter.core.domain.model.ListingStatus
import com.barter.core.domain.model.RENEWAL_COST_MDL
import com.barter.core.util.currentTimeMillis
import com.barter.core.presentation.components.AvailabilityBadge
import com.barter.core.presentation.components.AvailabilityPicker
import com.barter.core.presentation.components.ListingStatusBadge
import com.barter.core.presentation.components.PhotoPickerSection
import com.barter.core.presentation.theme.*
import com.barter.core.presentation.vm.EditListingViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditListingScreen(
    listingId: String,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
) {
    val vm: EditListingViewModel = remember { AppDI.get() }
    val state by vm.state.collectAsState()

    LaunchedEffect(listingId) { vm.loadListing(listingId) }

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onDeleted()
    }

    // Delete confirmation dialog
    if (state.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { vm.dismissDelete() },
            title = { Text("Delete Listing") },
            text = { Text("Are you sure you want to delete this listing? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { vm.confirmDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = BarterCoral),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismissDelete() }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Edit Listing")
                        if (!state.loading) {
                            ListingStatusBadge(state.listingStatus)
                            AvailabilityBadge(state.availability)
                        }
                    }
                },
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
    ) { padding ->
        if (state.loading) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = BarterTeal)
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Photo
                PhotoPickerSection(
                    imageUrl = state.imageUrl,
                    onUrlChange = vm::onImageUrlChange,
                )

                OutlinedTextField(
                    value = state.title,
                    onValueChange = vm::onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BarterTeal,
                        focusedLabelColor = BarterTeal,
                        cursorColor = BarterTeal,
                    ),
                )

                OutlinedTextField(
                    value = state.description,
                    onValueChange = vm::onDescriptionChange,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    label = { Text("Description") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BarterTeal,
                        focusedLabelColor = BarterTeal,
                        cursorColor = BarterTeal,
                    ),
                )

                // Estimated value
                OutlinedTextField(
                    value = state.estimatedValue,
                    onValueChange = vm::onEstimatedValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Estimated Value (EUR)") },
                    placeholder = { Text("e.g. 150") },
                    singleLine = true,
                    leadingIcon = { Text("EUR", style = MaterialTheme.typography.labelMedium, color = BarterTeal) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BarterTeal,
                        focusedLabelColor = BarterTeal,
                        cursorColor = BarterTeal,
                    ),
                )

                // Validity info
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BarterTeal.copy(alpha = 0.06f)),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = BarterTeal, modifier = Modifier.size(18.dp))
                        val daysText = state.validUntilMs?.let {
                            val days = ((it - currentTimeMillis()) / 86_400_000).toInt()
                            if (days > 0) "$days days remaining" else "Expired"
                        } ?: "No expiry set"
                        Text(
                            "Validity: $daysText (30-day periods, renewal costs ${RENEWAL_COST_MDL.toInt()} MDL)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Kind selector
                Text("Type", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ListingKind.entries.forEach { kind ->
                        val selected = state.kind == kind
                        val emoji = when (kind) {
                            ListingKind.GOODS -> "\uD83D\uDCE6"
                            ListingKind.SERVICES -> "\u2728"
                            ListingKind.BOTH -> "\uD83D\uDD04"
                        }
                        FilterChip(
                            selected = selected,
                            onClick = { vm.onKindChange(kind) },
                            label = { Text("$emoji ${kind.name}") },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BarterTeal.copy(alpha = 0.15f),
                                selectedLabelColor = BarterTeal,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = MaterialTheme.colorScheme.outline,
                                selectedBorderColor = BarterTeal,
                                enabled = true,
                                selected = selected,
                            ),
                        )
                    }
                }

                // Availability
                Text("Availability", style = MaterialTheme.typography.titleMedium)
                AvailabilityPicker(
                    selected = state.availability,
                    onSelect = vm::onAvailabilityChange,
                )

                // Tags
                Text("Tags", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    vm.tagOptions.forEach { cat ->
                        val selected = cat.id in state.selectedTags
                        FilterChip(
                            selected = selected,
                            onClick = { vm.toggleTag(cat.id) },
                            label = { Text("${cat.emoji} ${cat.label}") },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BarterAmber.copy(alpha = 0.15f),
                                selectedLabelColor = BarterAmber,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = MaterialTheme.colorScheme.outline,
                                selectedBorderColor = BarterAmber,
                                enabled = true,
                                selected = selected,
                            ),
                        )
                    }
                }

                state.error?.let { error ->
                    Text(error, color = BarterCoral, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(8.dp))

                // Hide/Show toggle button
                OutlinedButton(
                    onClick = vm::toggleVisibility,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.saving,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    val isHidden = state.listingStatus == ListingStatus.HIDDEN
                    Icon(
                        if (isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (isHidden) "Show Listing" else "Hide Listing")
                }

                // Renew button (if expired) — costs money
                if (state.listingStatus == ListingStatus.EXPIRED) {
                    Button(
                        onClick = vm::renewListing,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.saving,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BarterAmber),
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Renew +30 days (${RENEWAL_COST_MDL.toInt()} MDL)")
                    }
                }

                // Save button
                Button(
                    onClick = vm::save,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.saving,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BarterTeal),
                ) {
                    if (state.saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Save Changes", Modifier.padding(vertical = 4.dp))
                    }
                }

                // Delete button
                OutlinedButton(
                    onClick = vm::requestDelete,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BarterCoral),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                ) {
                    Text("Delete Listing", Modifier.padding(vertical = 4.dp), color = BarterCoral)
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
