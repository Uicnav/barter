package com.barter.core.presentation.vm

import com.barter.core.domain.model.InterestCategory
import com.barter.core.domain.model.Listing
import com.barter.core.domain.model.PredefinedCategories
import com.barter.core.domain.model.SortOption
import com.barter.core.domain.model.SwipeAction
import com.barter.core.domain.repo.BarterRepository
import com.barter.core.domain.usecase.SearchListingsUseCase
import com.barter.core.domain.usecase.SwipeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.*

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
    val distances: Map<String, Double> = emptyMap(),
)

class BrowseViewModel(
    private val searchListings: SearchListingsUseCase,
    private val swipeUseCase: SwipeUseCase,
    private val repo: BarterRepository,
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
                val results = searchListings(s.query.trim(), s.selectedCategory, s.sortBy)
                val user = repo.currentUser.first()
                val distances = computeDistances(user.latitude, user.longitude, results)
                results to distances
            }.onSuccess { (results, distances) ->
                _state.value = _state.value.copy(
                    loading = false, listings = results, distances = distances,
                )
            }.onFailure { e ->
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    private fun computeDistances(
        userLat: Double?, userLng: Double?, listings: List<Listing>,
    ): Map<String, Double> {
        if (userLat == null || userLng == null) return emptyMap()
        return listings.mapNotNull { listing ->
            val oLat = listing.owner.latitude ?: return@mapNotNull null
            val oLng = listing.owner.longitude ?: return@mapNotNull null
            listing.id to haversineKm(userLat, userLng, oLat, oLng)
        }.toMap()
    }

    private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = (lat2 - lat1) * PI / 180.0
        val dLng = (lng2 - lng1) * PI / 180.0
        val a = sin(dLat / 2).pow(2) +
            cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) *
            sin(dLng / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
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
