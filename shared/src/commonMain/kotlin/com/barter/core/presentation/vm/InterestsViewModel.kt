package com.barter.core.presentation.vm

import com.barter.core.domain.model.PredefinedCategories
import com.barter.core.domain.repo.BarterRepository
import com.barter.core.domain.usecase.UpdateInterestsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class InterestsState(
    val selected: Set<String> = emptySet(),
    val saving: Boolean = false,
    val saved: Boolean = false,
)

class InterestsViewModel(
    private val repo: BarterRepository,
    private val updateInterests: UpdateInterestsUseCase,
) : BaseViewModel() {

    private val _state = MutableStateFlow(InterestsState())
    val state: StateFlow<InterestsState> = _state.asStateFlow()

    val categories = PredefinedCategories.all

    fun loadCurrentInterests() {
        scope.launch {
            val user = repo.currentUser.first()
            if (user.interests.isNotEmpty()) {
                _state.value = _state.value.copy(selected = user.interests.toSet())
            }
        }
    }

    fun toggle(categoryId: String) {
        val current = _state.value.selected
        _state.value = _state.value.copy(
            selected = if (categoryId in current) current - categoryId else current + categoryId,
        )
    }

    fun save() {
        scope.launch {
            _state.value = _state.value.copy(saving = true)
            updateInterests(_state.value.selected.toList())
            _state.value = _state.value.copy(saving = false, saved = true)
        }
    }
}
