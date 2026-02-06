package com.barter.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.barter.BarterAppRoot
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init

/**
 * Android entrypoint.
 * Shared UI is inside :shared -> BarterAppRoot()
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileKit.init(this)
        setContent { BarterAppRoot() }
    }
}
