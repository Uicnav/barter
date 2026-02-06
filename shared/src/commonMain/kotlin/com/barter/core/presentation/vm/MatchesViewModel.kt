package com.barter.core.presentation.vm

import com.barter.core.domain.model.EnrichedMatch
import com.barter.core.domain.usecase.LoadEnrichedMatchesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MatchesState(
    val loading: Boolean = false,
    val matches: List<EnrichedMatch> = emptyList(),
    val error: String? = null,
)

class MatchesViewModel(
    private val loadEnrichedMatches: LoadEnrichedMatchesUseCase,
) : BaseViewModel() {

    private val _state = MutableStateFlow(MatchesState())
    val state: StateFlow<MatchesState> = _state.asStateFlow()

    fun load() {
        scope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { loadEnrichedMatches() }
                .onSuccess { _state.value = _state.value.copy(loading = false, matches = it) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message) }
        }
    }
}
