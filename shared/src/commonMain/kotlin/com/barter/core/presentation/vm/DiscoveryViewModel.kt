package com.barter.core.presentation.vm

import com.barter.core.domain.model.InterestCategory
import com.barter.core.domain.model.Listing
import com.barter.core.domain.model.PredefinedCategories
import com.barter.core.domain.model.SwipeAction
import com.barter.core.domain.repo.BarterRepository
import com.barter.core.domain.usecase.LoadDiscoveryUseCase
import com.barter.core.domain.usecase.SwipeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class DiscoveryState(
    val loading: Boolean = false,
    val cards: List<Listing> = emptyList(),
    val allCards: List<Listing> = emptyList(),
    val categories: List<InterestCategory> = emptyList(),
    val selectedCategory: String? = null,
    val lastSwipedCard: Listing? = null,
    val lastMatchId: String? = null,
    val error: String? = null,
)

class DiscoveryViewModel(
    private val loadDiscovery: LoadDiscoveryUseCase,
    private val swipeUseCase: SwipeUseCase,
    private val repo: BarterRepository,
) : BaseViewModel() {

    private val _state = MutableStateFlow(DiscoveryState())
    val state: StateFlow<DiscoveryState> = _state.asStateFlow()

    fun load() {
        scope.launch {
            _state.value = _state.value.copy(loading = true, error = null, lastMatchId = null)
            runCatching {
                val user = repo.currentUser.first()
                loadDiscovery(user.interests)
            }.onSuccess { cards ->
                val tagSet = cards.flatMap { it.tags }.toSet()
                val categories = PredefinedCategories.all.filter { it.id in tagSet }
                _state.value = _state.value.copy(
                    loading = false,
                    cards = cards,
                    allCards = cards,
                    categories = categories,
                    selectedCategory = null,
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun selectCategory(categoryId: String?) {
        val all = _state.value.allCards
        val filtered = if (categoryId == null) all
        else all.filter { listing -> listing.tags.any { it == categoryId } }
        _state.value = _state.value.copy(
            selectedCategory = categoryId,
            cards = filtered,
        )
    }

    fun likeTopCard() = swipeTopCard(SwipeAction.LIKE)
    fun passTopCard() = swipeTopCard(SwipeAction.PASS)

    fun dismissMatch() {
        _state.value = _state.value.copy(lastMatchId = null)
    }

    fun undoLastSwipe() {
        val last = _state.value.lastSwipedCard ?: return
        _state.value = _state.value.copy(
            cards = listOf(last) + _state.value.cards,
            lastSwipedCard = null,
        )
    }

    private fun swipeTopCard(action: SwipeAction) {
        val top = _state.value.cards.firstOrNull() ?: return

        scope.launch {
            val match = runCatching { swipeUseCase(top.id, action) }.getOrNull()
            val remaining = _state.value.cards.drop(1)

            _state.value = _state.value.copy(
                cards = remaining,
                lastSwipedCard = top,
                lastMatchId = match?.id,
            )
        }
    }
}
