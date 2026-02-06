package com.barter.core.presentation.vm

import com.barter.core.domain.model.InterestCategory
import com.barter.core.domain.model.Listing
import com.barter.core.domain.model.PredefinedCategories
import com.barter.core.domain.model.SortOption
import com.barter.core.domain.model.SwipeAction
import com.barter.core.domain.usecase.SearchListingsUseCase
import com.barter.core.domain.usecase.SwipeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BrowseState(
    val query: String = "",
    val selectedCategory: String? = null,
    val sortBy: SortOption = SortOption.NEWEST,
    val listings: List<Listing> = emptyList(),
    val categories: List<InterestCategory> = PredefinedCategories.all,
    val loading: Boolean = false,
    val error: String? = null,
    val likedIds: Set<String> = emptySet(),
    val likingId: String? = null,
    val lastMatchId: String? = null,
)

class BrowseViewModel(
    private val searchListings: SearchListingsUseCase,
    private val swipeUseCase: SwipeUseCase,
) : BaseViewModel() {

    private val _state = MutableStateFlow(BrowseState())
    val state: StateFlow<BrowseState> = _state.asStateFlow()

    fun load() {
        search()
    }

    fun onQueryChange(value: String) {
        _state.value = _state.value.copy(query = value)
    }

    fun search() {
        val s = _state.value
        scope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching {
                searchListings(s.query.trim(), s.selectedCategory, s.sortBy)
            }.onSuccess { results ->
                _state.value = _state.value.copy(loading = false, listings = results)
            }.onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun selectCategory(categoryId: String?) {
        _state.value = _state.value.copy(selectedCategory = categoryId)
        search()
    }

    fun setSortBy(sort: SortOption) {
        _state.value = _state.value.copy(sortBy = sort)
        search()
    }

    fun likeListing(listingId: String) {
        if (_state.value.likedIds.contains(listingId) || _state.value.likingId != null) return
        scope.launch {
            _state.value = _state.value.copy(likingId = listingId)
            runCatching {
                swipeUseCase(listingId, SwipeAction.LIKE)
            }.onSuccess { match ->
                _state.value = _state.value.copy(
                    likingId = null,
                    likedIds = _state.value.likedIds + listingId,
                    lastMatchId = match?.id,
                )
            }.onFailure {
                _state.value = _state.value.copy(likingId = null)
            }
        }
    }

    fun dismissMatch() {
        _state.value = _state.value.copy(lastMatchId = null)
    }
}
