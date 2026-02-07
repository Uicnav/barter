package com.barter.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.barter.BarterAppRoot
import com.barter.core.data.AndroidSessionStorage
import com.barter.core.data.SessionStorage
import com.barter.core.domain.location.AndroidLocationProvider
import com.barter.core.domain.location.LocationProvider
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import org.koin.dsl.module

/**
 * Android entrypoint.
 * Shared UI is inside :shared -> BarterAppRoot()
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileKit.init(this)

        val platformModule = module {
            single<SessionStorage> {
                AndroidSessionStorage(applicationContext)
            }
            single<LocationProvider> {
                AndroidLocationProvider(applicationContext)
            }
        }

        setContent { BarterAppRoot(platformModule) }
    }
}
