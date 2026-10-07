package com.desarrolloMovielexample.huella.features.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desarrolloMovielexample.huella.domain.model.Publication
import com.desarrolloMovielexample.huella.domain.repository.FakeRepository
import com.desarrolloMovielexample.huella.domain.repository.HuellaRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DetailSheet { INTERES, REPORTAR, VISTO, LLAMAR }

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
)

class PublicationDetailViewModel(
    private val publicationId: String,
    private val repo: HuellaRepository = FakeRepository,
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
    }

    fun openSheet(sheet: DetailSheet) = _uiState.update { it.copy(sheet = sheet) }

    fun closeSheet() = _uiState.update { it.copy(sheet = null, isSending = false) }

    fun onInterestMessageChange(text: String) = _uiState.update { it.copy(interestMessage = text) }
    fun onReportReasonChange(reason: String) = _uiState.update { it.copy(reportReason = reason) }
    fun onReportDetailChange(text: String) = _uiState.update { it.copy(reportDetail = text) }
    fun onSightingChange(text: String) = _uiState.update { it.copy(sightingText = text) }

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
            delay(700) // ponytail: no repository endpoint for sightings yet; add sendSighting() to HuellaRepository when the backend exists.
            _uiState.update { it.copy(isSending = false, sheet = null, sightingText = "") }
            _messages.send("Aviso enviado. ¡Gracias por ayudar! +5 puntos")
        }
    }
}
