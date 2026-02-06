package com.barter.core.presentation.vm

import com.barter.core.domain.model.AvailabilityStatus
import com.barter.core.domain.model.ListingKind
import com.barter.core.domain.model.PredefinedCategories
import com.barter.core.domain.model.THIRTY_DAYS_MS
import com.barter.core.domain.usecase.CreateListingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateListingState(
    val title: String = "",
    val description: String = "",
    val kind: ListingKind = ListingKind.GOODS,
    val selectedTags: Set<String> = emptySet(),
    val estimatedValue: String = "",
    val imageUrl: String = "",
    val availability: AvailabilityStatus = AvailabilityStatus.AVAILABLE,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
)

class CreateListingViewModel(
    private val createListing: CreateListingUseCase,
) : BaseViewModel() {

    private val _state = MutableStateFlow(CreateListingState())
    val state: StateFlow<CreateListingState> = _state.asStateFlow()

    val tagOptions = PredefinedCategories.all

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

    fun onAvailabilityChange(value: AvailabilityStatus) {
        _state.value = _state.value.copy(availability = value)
    }

    fun toggleTag(tagId: String) {
        val current = _state.value.selectedTags
        _state.value = _state.value.copy(
            selectedTags = if (tagId in current) current - tagId else current + tagId,
        )
    }

    fun submit() {
        val s = _state.value
        if (s.title.isBlank()) {
            _state.value = s.copy(error = "Title is required")
            return
        }
        if (s.description.isBlank()) {
            _state.value = s.copy(error = "Description is required")
            return
        }

        val value = s.estimatedValue.toDoubleOrNull()

        scope.launch {
            _state.value = _state.value.copy(saving = true, error = null)
            runCatching {
                createListing(
                    s.title.trim(),
                    s.description.trim(),
                    s.kind,
                    s.selectedTags.toList(),
                    value,
                    s.imageUrl.trim(),
                    System.currentTimeMillis() + THIRTY_DAYS_MS,
                )
            }.onSuccess {
                _state.value = CreateListingState(saved = true)
            }.onFailure { e ->
                _state.value = _state.value.copy(
                    saving = false,
                    error = e.message ?: "Failed to create listing",
                )
            }
        }
    }
}
