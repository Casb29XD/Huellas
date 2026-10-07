package com.desarrolloMovielexample.huella.features.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desarrolloMovielexample.huella.domain.model.Applicant
import com.desarrolloMovielexample.huella.domain.model.Publication
import com.desarrolloMovielexample.huella.domain.model.ReceivedRequestGroup
import com.desarrolloMovielexample.huella.domain.model.SentRequest
import com.desarrolloMovielexample.huella.domain.repository.FakeRepository
import com.desarrolloMovielexample.huella.domain.repository.HuellaRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SentRequestUi(val request: SentRequest, val publication: Publication)
data class ReceivedGroupUi(val publication: Publication, val applicants: List<Applicant>)

data class RequestsUiState(
    val selectedTab: Int = 0,
    val sent: List<SentRequestUi> = emptyList(),
    val received: List<ReceivedGroupUi> = emptyList(),
    val busyApplicantId: String? = null,
    /** Publication whose request was just accepted -> shows the "Solicitud aceptada" dialog. */
    val acceptedPublicationId: String? = null,
    val isClosing: Boolean = false,
)

class RequestsViewModel(private val repo: HuellaRepository = FakeRepository) : ViewModel() {

    private val local = MutableStateFlow(RequestsUiState())
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    val uiState: StateFlow<RequestsUiState> =
        combine(local, repo.sentRequests, repo.receivedRequests) { l, sent, received -> merge(l, sent, received) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                merge(local.value, repo.sentRequests.value, repo.receivedRequests.value),
            )

    private fun merge(l: RequestsUiState, sent: List<SentRequest>, received: List<ReceivedRequestGroup>) = l.copy(
        sent = sent.mapNotNull { r -> repo.getPublication(r.publicationId)?.let { SentRequestUi(r, it) } },
        received = received.mapNotNull { g -> repo.getPublication(g.publicationId)?.let { ReceivedGroupUi(it, g.applicants) } },
    )

    private var tabInitialized = false

    /** Applies the tab requested by navigation only once (returning from chat keeps the user's tab). */
    fun setInitialTab(tab: Int) {
        if (tabInitialized) return
        tabInitialized = true
        local.update { it.copy(selectedTab = tab.coerceIn(0, 1)) }
    }

    fun selectTab(tab: Int) = local.update { it.copy(selectedTab = tab) }

    fun respond(applicant: Applicant, publicationId: String, accept: Boolean) {
        if (local.value.busyApplicantId != null) return
        viewModelScope.launch {
            local.update { it.copy(busyApplicantId = applicant.id) }
            repo.respondToRequest(applicant.id, accept)
                .onSuccess {
                    local.update { it.copy(busyApplicantId = null, acceptedPublicationId = if (accept) publicationId else null) }
                    if (!accept) _messages.send("Solicitud de ${applicant.name} rechazada")
                }
                .onFailure { e ->
                    local.update { it.copy(busyApplicantId = null) }
                    _messages.send(e.message ?: "No pudimos responder la solicitud")
                }
        }
    }

    fun closePublication() {
        val id = local.value.acceptedPublicationId ?: return
        viewModelScope.launch {
            local.update { it.copy(isClosing = true) }
            repo.closePublication(id)
                .onSuccess {
                    local.update { it.copy(isClosing = false, acceptedPublicationId = null) }
                    _messages.send("Publicación cerrada · +50 puntos")
                }
                .onFailure { e ->
                    local.update { it.copy(isClosing = false) }
                    _messages.send(e.message ?: "No pudimos cerrar la publicación")
                }
        }
    }

    fun keepOpen() {
        local.update { it.copy(acceptedPublicationId = null) }
        _messages.trySend("La publicación sigue abierta")
    }
}
