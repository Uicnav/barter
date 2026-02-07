package com.barter.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberLocationPermissionLauncher(
    onGranted: () -> Unit,
    onDenied: () -> Unit,
): LocationPermissionLauncher {
    return remember {
        object : LocationPermissionLauncher {
            override fun launch() {
                onDenied()
            }
        }
    }
}
