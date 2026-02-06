package com.barter.core.presentation.components

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher

@Composable
actual fun rememberCameraLauncher(onResult: (PlatformFile?) -> Unit): CameraLauncher {
    val launcher = rememberFilePickerLauncher(
        type = FileKitType.Image,
        title = "Pick a photo",
        onResult = onResult,
    )
    return object : CameraLauncher {
        override fun launch() {
            launcher.launch()
        }
    }
}
