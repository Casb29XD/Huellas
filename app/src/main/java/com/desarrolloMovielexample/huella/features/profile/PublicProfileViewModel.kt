package com.desarrolloMovielexample.huella.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.utils.UiText
import com.desarrolloMovielexample.huella.core.utils.toUiText
import com.desarrolloMovielexample.huella.core.utils.uiText
import com.desarrolloMovielexample.huella.domain.model.Publication
import com.desarrolloMovielexample.huella.domain.model.User
import com.desarrolloMovielexample.huella.data.remote.SupabaseRepository
import com.desarrolloMovielexample.huella.domain.repository.HuellaRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

data class PublicProfileUiState(
    val isLoaded: Boolean = false,
    val user: User? = null,
    val publications: List<Publication> = emptyList(),
)

class PublicProfileViewModel(private val repo: HuellaRepository = SupabaseRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(PublicProfileUiState())
    val uiState: StateFlow<PublicProfileUiState> = _uiState.asStateFlow()

    private val _messages = Channel<UiText>(Channel.BUFFERED)
    val messages: Flow<UiText> = _messages.receiveAsFlow()
    private val _openChats = Channel<String>(Channel.BUFFERED)
    val openChats: Flow<String> = _openChats.receiveAsFlow()

    private var loadedId: String? = null

    fun load(userId: String) {
        if (loadedId == userId) return
        loadedId = userId
        val cached = repo.getUser(userId)
        _uiState.value = PublicProfileUiState(cached != null, cached, repo.publicationsByAuthor(userId))
        viewModelScope.launch {
            val fresh = repo.refreshUser(userId).getOrNull()
            _uiState.value = PublicProfileUiState(true, fresh ?: cached, repo.publicationsByAuthor(userId))
        }
    }

    fun contact(userId: String) {
        viewModelScope.launch {
            repo.openConversation(userId)
                .onSuccess { _openChats.send(it) }
                .onFailure { e -> _messages.send(e.toUiText(R.string.error_open_chat)) }
        }
    }

    /**
     * No hay reportes de perfiles en el esquema: se reporta la publicación visible más reciente del usuario
     * (llega a la cola de moderación con el motivo "Perfil reportado"). Sin publicaciones no hay qué reportar.
     */
    fun report() {
        val latest = _uiState.value.publications.firstOrNull()
        if (latest == null) {
            _messages.trySend(uiText(R.string.msg_profile_nothing_to_report))
            return
        }
        viewModelScope.launch {
            repo.reportPublication(latest.id, "Perfil reportado", "Reporte enviado desde el perfil público")
                .onSuccess { _messages.send(uiText(R.string.msg_profile_report_sent)) }
                .onFailure { e -> _messages.send(e.toUiText(R.string.error_send_report)) }
        }
    }
}
