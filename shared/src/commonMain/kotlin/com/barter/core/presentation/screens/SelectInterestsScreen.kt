package com.barter.core.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barter.core.di.AppDI
import com.barter.core.presentation.theme.*
import com.barter.core.presentation.vm.InterestsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SelectInterestsScreen(
    isOnboarding: Boolean = true,
    onDone: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val vm: InterestsViewModel = remember { AppDI.get<InterestsViewModel>() }
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.loadCurrentInterests() }

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Scaffold(
        topBar = {
            if (!isOnboarding && onBack != null) {
                TopAppBar(
                    title = { Text("Edit Interests") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isOnboarding) {
                Spacer(Modifier.height(24.dp))
                Text(
                    "\uD83C\uDFAF",
                    fontSize = 48.sp,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "What interests you?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = BarterTeal,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Select categories to personalize your feed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
            } else {
                Spacer(Modifier.height(8.dp))
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                vm.categories.forEach { cat ->
                    val selected = cat.id in state.selected
                    FilterChip(
                        selected = selected,
                        onClick = { vm.toggle(cat.id) },
                        label = {
                            Text("${cat.emoji} ${cat.label}")
                        },
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

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = vm::save,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.selected.isNotEmpty() && !state.saving,
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
                    Text(
                        if (isOnboarding) "Continue" else "Save",
                        Modifier.padding(vertical = 4.dp),
                    )
                }
            }

            if (state.selected.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Select at least one interest to continue",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
