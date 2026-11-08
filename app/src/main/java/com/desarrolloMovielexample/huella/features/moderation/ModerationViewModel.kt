package com.desarrolloMovielexample.huella.features.moderation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desarrolloMovielexample.huella.domain.model.ModerationItem
import com.desarrolloMovielexample.huella.domain.model.ModerationStats
import com.desarrolloMovielexample.huella.domain.model.Report
import com.desarrolloMovielexample.huella.data.remote.SupabaseRepository
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
    val selectedTab: Int = 0,
    val isLoading: Boolean = true,
    val loadError: String? = null,
) {
    val visibleItems: List<ModerationItem> get() = if (selectedTab == 0) pending else reported
}

class ModerationViewModel(private val repo: HuellaRepository = SupabaseRepository) : ViewModel() {
    private val selectedTab = MutableStateFlow(0)
    private val isLoading = MutableStateFlow(true)
    private val loadError = MutableStateFlow<String?>(null)
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    val uiState: StateFlow<ModerationUiState> =
        combine(
            combine(repo.moderationStats, repo.moderationQueue) { stats, queue -> stats to queue },
            repo.moderationReports,
            selectedTab,
            isLoading,
            loadError,
        ) { statsAndQueue, reports, tab, loading, error ->
            ModerationUiState(statsAndQueue.first, statsAndQueue.second, reports, tab, loading, error)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ModerationUiState(
                stats = repo.moderationStats.value,
                pending = repo.moderationQueue.value,
                reported = repo.moderationReports.value,
            ),
        )

    init {
        refresh()
    }

    fun selectTab(index: Int) {
        selectedTab.value = index.coerceIn(0, 1)
    }

    fun retry() = refresh()

    private fun refresh() {
        viewModelScope.launch {
            isLoading.value = true
            loadError.value = null
            repo.refreshModerationData()
                .onSuccess { isLoading.value = false }
                .onFailure { error ->
                    isLoading.value = false
                    loadError.value = error.message ?: "No pudimos cargar la cola de moderación"
                    _messages.send(loadError.value.orEmpty())
                }
        }
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
    val rejectError: String? = null,
    val isApproving: Boolean = false,
    val isRejecting: Boolean = false,
    val resolvingReportId: String? = null,
    val finished: Boolean = false,
) {
    val busy: Boolean get() = isApproving || isRejecting || finished
}

class ModerationDetailViewModel(private val repo: HuellaRepository = SupabaseRepository) : ViewModel() {
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

    fun resolveReport(report: Report) {
        if (_uiState.value.resolvingReportId != null || report.id.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(resolvingReportId = report.id) }
            val result = repo.resolveReport(report.id)
            _uiState.update { state ->
                state.copy(
                    resolvingReportId = null,
                    item = if (result.isSuccess) state.item?.let { it.copy(reports = it.reports - report) } else state.item,
                )
            }
            _messages.send(
                if (result.isSuccess) "Reporte marcado como resuelto."
                else result.exceptionOrNull()?.message ?: "No pudimos resolver el reporte",
            )
        }
    }

    fun openRejectSheet() {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(showRejectSheet = true) }
    }

    fun closeRejectSheet() {
        _uiState.update { it.copy(showRejectSheet = false, reasonError = false, commentError = null, rejectError = null) }
    }

    fun selectReason(reason: String) {
        _uiState.update { it.copy(rejectReason = reason, reasonError = false, commentError = null, rejectError = null) }
    }

    fun updateComment(comment: String) {
        _uiState.update { it.copy(rejectComment = comment, commentError = null) }
    }

    fun confirmReject() {
        val state = _uiState.value
        val item = state.item ?: return
        if (state.busy) return
        val reason = state.rejectReason
        // Errores de validación solo inline y persistentes (sin snackbar que tape el CTA ni duplique el texto).
        if (reason == null) {
            _uiState.update { it.copy(reasonError = true) }
            return
        }
        if (reason == "Otro" && state.rejectComment.isBlank()) {
            _uiState.update { it.copy(commentError = "Cuéntale al autor el motivo del rechazo") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isRejecting = true, rejectError = null) }
            val result = repo.reject(item.id, reason, state.rejectComment.trim())
            if (result.isSuccess) {
                _uiState.update { it.copy(isRejecting = false, finished = true, showRejectSheet = false) }
                _messages.send("Publicación rechazada. Notificamos al autor.")
            } else {
                val msg = result.exceptionOrNull()?.message ?: "No pudimos rechazar la publicación"
                _uiState.update { it.copy(isRejecting = false, rejectError = msg) }
            }
        }
    }
}
