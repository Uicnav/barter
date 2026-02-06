package com.barter.desktop

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.barter.BarterAppRoot

/**
 * Desktop entrypoint.
 */
fun main() = application {
    val iconStream = Thread.currentThread().contextClassLoader?.getResourceAsStream("icon.png")
    @Suppress("DEPRECATION")
    val icon = iconStream?.let { BitmapPainter(loadImageBitmap(it)) }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Barter",
        icon = icon,
    ) {
        BarterAppRoot()
    }
}
