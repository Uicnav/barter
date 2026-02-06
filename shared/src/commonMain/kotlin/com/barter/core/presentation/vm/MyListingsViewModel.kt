package com.barter.core.presentation.vm

import com.barter.core.domain.model.AvailabilityStatus
import com.barter.core.domain.model.Listing
import com.barter.core.domain.model.status
import com.barter.core.domain.usecase.DeleteListingUseCase
import com.barter.core.domain.usecase.LoadMyListingsUseCase
import com.barter.core.domain.usecase.RenewListingUseCase
import com.barter.core.domain.usecase.ToggleListingVisibilityUseCase
import com.barter.core.domain.usecase.UpdateAvailabilityUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MyListingsState(
    val loading: Boolean = false,
    val listings: List<Listing> = emptyList(),
    val deletingId: String? = null,
    val error: String? = null,
    val renewalError: String? = null,
)

class MyListingsViewModel(
    private val loadMyListings: LoadMyListingsUseCase,
    private val deleteListingUseCase: DeleteListingUseCase,
    private val toggleVisibilityUseCase: ToggleListingVisibilityUseCase,
    private val renewListingUseCase: RenewListingUseCase,
    private val updateAvailabilityUseCase: UpdateAvailabilityUseCase,
) : BaseViewModel() {

    private val _state = MutableStateFlow(MyListingsState())
    val state: StateFlow<MyListingsState> = _state.asStateFlow()

    fun load() {
        scope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { loadMyListings() }
                .onSuccess { _state.value = _state.value.copy(loading = false, listings = it) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message) }
        }
    }

    fun deleteListing(listingId: String) {
        scope.launch {
            _state.value = _state.value.copy(deletingId = listingId)
            runCatching { deleteListingUseCase(listingId) }
                .onSuccess {
                    _state.value = _state.value.copy(
                        deletingId = null,
                        listings = _state.value.listings.filter { it.id != listingId },
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(deletingId = null, error = it.message)
                }
        }
    }

    fun toggleVisibility(listingId: String) {
        scope.launch {
            runCatching { toggleVisibilityUseCase(listingId) }
                .onSuccess { updated ->
                    _state.value = _state.value.copy(
                        listings = _state.value.listings.map {
                            if (it.id == listingId) updated else it
                        },
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(error = it.message)
                }
        }
    }

    fun renewListing(listingId: String) {
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        val newExpiry = System.currentTimeMillis() + thirtyDaysMs
        scope.launch {
            runCatching { renewListingUseCase(listingId, newExpiry) }
                .onSuccess { updated ->
                    _state.value = _state.value.copy(
                        renewalError = null,
                        listings = _state.value.listings.map {
                            if (it.id == listingId) updated else it
                        },
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(renewalError = it.message)
                }
        }
    }

    fun dismissRenewalError() {
        _state.value = _state.value.copy(renewalError = null)
    }

    fun cycleAvailability(listingId: String) {
        val listing = _state.value.listings.firstOrNull { it.id == listingId } ?: return
        val next = when (listing.availability) {
            AvailabilityStatus.AVAILABLE -> AvailabilityStatus.RESERVED
            AvailabilityStatus.RESERVED -> AvailabilityStatus.SOLD
            AvailabilityStatus.SOLD -> AvailabilityStatus.AVAILABLE
        }
        scope.launch {
            runCatching { updateAvailabilityUseCase(listingId, next) }
                .onSuccess { updated ->
                    _state.value = _state.value.copy(
                        listings = _state.value.listings.map {
                            if (it.id == listingId) updated else it
                        },
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(error = it.message)
                }
        }
    }
}
