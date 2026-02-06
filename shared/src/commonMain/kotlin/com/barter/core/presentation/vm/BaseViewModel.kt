package com.barter.core.presentation.vm

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * KMP-friendly ViewModel:
 * - no AndroidX dependency
 * - works on Android, Desktop, Web
 *
 * In a real app, you can add:
 * - close() called from platform lifecycle
 * - structured cancellation
 */
open class BaseViewModel {
    protected val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    open fun clear() {
        // Optionally cancel scope if you manage lifecycle.
        // scope.cancel()
    }
}
