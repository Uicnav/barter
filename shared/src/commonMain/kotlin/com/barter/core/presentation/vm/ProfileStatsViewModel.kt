package com.barter.core.presentation.vm

import com.barter.core.domain.model.ProfileStats
import com.barter.core.domain.usecase.LoadProfileStatsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileStatsViewModel(
    private val loadProfileStats: LoadProfileStatsUseCase,
) : BaseViewModel() {

    private val _stats = MutableStateFlow(ProfileStats())
    val stats: StateFlow<ProfileStats> = _stats.asStateFlow()

    fun load() {
        scope.launch {
            runCatching { loadProfileStats() }
                .onSuccess { _stats.value = it }
        }
    }
}
