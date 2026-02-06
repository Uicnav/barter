package com.barter.core.presentation.components

import androidx.compose.runtime.Composable

interface LocationPermissionLauncher {
    fun launch()
}

@Composable
expect fun rememberLocationPermissionLauncher(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
): LocationPermissionLauncher
