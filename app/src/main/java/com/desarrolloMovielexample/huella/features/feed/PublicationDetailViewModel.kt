package com.desarrolloMovielexample.huella.features.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desarrolloMovielexample.huella.domain.model.Comment
import com.desarrolloMovielexample.huella.domain.model.Publication
import com.desarrolloMovielexample.huella.data.remote.SupabaseRepository
import com.desarrolloMovielexample.huella.domain.repository.HuellaRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DetailSheet { INTERES, REPORTAR, VISTO, LLAMAR, COMENTARIOS }

val REPORT_REASONS = listOf("Venta de animales", "Maltrato", "Información falsa", "Spam", "Otro")

data class PublicationDetailUiState(
    val publication: Publication? = null,
    val requestSent: Boolean = false,
    val sheet: DetailSheet? = null,
    val interestMessage: String = "",
    val reportReason: String? = null,
    val reportDetail: String = "",
    val sightingText: String = "",
    val isSending: Boolean = false,
    val comments: List<Comment> = emptyList(),
    val commentDraft: String = "",
    val isAuthorOrMod: Boolean = false,
)

class PublicationDetailViewModel(
    private val publicationId: String,
    private val repo: HuellaRepository = SupabaseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PublicationDetailUiState(publication = repo.getPublication(publicationId)))
    val uiState: StateFlow<PublicationDetailUiState> = _uiState.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    init {
        viewModelScope.launch {
            repo.sentRequests.collect { list ->
                _uiState.update { s -> s.copy(requestSent = list.any { it.publicationId == publicationId }) }
            }
        }
        viewModelScope.launch {
            repo.comments(publicationId).collect { list ->
                _uiState.update { it.copy(comments = list) }
            }
        }
        viewModelScope.launch {
            repo.currentUser.collect { user ->
                val pub = _uiState.value.publication
                val isAuthor = user != null && pub != null && user.id == pub.author.userId
                val isMod = user?.isModerator == true
                _uiState.update { it.copy(isAuthorOrMod = isAuthor || isMod) }
            }
        }
        viewModelScope.launch {
            repo.refreshPublication(publicationId)
            repo.getPublication(publicationId)?.let { fresh -> _uiState.update { it.copy(publication = fresh) } }
        }
        viewModelScope.launch {
            repo.publications.collect { list ->
                val fresh = list.firstOrNull { it.id == publicationId } ?: repo.getPublication(publicationId)
                if (fresh != null) {
                    _uiState.update { it.copy(publication = fresh) }
                }
            }
        }
    }

    fun openSheet(sheet: DetailSheet) = _uiState.update { it.copy(sheet = sheet) }
    fun closeSheet() = _uiState.update { it.copy(sheet = null, isSending = false) }

    fun onInterestMessageChange(text: String) = _uiState.update { it.copy(interestMessage = text) }
    fun onReportReasonChange(reason: String) = _uiState.update { it.copy(reportReason = reason) }
    fun onReportDetailChange(text: String) = _uiState.update { it.copy(reportDetail = text) }
    fun onSightingChange(text: String) = _uiState.update { it.copy(sightingText = text) }
    fun onCommentDraftChange(text: String) = _uiState.update { it.copy(commentDraft = text) }

    fun vote(value: Int) {
        viewModelScope.launch {
            repo.votePublication(publicationId, value)
            val updated = repo.getPublication(publicationId)
            if (updated != null) _uiState.update { it.copy(publication = updated) }
        }
    }

    fun addComment() {
        val s = _uiState.value
        val text = s.commentDraft.trim()
        if (text.isBlank()) return
        viewModelScope.launch {
            repo.addComment(publicationId, text)
                .onSuccess {
                    _uiState.update { it.copy(commentDraft = "") }
                    _messages.send("Comentario publicado")
                }
                .onFailure {
                    _messages.send(it.message ?: "No se pudo publicar el comentario")
                }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            repo.deleteComment(publicationId, commentId)
                .onSuccess { _messages.send("Comentario eliminado") }
                .onFailure { _messages.send(it.message ?: "No se pudo eliminar el comentario") }
        }
    }

    fun deleteCurrentPublication(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repo.deletePublication(publicationId)
                .onSuccess {
                    _messages.send("Publicación eliminada")
                    onSuccess()
                }
                .onFailure {
                    _messages.send(it.message ?: "No se pudo eliminar la publicación")
                }
        }
    }

    fun sendInterest() {
        val s = _uiState.value
        if (s.isSending) return
        if (s.interestMessage.length > 500) {
            _messages.trySend("El mensaje no puede superar 500 caracteres")
            return
        }
        _uiState.update { it.copy(isSending = true) }
        viewModelScope.launch {
            repo.sendAdoptionRequest(publicationId, s.interestMessage.trim())
                .onSuccess {
                    _uiState.update { it.copy(isSending = false, sheet = null, requestSent = true, interestMessage = "") }
                    _messages.send("Solicitud enviada. Te avisaremos cuando ${s.publication?.author?.name?.substringBefore(" ") ?: "el autor"} responda")
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSending = false) }
                    _messages.send(e.message ?: "No pudimos enviar la solicitud")
                }
        }
    }

    fun sendReport() {
        val s = _uiState.value
        if (s.isSending) return
        if (s.reportReason == null) {
            _messages.trySend("Selecciona un motivo para el reporte")
            return
        }
        _uiState.update { it.copy(isSending = true) }
        viewModelScope.launch {
            repo.reportPublication(publicationId, s.reportReason, s.reportDetail.trim())
                .onSuccess {
                    _uiState.update { it.copy(isSending = false, sheet = null, reportReason = null, reportDetail = "") }
                    _messages.send("Gracias. Un moderador revisará tu reporte")
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSending = false) }
                    _messages.send(e.message ?: "No pudimos enviar el reporte")
                }
        }
    }

    fun sendSighting() {
        val s = _uiState.value
        if (s.isSending) return
        if (s.sightingText.isBlank()) {
            _messages.trySend("Cuéntanos dónde y cuándo lo viste")
            return
        }
        _uiState.update { it.copy(isSending = true) }
        viewModelScope.launch {
            repo.reportSighting(publicationId, s.sightingText)
                .onSuccess {
                    _uiState.update { it.copy(isSending = false, sheet = null, sightingText = "") }
                    _messages.send("Aviso enviado al autor. ¡Gracias por ayudar!")
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSending = false) }
                    _messages.send(e.message ?: "No pudimos enviar el aviso")
                }
        }
    }
}
