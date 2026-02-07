package com.barter.core.presentation.vm

import com.barter.core.domain.model.*
import com.barter.core.domain.repo.BarterRepository
import com.barter.core.domain.usecase.SubmitReviewUseCase
import com.barter.core.domain.usecase.UpdateDealStatusUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.abs

data class DealState(
    val deals: List<Deal> = emptyList(),
    val offerText: String = "",
    val requestText: String = "",
    val offerValue: String = "",
    val requestValue: String = "",
    val cashTopUp: String = "",
    val note: String = "",
    val valueSummary: DealValueSummary? = null,
    val showReviewDialog: Boolean = false,
    val reviewDealId: String? = null,
    val reviewRating: Int = 0,
    val reviewComment: String = "",
    val reviewSubmitting: Boolean = false,
    val reviewSubmitted: Boolean = false,
)

class DealViewModel(
    private val repo: BarterRepository,
    private val updateStatus: UpdateDealStatusUseCase,
    private val submitReviewUseCase: SubmitReviewUseCase,
) : BaseViewModel() {

    private val _matchId = MutableStateFlow<String?>(null)
    private val _offer = MutableStateFlow("")
    private val _request = MutableStateFlow("")
    private val _offerValue = MutableStateFlow("")
    private val _requestValue = MutableStateFlow("")
    private val _cashTopUp = MutableStateFlow("")
    private val _note = MutableStateFlow("")
    private val _reviewState = MutableStateFlow(ReviewDialogState())

    private data class ReviewDialogState(
        val show: Boolean = false,
        val dealId: String? = null,
        val rating: Int = 0,
        val comment: String = "",
        val submitting: Boolean = false,
        val submitted: Boolean = false,
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val state: StateFlow<DealState> =
        combine(
            combine(
                _matchId.flatMapLatest { id ->
                    if (id == null) flowOf(emptyList()) else repo.observeDeals(id)
                },
                _offer,
                _request,
                combine(_offerValue, _requestValue, _cashTopUp, _note) { ov, rv, ct, n ->
                    Quad(ov, rv, ct, n)
                },
            ) { deals, offer, request, quad -> Triple(deals, offer, Pair(request, quad)) },
            _reviewState,
        ) { (deals, offer, reqQuad), review ->
            val (request, quad) = reqQuad
            val (ov, rv, ct, n) = quad
            val offerTotal = ov.toDoubleOrNull() ?: 0.0
            val requestTotal = rv.toDoubleOrNull() ?: 0.0
            val diff = offerTotal - requestTotal
            val summary = if (ov.isNotBlank() || rv.isNotBlank()) {
                DealValueSummary(
                    offerTotal = offerTotal,
                    requestTotal = requestTotal,
                    difference = diff,
                    suggestedTopUp = if (diff < 0) abs(diff) else 0.0,
                    isFair = abs(diff) <= (offerTotal + requestTotal) * 0.1,
                )
            } else null

            DealState(
                deals = deals, offerText = offer, requestText = request,
                offerValue = ov, requestValue = rv, cashTopUp = ct,
                note = n, valueSummary = summary,
                showReviewDialog = review.show,
                reviewDealId = review.dealId,
                reviewRating = review.rating,
                reviewComment = review.comment,
                reviewSubmitting = review.submitting,
                reviewSubmitted = review.submitted,
            )
        }.stateIn(scope, SharingStarted.Eagerly, DealState())

    fun bind(matchId: String) {
        _matchId.value = matchId
    }

    fun onOfferChange(text: String) { _offer.value = text }
    fun onRequestChange(text: String) { _request.value = text }
    fun onOfferValueChange(text: String) { _offerValue.value = text }
    fun onRequestValueChange(text: String) { _requestValue.value = text }
    fun onCashTopUpChange(text: String) { _cashTopUp.value = text }
    fun onNoteChange(text: String) { _note.value = text }

    fun propose() {
        val matchId = _matchId.value ?: return
        val offer = _offer.value.trim()
        val request = _request.value.trim()
        if (offer.isEmpty() || request.isEmpty()) return

        val offerVal = _offerValue.value.toDoubleOrNull()
        val requestVal = _requestValue.value.toDoubleOrNull()
        val topUp = _cashTopUp.value.toDoubleOrNull() ?: 0.0
        val note = _note.value.trim()

        val offerItems = offer.split(",").mapNotNull { s ->
            val t = s.trim()
            if (t.isEmpty()) null else DealItem(id = "o_$t", title = t, kind = ListingKind.BOTH, estimatedValue = offerVal)
        }
        val requestItems = request.split(",").mapNotNull { s ->
            val t = s.trim()
            if (t.isEmpty()) null else DealItem(id = "r_$t", title = t, kind = ListingKind.BOTH, estimatedValue = requestVal)
        }

        scope.launch {
            repo.proposeDeal(matchId, offerItems, requestItems, topUp, note)
            _offer.value = ""
            _request.value = ""
            _offerValue.value = ""
            _requestValue.value = ""
            _cashTopUp.value = ""
            _note.value = ""
        }
    }

    fun accept(dealId: String) = setStatus(dealId, DealStatus.ACCEPTED)
    fun reject(dealId: String) = setStatus(dealId, DealStatus.REJECTED)

    fun complete(dealId: String) {
        scope.launch {
            updateStatus(dealId, DealStatus.COMPLETED)
            _reviewState.value = ReviewDialogState(show = true, dealId = dealId)
        }
    }

    private fun setStatus(dealId: String, status: DealStatus) {
        scope.launch { updateStatus(dealId, status) }
    }

    // ── Review dialog ──────────────────────────────────────

    fun onReviewRatingChange(rating: Int) {
        _reviewState.value = _reviewState.value.copy(rating = rating)
    }

    fun onReviewCommentChange(text: String) {
        _reviewState.value = _reviewState.value.copy(comment = text)
    }

    fun submitReview() {
        val rs = _reviewState.value
        val dealId = rs.dealId ?: return
        if (rs.rating < 1) return
        scope.launch {
            _reviewState.value = rs.copy(submitting = true)
            runCatching { submitReviewUseCase(dealId, rs.rating, rs.comment) }
                .onSuccess {
                    _reviewState.value = ReviewDialogState(submitted = true)
                }
                .onFailure {
                    _reviewState.value = rs.copy(submitting = false)
                }
        }
    }

    fun dismissReviewDialog() {
        _reviewState.value = ReviewDialogState()
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
