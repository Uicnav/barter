package com.barter.core.presentation.vm

import com.barter.core.domain.model.Listing
import com.barter.core.domain.model.SwipeAction
import com.barter.core.domain.usecase.GetListingByIdUseCase
import com.barter.core.domain.usecase.SwipeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ListingDetailState(
    val loading: Boolean = false,
    val listing: Listing? = null,
    val error: String? = null,
    val isLiked: Boolean = false,
    val isLiking: Boolean = false,
    val isPassed: Boolean = false,
    val lastMatchId: String? = null,
)

class ListingDetailViewModel(
    private val getListingById: GetListingByIdUseCase,
    private val swipeUseCase: SwipeUseCase,
) : BaseViewModel() {

    private val _state = MutableStateFlow(ListingDetailState())
    val state: StateFlow<ListingDetailState> = _state.asStateFlow()

    fun load(listingId: String) {
        scope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { getListingById(listingId) }
                .onSuccess { _state.value = _state.value.copy(loading = false, listing = it) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message) }
        }
    }

    fun likeListing() {
        val listingId = _state.value.listing?.id ?: return
        if (_state.value.isLiked || _state.value.isLiking) return
        scope.launch {
            _state.value = _state.value.copy(isLiking = true)
            runCatching { swipeUseCase(listingId, SwipeAction.LIKE) }
                .onSuccess { match ->
                    _state.value = _state.value.copy(
                        isLiking = false,
                        isLiked = true,
                        lastMatchId = match?.id,
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(isLiking = false)
                }
        }
    }

    fun passListing() {
        val listingId = _state.value.listing?.id ?: return
        scope.launch {
            runCatching { swipeUseCase(listingId, SwipeAction.PASS) }
            _state.value = _state.value.copy(isPassed = true)
        }
    }

    fun dismissMatch() {
        _state.value = _state.value.copy(lastMatchId = null)
    }
}
