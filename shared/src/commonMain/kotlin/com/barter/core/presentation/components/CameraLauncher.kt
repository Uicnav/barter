package com.barter.core.presentation.components

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.PlatformFile

/**
 * Simple launcher abstraction for camera capture.
 * On Android this opens the real camera; on Desktop/Web it falls back to the gallery picker.
 */
interface CameraLauncher {
    fun launch()
}

@Composable
expect fun rememberCameraLauncher(onResult: (PlatformFile?) -> Unit): CameraLauncher
