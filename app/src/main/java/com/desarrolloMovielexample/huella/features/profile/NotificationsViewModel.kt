package com.desarrolloMovielexample.huella.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.utils.UiText
import com.desarrolloMovielexample.huella.core.utils.uiText
import com.desarrolloMovielexample.huella.domain.model.AppNotification
import com.desarrolloMovielexample.huella.data.remote.SupabaseRepository
import com.desarrolloMovielexample.huella.domain.repository.HuellaRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn

data class NotificationsUiState(
    /** Grupos "Hoy", "Ayer", "Esta semana", "Antes" en orden de aparición (la lista llega por created_at desc). */
    val groups: List<Pair<String, List<AppNotification>>> = emptyList(),
) {
    val isEmpty: Boolean get() = groups.isEmpty()
    val hasUnread: Boolean get() = groups.any { (_, items) -> items.any { !it.read } }
}

class NotificationsViewModel(private val repo: HuellaRepository = SupabaseRepository) : ViewModel() {
    private fun toState(list: List<AppNotification>) = NotificationsUiState(list.groupBy { it.group }.toList())

    val uiState: StateFlow<NotificationsUiState> = repo.notifications
        .map(::toState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), toState(repo.notifications.value))

    private val _messages = Channel<UiText>(Channel.BUFFERED)
    val messages: Flow<UiText> = _messages.receiveAsFlow()

    fun markAsRead(id: String) {
        repo.markNotificationRead(id)
    }

    fun deleteNotification(id: String) {
        repo.deleteNotification(id)
        _messages.trySend(uiText(R.string.msg_notification_deleted))
    }

    fun markAllRead() {
        if (!uiState.value.hasUnread) {
            _messages.trySend(uiText(R.string.msg_no_unread))
            return
        }
        repo.markAllNotificationsRead()
        _messages.trySend(uiText(R.string.msg_marked_read))
    }
}
