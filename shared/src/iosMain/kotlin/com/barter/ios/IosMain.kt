package com.barter.ios

import androidx.compose.ui.window.ComposeUIViewController
import com.barter.BarterAppRoot
import com.barter.core.data.IosSessionStorage
import com.barter.core.data.SessionStorage
import com.barter.core.domain.location.IosLocationProvider
import com.barter.core.domain.location.LocationProvider
import org.koin.dsl.module

fun MainViewController() = ComposeUIViewController {
    val platformModule = module {
        single<SessionStorage> { IosSessionStorage() }
        single<LocationProvider> { IosLocationProvider() }
    }
    BarterAppRoot(platformModule)
}
