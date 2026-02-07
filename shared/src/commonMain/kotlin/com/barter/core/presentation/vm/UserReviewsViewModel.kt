package com.barter.core.presentation.vm

import com.barter.core.domain.model.Review
import com.barter.core.domain.usecase.GetUserReviewsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserReviewsState(
    val loading: Boolean = false,
    val reviews: List<Review> = emptyList(),
    val averageRating: Double = 0.0,
    val error: String? = null,
)

class UserReviewsViewModel(
    private val getReviews: GetUserReviewsUseCase,
) : BaseViewModel() {

    private val _state = MutableStateFlow(UserReviewsState())
    val state: StateFlow<UserReviewsState> = _state.asStateFlow()

    fun load(userId: String) {
        scope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { getReviews(userId) }
                .onSuccess { reviews ->
                    val avg = if (reviews.isNotEmpty()) {
                        reviews.map { it.rating }.average()
                    } else 0.0
                    _state.value = _state.value.copy(
                        loading = false,
                        reviews = reviews,
                        averageRating = kotlin.math.round(avg * 10) / 10.0,
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(loading = false, error = it.message)
                }
        }
    }
}
