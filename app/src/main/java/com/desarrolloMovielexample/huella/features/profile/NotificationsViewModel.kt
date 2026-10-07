package com.desarrolloMovielexample.huella.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desarrolloMovielexample.huella.domain.model.AppNotification
import com.desarrolloMovielexample.huella.domain.repository.FakeRepository
import com.desarrolloMovielexample.huella.domain.repository.HuellaRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn

data class NotificationsUiState(
    /** Ordered groups: "Hoy", "Ayer", "Esta semana" (order of first appearance). */
    val groups: List<Pair<String, List<AppNotification>>> = emptyList(),
) {
    val isEmpty: Boolean get() = groups.isEmpty()
    val hasUnread: Boolean get() = groups.any { (_, items) -> items.any { !it.read } }
}

class NotificationsViewModel(private val repo: HuellaRepository = FakeRepository) : ViewModel() {
    private fun toState(list: List<AppNotification>) = NotificationsUiState(list.groupBy { it.group }.toList())

    val uiState: StateFlow<NotificationsUiState> = repo.notifications
        .map(::toState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), toState(repo.notifications.value))

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    fun markAllRead() {
        if (!uiState.value.hasUnread) {
            _messages.trySend("No tienes notificaciones sin leer")
            return
        }
        repo.markAllNotificationsRead()
        _messages.trySend("Notificaciones marcadas como leídas")
    }
}
