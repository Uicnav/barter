package com.barter.core.presentation.vm

import com.barter.core.domain.model.Message
import com.barter.core.domain.repo.BarterRepository
import com.barter.core.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatState(
    val messages: List<Message> = emptyList(),
    val draft: String = "",
)

class ChatViewModel(
    private val repo: BarterRepository,
) : BaseViewModel() {

    private val _matchId = MutableStateFlow<String?>(null)
    private val _draft = MutableStateFlow("")

    val state: StateFlow<ChatState> =
        combine(
            _matchId.flatMapLatest { id ->
                if (id == null) flowOf(emptyList()) else repo.observeMessages(id)
            },
            _draft
        ) { messages, draft ->
            ChatState(messages = messages, draft = draft)
        }.stateIn(scope, SharingStarted.Eagerly, ChatState())

    fun bind(matchId: String) {
        _matchId.value = matchId
    }

    fun onDraftChange(text: String) {
        _draft.value = text
    }

    fun send() {
        val matchId = _matchId.value ?: return
        val text = _draft.value.trim()
        if (text.isEmpty()) return

        scope.launch {
            repo.sendMessage(matchId, text)
            _draft.value = ""
        }
    }
}
