package com.desarrolloMovielexample.huella.features.moderation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desarrolloMovielexample.huella.domain.model.ModerationItem
import com.desarrolloMovielexample.huella.domain.model.ModerationStats
import com.desarrolloMovielexample.huella.domain.repository.FakeRepository
import com.desarrolloMovielexample.huella.domain.repository.HuellaRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------- Panel de moderación ----------

data class ModerationUiState(
    val stats: ModerationStats = ModerationStats(0, 0, 0),
    val pending: List<ModerationItem> = emptyList(),
    val reported: List<ModerationItem> = emptyList(),
    val selectedTab: Int = 0, // 0 Pendientes, 1 Reportes
) {
    val visibleItems: List<ModerationItem> get() = if (selectedTab == 0) pending else reported
}

class ModerationViewModel(private val repo: HuellaRepository = FakeRepository) : ViewModel() {
    private val selectedTab = MutableStateFlow(0)

    val uiState: StateFlow<ModerationUiState> =
        combine(repo.moderationStats, repo.moderationQueue, repo.moderationReports, selectedTab) { stats, queue, reports, tab ->
            ModerationUiState(stats, queue, reports, tab)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ModerationUiState(repo.moderationStats.value, repo.moderationQueue.value, repo.moderationReports.value),
        )

    fun selectTab(index: Int) {
        selectedTab.value = index
    }
}

// ---------- Detalle para moderar + diálogo de rechazo ----------

val REJECT_REASONS = listOf("Venta de animales", "Contenido falso", "Fotos inadecuadas", "Duplicado", "Otro")

data class ModerationDetailUiState(
    val item: ModerationItem? = null,
    val loaded: Boolean = false,
    val showRejectSheet: Boolean = false,
    val rejectReason: String? = null,
    val rejectComment: String = "",
    val reasonError: Boolean = false,
    val commentError: String? = null,
    val isApproving: Boolean = false,
    val isRejecting: Boolean = false,
    val finished: Boolean = false,
) {
    val busy: Boolean get() = isApproving || isRejecting || finished
}

class ModerationDetailViewModel(private val repo: HuellaRepository = FakeRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ModerationDetailUiState())
    val uiState: StateFlow<ModerationDetailUiState> = _uiState.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    /** Loads once; keeps the item after approve/reject removes it from the queue. */
    fun load(itemId: String) {
        if (_uiState.value.loaded) return
        _uiState.update { it.copy(item = repo.getModerationItem(itemId), loaded = true) }
    }

    fun approve() {
        val item = _uiState.value.item ?: return
        if (_uiState.value.busy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isApproving = true) }
            val result = repo.approve(item.id)
            _uiState.update { it.copy(isApproving = false, finished = result.isSuccess) }
            _messages.send(
                if (result.isSuccess) "Publicación aprobada. Ya es visible para todos."
                else result.exceptionOrNull()?.message ?: "No pudimos aprobar la publicación",
            )
        }
    }

    fun openRejectSheet() {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(showRejectSheet = true) }
    }

    fun closeRejectSheet() {
        _uiState.update { it.copy(showRejectSheet = false, reasonError = false, commentError = null) }
    }

    fun selectReason(reason: String) {
        _uiState.update { it.copy(rejectReason = reason, reasonError = false, commentError = null) }
    }

    fun updateComment(comment: String) {
        _uiState.update { it.copy(rejectComment = comment, commentError = null) }
    }

    fun confirmReject() {
        val state = _uiState.value
        val item = state.item ?: return
        if (state.busy) return
        val reason = state.rejectReason
        if (reason == null) {
            _uiState.update { it.copy(reasonError = true) }
            _messages.trySend("Selecciona un motivo de rechazo")
            return
        }
        if (reason == "Otro" && state.rejectComment.isBlank()) {
            val msg = "Cuéntale al autor el motivo del rechazo"
            _uiState.update { it.copy(commentError = msg) }
            _messages.trySend(msg)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isRejecting = true) }
            val result = repo.reject(item.id, reason, state.rejectComment.trim())
            _uiState.update {
                it.copy(isRejecting = false, finished = result.isSuccess, showRejectSheet = !result.isSuccess && it.showRejectSheet)
            }
            _messages.send(
                if (result.isSuccess) "Publicación rechazada. Notificamos al autor."
                else result.exceptionOrNull()?.message ?: "No pudimos rechazar la publicación",
            )
        }
    }
}
