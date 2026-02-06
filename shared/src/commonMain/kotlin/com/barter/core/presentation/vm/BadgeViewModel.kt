package com.barter.core.presentation.vm

import com.barter.core.domain.usecase.GetUnreadCountsUseCase
import com.barter.core.domain.usecase.GetUnreadNotificationCountUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BadgeState(
    val matchCount: Int = 0,
    val unreadMessageCount: Int = 0,
    val notificationCount: Int = 0,
)

class BadgeViewModel(
    private val unreadCounts: GetUnreadCountsUseCase,
    private val unreadNotifications: GetUnreadNotificationCountUseCase,
) : BaseViewModel() {

    private val _state = MutableStateFlow(BadgeState())
    val state: StateFlow<BadgeState> = _state.asStateFlow()

    fun refresh() {
        scope.launch {
            runCatching {
                val matches = unreadCounts.matchCount()
                val messages = unreadCounts.messageCount()
                val notifications = unreadNotifications()
                _state.value = BadgeState(
                    matchCount = matches,
                    unreadMessageCount = messages,
                    notificationCount = notifications,
                )
            }
        }
    }
}
