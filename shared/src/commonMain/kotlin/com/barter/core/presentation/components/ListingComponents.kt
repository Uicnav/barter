package com.barter.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.barter.core.data.ImageStore
import com.barter.core.domain.model.AvailabilityStatus
import com.barter.core.domain.model.Listing
import com.barter.core.domain.model.ListingStatus
import com.barter.core.domain.model.daysRemaining
import com.barter.core.domain.model.isExpired
import com.barter.core.domain.model.status
import com.barter.core.presentation.theme.*
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.readBytes
import com.barter.core.util.currentTimeMillis
import kotlinx.coroutines.launch

@Composable
fun ListingImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val localBytes = if (ImageStore.isLocal(imageUrl)) ImageStore.load(imageUrl) else null
    when {
        localBytes != null -> {
            AsyncImage(
                model = localBytes,
                contentDescription = "Listing photo",
                modifier = modifier.clip(RoundedCornerShape(12.dp)),
                contentScale = contentScale,
            )
        }
        imageUrl.isNotBlank() -> {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Listing photo",
                modifier = modifier.clip(RoundedCornerShape(12.dp)),
                contentScale = contentScale,
            )
        }
        else -> {
            // Gradient placeholder fallback
            Box(
                modifier = modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                BarterTeal.copy(alpha = 0.3f),
                                BarterAmber.copy(alpha = 0.3f),
                            )
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = BarterTeal.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
fun ListingStatusBadge(status: ListingStatus) {
    val (color, label) = when (status) {
        ListingStatus.ACTIVE -> BarterGreen to "Active"
        ListingStatus.EXPIRED -> BarterAmber to "Expired"
        ListingStatus.HIDDEN -> Color.Gray to "Hidden"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun ValidityIndicator(listing: Listing) {
    val days = listing.daysRemaining
    when {
        listing.isExpired -> {
            Text(
                "Expired",
                style = MaterialTheme.typography.labelSmall,
                color = BarterCoral,
                fontWeight = FontWeight.Bold,
            )
        }
        days != null && days <= 7 -> {
            Text(
                "$days days left",
                style = MaterialTheme.typography.labelSmall,
                color = BarterAmber,
                fontWeight = FontWeight.SemiBold,
            )
        }
        days != null -> {
            Text(
                "$days days left",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun PhotoUrlField(
    imageUrl: String,
    onUrlChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Preview
        if (imageUrl.isNotBlank()) {
            ListingImage(
                imageUrl = imageUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )
        }

        OutlinedTextField(
            value = imageUrl,
            onValueChange = onUrlChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Image URL") },
            placeholder = { Text("https://example.com/photo.jpg") },
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Image, null, tint = BarterTeal)
            },
            trailingIcon = {
                if (imageUrl.isNotBlank()) {
                    IconButton(onClick = { onUrlChange("") }) {
                        Icon(Icons.Default.Clear, "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BarterTeal,
                focusedLabelColor = BarterTeal,
                cursorColor = BarterTeal,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerButton(
    selectedMs: Long?,
    onDateSelected: (Long?) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }

    val formattedDate = selectedMs?.let { ms ->
        val days = ((ms - currentTimeMillis()) / 86_400_000).toInt()
        if (days > 0) "Valid for $days days" else "Expired"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = { showPicker = true },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(formattedDate ?: "Set Validity Date")
        }

        if (selectedMs != null) {
            IconButton(onClick = { onDateSelected(null) }) {
                Icon(Icons.Default.Clear, "Clear date", tint = BarterCoral)
            }
        }
    }

    if (showPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedMs ?: (currentTimeMillis() + 30L * 86_400_000),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                    showPicker = false
                }) {
                    Text("OK", color = BarterTeal)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = BarterTeal,
                    todayDateBorderColor = BarterTeal,
                ),
            )
        }
    }
}

// ── Availability badge ────────────────────────────────────
@Composable
fun AvailabilityBadge(availability: AvailabilityStatus) {
    val (color, label) = when (availability) {
        AvailabilityStatus.AVAILABLE -> BarterGreen to "Available"
        AvailabilityStatus.RESERVED -> BarterAmber to "Reserved"
        AvailabilityStatus.SOLD -> Color.Gray to "Sold"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ── Availability picker (row of FilterChips) ─────────────
@Composable
fun AvailabilityPicker(
    selected: AvailabilityStatus,
    onSelect: (AvailabilityStatus) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AvailabilityStatus.entries.forEach { status ->
            val isSelected = selected == status
            val (chipColor, label) = when (status) {
                AvailabilityStatus.AVAILABLE -> BarterGreen to "Available"
                AvailabilityStatus.RESERVED -> BarterAmber to "Reserved"
                AvailabilityStatus.SOLD -> Color.Gray to "Sold"
            }
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(status) },
                label = { Text(label) },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = chipColor.copy(alpha = 0.15f),
                    selectedLabelColor = chipColor,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = MaterialTheme.colorScheme.outline,
                    selectedBorderColor = chipColor,
                    enabled = true,
                    selected = isSelected,
                ),
            )
        }
    }
}

// ── Match celebration overlay ────────────────────────────
@Composable
fun MatchCelebration(
    onChat: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(12.dp),
        ) {
            Column(
                Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("\uD83C\uDF89", fontSize = 56.sp)
                Text(
                    "It's a Match!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = BarterTeal,
                )
                Text(
                    "You both want to barter!\nStart chatting to agree on a deal.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onChat,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BarterTeal),
                ) {
                    Text("Start Chatting", Modifier.padding(vertical = 4.dp))
                }
                TextButton(onClick = onDismiss) {
                    Text("Keep Swiping")
                }
            }
        }
    }
}

// ── Photo picker section (URL input + gallery/camera placeholders) ──
@Composable
fun PhotoPickerSection(
    imageUrl: String,
    onUrlChange: (String) -> Unit,
) {
    var showUrlInput by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val imagePickerLauncher = rememberFilePickerLauncher(
        type = FileKitType.Image,
        title = "Pick a photo",
    ) { file ->
        file?.let {
            scope.launch {
                val bytes = it.readBytes()
                val key = ImageStore.store(bytes)
                onUrlChange(key)
            }
        }
    }

    val cameraLauncher = rememberCameraLauncher { file ->
        file?.let {
            scope.launch {
                val bytes = it.readBytes()
                val key = ImageStore.store(bytes)
                onUrlChange(key)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Preview
        if (imageUrl.isNotBlank()) {
            ListingImage(
                imageUrl = imageUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { cameraLauncher.launch() },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.CameraAlt, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Camera")
            }
            OutlinedButton(
                onClick = { imagePickerLauncher.launch() },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.PhotoLibrary, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Gallery")
            }
        }

        // URL fallback (collapsible)
        TextButton(onClick = { showUrlInput = !showUrlInput }) {
            Text(
                if (showUrlInput) "Hide URL input" else "Enter URL instead",
                color = BarterTeal,
            )
        }

        if (showUrlInput) {
            OutlinedTextField(
                value = imageUrl,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Image URL") },
                placeholder = { Text("https://example.com/photo.jpg") },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Image, null, tint = BarterTeal)
                },
                trailingIcon = {
                    if (imageUrl.isNotBlank()) {
                        IconButton(onClick = { onUrlChange("") }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BarterTeal,
                    focusedLabelColor = BarterTeal,
                    cursorColor = BarterTeal,
                ),
            )
        }
    }
}
