package com.barter.core.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.barter.core.di.AppDI
import com.barter.core.domain.model.ListingKind
import com.barter.core.presentation.components.AvailabilityPicker
import com.barter.core.presentation.components.PhotoPickerSection
import com.barter.core.presentation.theme.*
import com.barter.core.presentation.vm.CreateListingViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateListingScreen(
    onBack: () -> Unit,
) {
    val vm: CreateListingViewModel = remember { AppDI.get() }
    val state by vm.state.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Listing") },
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
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // ── Photo Section ──────────────────────────────
            SectionHeader(icon = Icons.Default.Photo, title = "Photo")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp),
            ) {
                Box(Modifier.padding(16.dp)) {
                    PhotoPickerSection(
                        imageUrl = state.imageUrl,
                        onUrlChange = vm::onImageUrlChange,
                    )
                }
            }

            // ── Details Section ────────────────────────────
            SectionHeader(icon = Icons.Default.Title, title = "Details")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp),
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = vm::onTitleChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Title") },
                        placeholder = { Text("What are you offering?") },
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
                        placeholder = { Text("Describe what you offer and what you're looking for...") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BarterTeal,
                            focusedLabelColor = BarterTeal,
                            cursorColor = BarterTeal,
                        ),
                    )

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
                }
            }

            // ── Validity info ─────────────────────────────
            SectionHeader(icon = Icons.Default.Schedule, title = "Validity")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BarterTeal.copy(alpha = 0.06f)),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                Row(
                    Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Schedule, null, tint = BarterTeal, modifier = Modifier.size(20.dp))
                    Text(
                        "Your listing will be active for 30 days. After expiration, you can renew it for 10 MDL.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Type Section ───────────────────────────────
            SectionHeader(icon = Icons.Default.Description, title = "Type")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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

            // ── Availability Section ─────────────────────────
            SectionHeader(icon = Icons.Default.Inventory2, title = "Availability")
            AvailabilityPicker(
                selected = state.availability,
                onSelect = vm::onAvailabilityChange,
            )

            // ── Tags Section ───────────────────────────────
            SectionHeader(icon = Icons.Default.LocalOffer, title = "Tags")
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
                Text(
                    error,
                    color = BarterCoral,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = vm::submit,
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
                    Text("Create Listing", Modifier.padding(vertical = 4.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
internal fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
) {
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = BarterTeal)
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = BarterTeal,
        )
    }
}
