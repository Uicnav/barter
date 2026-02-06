package com.barter.core.presentation.vm

import com.barter.core.domain.model.AvailabilityStatus
import com.barter.core.domain.model.ListingKind
import com.barter.core.domain.model.ListingStatus
import com.barter.core.domain.model.PredefinedCategories
import com.barter.core.domain.model.status
import com.barter.core.domain.usecase.DeleteListingUseCase
import com.barter.core.domain.usecase.GetListingByIdUseCase
import com.barter.core.domain.usecase.RenewListingUseCase
import com.barter.core.domain.usecase.ToggleListingVisibilityUseCase
import com.barter.core.domain.usecase.UpdateListingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditListingState(
    val loading: Boolean = false,
    val title: String = "",
    val description: String = "",
    val kind: ListingKind = ListingKind.GOODS,
    val selectedTags: Set<String> = emptySet(),
    val estimatedValue: String = "",
    val imageUrl: String = "",
    val validUntilMs: Long? = null,
    val listingStatus: ListingStatus = ListingStatus.ACTIVE,
    val availability: AvailabilityStatus = AvailabilityStatus.AVAILABLE,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val error: String? = null,
)

class EditListingViewModel(
    private val getListingById: GetListingByIdUseCase,
    private val updateListing: UpdateListingUseCase,
    private val deleteListingUseCase: DeleteListingUseCase,
    private val toggleVisibilityUseCase: ToggleListingVisibilityUseCase,
    private val renewListingUseCase: RenewListingUseCase,
) : BaseViewModel() {

    private val _state = MutableStateFlow(EditListingState())
    val state: StateFlow<EditListingState> = _state.asStateFlow()

    val tagOptions = PredefinedCategories.all

    private var listingId: String = ""

    fun loadListing(id: String) {
        listingId = id
        scope.launch {
            _state.value = _state.value.copy(loading = true)
            runCatching { getListingById(id) }
                .onSuccess { listing ->
                    if (listing != null) {
                        _state.value = _state.value.copy(
                            loading = false,
                            title = listing.title,
                            description = listing.description,
                            kind = listing.kind,
                            selectedTags = listing.tags.toSet(),
                            estimatedValue = listing.estimatedValue?.toString() ?: "",
                            imageUrl = listing.imageUrl,
                            validUntilMs = listing.validUntilMs,
                            listingStatus = listing.status,
                            availability = listing.availability,
                        )
                    } else {
                        _state.value = _state.value.copy(loading = false, error = "Listing not found")
                    }
                }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message) }
        }
    }

    fun onTitleChange(value: String) {
        _state.value = _state.value.copy(title = value, error = null)
    }

    fun onDescriptionChange(value: String) {
        _state.value = _state.value.copy(description = value, error = null)
    }

    fun onKindChange(kind: ListingKind) {
        _state.value = _state.value.copy(kind = kind)
    }

    fun onEstimatedValueChange(value: String) {
        _state.value = _state.value.copy(estimatedValue = value, error = null)
    }

    fun onImageUrlChange(value: String) {
        _state.value = _state.value.copy(imageUrl = value)
    }

    fun toggleTag(tagId: String) {
        val current = _state.value.selectedTags
        _state.value = _state.value.copy(
            selectedTags = if (tagId in current) current - tagId else current + tagId,
        )
    }

    fun onAvailabilityChange(value: AvailabilityStatus) {
        _state.value = _state.value.copy(availability = value)
    }

    fun toggleVisibility() {
        scope.launch {
            _state.value = _state.value.copy(saving = true, error = null)
            runCatching { toggleVisibilityUseCase(listingId) }
                .onSuccess { updated ->
                    _state.value = _state.value.copy(
                        saving = false,
                        listingStatus = updated.status,
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(saving = false, error = it.message)
                }
        }
    }

    fun renewListing() {
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        val newExpiry = System.currentTimeMillis() + thirtyDaysMs
        scope.launch {
            _state.value = _state.value.copy(saving = true, error = null)
            runCatching { renewListingUseCase(listingId, newExpiry) }
                .onSuccess { updated ->
                    _state.value = _state.value.copy(
                        saving = false,
                        validUntilMs = updated.validUntilMs,
                        listingStatus = updated.status,
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(saving = false, error = it.message)
                }
        }
    }

    fun save() {
        val s = _state.value
        if (s.title.isBlank()) {
            _state.value = s.copy(error = "Title is required")
            return
        }
        if (s.description.isBlank()) {
            _state.value = s.copy(error = "Description is required")
            return
        }

        scope.launch {
            _state.value = _state.value.copy(saving = true, error = null)
            runCatching {
                updateListing(
                    listingId, s.title.trim(), s.description.trim(),
                    s.kind, s.selectedTags.toList(), s.estimatedValue.toDoubleOrNull(),
                    s.imageUrl.trim(), s.validUntilMs,
                )
            }.onSuccess {
                _state.value = _state.value.copy(saving = false, saved = true)
            }.onFailure { e ->
                _state.value = _state.value.copy(saving = false, error = e.message ?: "Failed to save")
            }
        }
    }

    fun requestDelete() {
        _state.value = _state.value.copy(showDeleteDialog = true)
    }

    fun dismissDelete() {
        _state.value = _state.value.copy(showDeleteDialog = false)
    }

    fun confirmDelete() {
        scope.launch {
            _state.value = _state.value.copy(showDeleteDialog = false, saving = true)
            runCatching { deleteListingUseCase(listingId) }
                .onSuccess { _state.value = _state.value.copy(saving = false, deleted = true) }
                .onFailure { _state.value = _state.value.copy(saving = false, error = it.message) }
        }
    }
}
