package com.barter.core.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barter.core.di.AppDI
import com.barter.core.domain.model.Deal
import com.barter.core.domain.model.DealStatus
import com.barter.core.domain.model.DealValueSummary
import com.barter.core.presentation.theme.*
import com.barter.core.presentation.vm.DealViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealScreen(
    matchId: String,
    onBack: () -> Unit,
) {
    val vm: DealViewModel = remember { AppDI.get() }
    val state by vm.state.collectAsState()

    LaunchedEffect(matchId) { vm.bind(matchId) }

    // Review dialog
    if (state.showReviewDialog) {
        ReviewDialog(
            rating = state.reviewRating,
            comment = state.reviewComment,
            submitting = state.reviewSubmitting,
            onRatingChange = vm::onReviewRatingChange,
            onCommentChange = vm::onReviewCommentChange,
            onSubmit = vm::submitReview,
            onDismiss = vm::dismissReviewDialog,
        )
    }

    // Review submitted snackbar
    if (state.reviewSubmitted) {
        LaunchedEffect(Unit) {
            vm.dismissReviewDialog()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deals") },
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
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Propose form
            item {
                ProposalForm(
                    offerText = state.offerText,
                    requestText = state.requestText,
                    offerValue = state.offerValue,
                    requestValue = state.requestValue,
                    cashTopUp = state.cashTopUp,
                    note = state.note,
                    valueSummary = state.valueSummary,
                    onOfferChange = vm::onOfferChange,
                    onRequestChange = vm::onRequestChange,
                    onOfferValueChange = vm::onOfferValueChange,
                    onRequestValueChange = vm::onRequestValueChange,
                    onCashTopUpChange = vm::onCashTopUpChange,
                    onNoteChange = vm::onNoteChange,
                    onPropose = vm::propose,
                )
            }

            // Deals list
            if (state.deals.isNotEmpty()) {
                item {
                    Text(
                        "Active Deals",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                items(state.deals.size) { index ->
                    DealCard(
                        deal = state.deals[index],
                        onAccept = { vm.accept(state.deals[index].id) },
                        onReject = { vm.reject(state.deals[index].id) },
                        onComplete = { vm.complete(state.deals[index].id) },
                    )
                }
            }

            if (state.deals.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No deals yet. Propose one above!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProposalForm(
    offerText: String,
    requestText: String,
    offerValue: String,
    requestValue: String,
    cashTopUp: String,
    note: String,
    valueSummary: DealValueSummary?,
    onOfferChange: (String) -> Unit,
    onRequestChange: (String) -> Unit,
    onOfferValueChange: (String) -> Unit,
    onRequestValueChange: (String) -> Unit,
    onCashTopUpChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onPropose: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Propose a Barter",
                style = MaterialTheme.typography.titleLarge,
                color = BarterTeal,
            )

            // Offer items + value
            OutlinedTextField(
                value = offerText,
                onValueChange = onOfferChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("What you offer") },
                placeholder = { Text("e.g. AirPods, Android consulting") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BarterTeal,
                    focusedLabelColor = BarterTeal,
                    cursorColor = BarterTeal,
                ),
            )

            OutlinedTextField(
                value = offerValue,
                onValueChange = onOfferValueChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Your offer value (EUR)") },
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

            // Request items + value
            OutlinedTextField(
                value = requestText,
                onValueChange = onRequestChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("What you want") },
                placeholder = { Text("e.g. Apple Watch, manicure") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BarterAmber,
                    focusedLabelColor = BarterAmber,
                    cursorColor = BarterAmber,
                ),
            )

            OutlinedTextField(
                value = requestValue,
                onValueChange = onRequestValueChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Their offer value (EUR)") },
                placeholder = { Text("e.g. 100") },
                singleLine = true,
                leadingIcon = { Text("EUR", style = MaterialTheme.typography.labelMedium, color = BarterAmber) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BarterAmber,
                    focusedLabelColor = BarterAmber,
                    cursorColor = BarterAmber,
                ),
            )

            // Value comparison card
            valueSummary?.let { summary ->
                ValueComparisonCard(summary)
            }

            // Cash top-up
            OutlinedTextField(
                value = cashTopUp,
                onValueChange = onCashTopUpChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Cash top-up (EUR)") },
                placeholder = { Text("0") },
                singleLine = true,
                leadingIcon = { Text("EUR", style = MaterialTheme.typography.labelMedium, color = BarterGreen) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BarterGreen,
                    focusedLabelColor = BarterGreen,
                    cursorColor = BarterGreen,
                ),
            )

            // Note
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Note (optional)") },
                placeholder = { Text("Any details about the deal...") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BarterTeal,
                    focusedLabelColor = BarterTeal,
                    cursorColor = BarterTeal,
                ),
            )

            Button(
                onClick = onPropose,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BarterTeal),
            ) {
                Text("Propose Deal", Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun ValueComparisonCard(summary: DealValueSummary) {
    val bgColor = if (summary.isFair) BarterGreen.copy(alpha = 0.08f) else BarterAmber.copy(alpha = 0.08f)
    val accentColor = if (summary.isFair) BarterGreen else BarterAmber

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                if (summary.isFair) "Fair Exchange" else "Value Gap",
                style = MaterialTheme.typography.titleSmall,
                color = accentColor,
                fontWeight = FontWeight.Bold,
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("You offer", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("EUR ${summary.offerTotal.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("They offer", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("EUR ${summary.requestTotal.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            if (!summary.isFair && summary.suggestedTopUp > 0) {
                HorizontalDivider(color = accentColor.copy(alpha = 0.2f))
                Text(
                    "Suggested top-up: EUR ${summary.suggestedTopUp.toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun DealCard(
    deal: Deal,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onComplete: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Status badge
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Deal",
                    style = MaterialTheme.typography.titleMedium,
                )
                StatusBadge(deal.status)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Offer
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Offering:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    deal.offer.joinToString(", ") { item ->
                        val v = item.estimatedValue?.let { " (EUR ${it.toInt()})" } ?: ""
                        "${item.title}$v"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }

            // Request
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Requesting:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    deal.request.joinToString(", ") { item ->
                        val v = item.estimatedValue?.let { " (EUR ${it.toInt()})" } ?: ""
                        "${item.title}$v"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }

            // Cash top-up & note
            if (deal.cashTopUp > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BarterGreen.copy(alpha = 0.1f),
                ) {
                    Text(
                        "Cash top-up: EUR ${deal.cashTopUp.toInt()}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = BarterGreen,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (deal.note.isNotBlank()) {
                Text(
                    "Note: ${deal.note}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (deal.status == DealStatus.PROPOSED) {
                    Button(
                        onClick = onAccept,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BarterGreen),
                    ) {
                        Text("Accept")
                    }
                    OutlinedButton(
                        onClick = onReject,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Reject")
                    }
                }
                if (deal.status == DealStatus.ACCEPTED) {
                    Button(
                        onClick = onComplete,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BarterTeal),
                    ) {
                        Text("Mark Complete")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: DealStatus) {
    val (bg, fg) = when (status) {
        DealStatus.PROPOSED -> BarterAmber.copy(alpha = 0.15f) to BarterAmber
        DealStatus.ACCEPTED -> BarterGreen.copy(alpha = 0.15f) to BarterGreen
        DealStatus.REJECTED -> BarterCoral.copy(alpha = 0.15f) to BarterCoral
        DealStatus.CANCELLED -> Color.Gray.copy(alpha = 0.15f) to Color.Gray
        DealStatus.COMPLETED -> BarterTeal.copy(alpha = 0.15f) to BarterTeal
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
    ) {
        Text(
            status.name,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = fg,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ReviewDialog(
    rating: Int,
    comment: String,
    submitting: Boolean,
    onRatingChange: (Int) -> Unit,
    onCommentChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Rate this Deal",
                style = MaterialTheme.typography.titleLarge,
                color = BarterTeal,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "How was your experience?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Star rating
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.weight(1f))
                    (1..5).forEach { star ->
                        Icon(
                            imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Star $star",
                            tint = if (star <= rating) BarterAmber else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { onRatingChange(star) },
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }

                if (rating > 0) {
                    Text(
                        when (rating) {
                            1 -> "Poor"
                            2 -> "Fair"
                            3 -> "Good"
                            4 -> "Great"
                            5 -> "Excellent"
                            else -> ""
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = BarterAmber,
                        fontWeight = FontWeight.Bold,
                    )
                }

                OutlinedTextField(
                    value = comment,
                    onValueChange = onCommentChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Comment (optional)") },
                    placeholder = { Text("Share your experience...") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BarterTeal,
                        focusedLabelColor = BarterTeal,
                        cursorColor = BarterTeal,
                    ),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = rating > 0 && !submitting,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BarterTeal),
            ) {
                if (submitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                } else {
                    Text("Submit Review")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Skip")
            }
        },
        shape = RoundedCornerShape(20.dp),
    )
}
