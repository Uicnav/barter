package com.barter.web

import androidx.compose.runtime.Composable
import com.barter.BarterAppRoot
import org.jetbrains.compose.web.renderComposable

/**
 * Web entrypoint (Wasm).
 */
fun main() {
    renderComposable(rootElementId = "root") {
        BarterAppRoot()
    }
}
