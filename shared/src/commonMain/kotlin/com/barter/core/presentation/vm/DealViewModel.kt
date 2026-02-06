package com.barter.core.presentation.vm

import com.barter.core.domain.model.*
import com.barter.core.domain.repo.BarterRepository
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
)

class DealViewModel(
    private val repo: BarterRepository,
    private val updateStatus: UpdateDealStatusUseCase,
) : BaseViewModel() {

    private val _matchId = MutableStateFlow<String?>(null)
    private val _offer = MutableStateFlow("")
    private val _request = MutableStateFlow("")
    private val _offerValue = MutableStateFlow("")
    private val _requestValue = MutableStateFlow("")
    private val _cashTopUp = MutableStateFlow("")
    private val _note = MutableStateFlow("")

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val state: StateFlow<DealState> =
        combine(
            _matchId.flatMapLatest { id ->
                if (id == null) flowOf(emptyList()) else repo.observeDeals(id)
            },
            _offer,
            _request,
            combine(_offerValue, _requestValue, _cashTopUp, _note) { ov, rv, ct, n ->
                Quad(ov, rv, ct, n)
            },
        ) { deals, offer, request, (ov, rv, ct, n) ->
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
    fun complete(dealId: String) = setStatus(dealId, DealStatus.COMPLETED)

    private fun setStatus(dealId: String, status: DealStatus) {
        scope.launch { updateStatus(dealId, status) }
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
