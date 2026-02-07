package com.barter.core.presentation.vm

import com.barter.core.domain.model.EnrichedMatch
import com.barter.core.domain.usecase.LoadEnrichedMatchesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatListState(
    val loading: Boolean = false,
    val conversations: List<EnrichedMatch> = emptyList(),
    val error: String? = null,
)

class ChatListViewModel(
    private val loadEnrichedMatches: LoadEnrichedMatchesUseCase,
) : BaseViewModel() {

    private val _state = MutableStateFlow(ChatListState())
    val state: StateFlow<ChatListState> = _state.asStateFlow()

    fun load() {
        scope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { loadEnrichedMatches() }
                .onSuccess { matches ->
                    _state.value = _state.value.copy(
                        loading = false,
                        conversations = matches,
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(loading = false, error = it.message)
                }
        }
    }
}
