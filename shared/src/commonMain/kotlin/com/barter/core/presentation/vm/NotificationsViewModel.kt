package com.barter.core.presentation.vm

import com.barter.core.domain.model.Notification
import com.barter.core.domain.usecase.LoadNotificationsUseCase
import com.barter.core.domain.usecase.MarkNotificationReadUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationsState(
    val notifications: List<Notification> = emptyList(),
    val loading: Boolean = false,
)

class NotificationsViewModel(
    private val loadNotifications: LoadNotificationsUseCase,
    private val markRead: MarkNotificationReadUseCase,
) : BaseViewModel() {

    private val _state = MutableStateFlow(NotificationsState())
    val state: StateFlow<NotificationsState> = _state.asStateFlow()

    fun load() {
        scope.launch {
            _state.value = _state.value.copy(loading = true)
            runCatching { loadNotifications() }
                .onSuccess { list ->
                    _state.value = NotificationsState(notifications = list, loading = false)
                }
                .onFailure {
                    _state.value = _state.value.copy(loading = false)
                }
        }
    }

    fun markAsRead(notificationId: String) {
        scope.launch {
            runCatching { markRead(notificationId) }
            // Refresh list
            runCatching { loadNotifications() }
                .onSuccess { list ->
                    _state.value = _state.value.copy(notifications = list)
                }
        }
    }
}
