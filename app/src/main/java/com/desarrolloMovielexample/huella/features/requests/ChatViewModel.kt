package com.desarrolloMovielexample.huella.features.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desarrolloMovielexample.huella.domain.model.ChatMessage
import com.desarrolloMovielexample.huella.domain.model.Conversation
import com.desarrolloMovielexample.huella.domain.repository.FakeRepository
import com.desarrolloMovielexample.huella.domain.repository.HuellaRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val conversation: Conversation? = null,
    val messages: List<ChatMessage> = emptyList(),
    val text: String = "",
    val isSending: Boolean = false,
)

class ChatViewModel(private val repo: HuellaRepository = FakeRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    private var conversationId: String? = null

    fun load(id: String) {
        if (conversationId == id) return
        conversationId = id
        _uiState.update { it.copy(conversation = repo.getConversation(id)) }
        viewModelScope.launch {
            repo.messages(id).collect { list -> _uiState.update { it.copy(messages = list) } }
        }
    }

    fun onTextChange(text: String) = _uiState.update { it.copy(text = text) }

    fun send() {
        val id = conversationId ?: return
        val text = _uiState.value.text
        if (text.isBlank()) {
            _messages.trySend("Escribe un mensaje")
            return
        }
        if (_uiState.value.isSending) return
        _uiState.update { it.copy(text = "", isSending = true) }
        viewModelScope.launch {
            repo.sendMessage(id, text)
                .onSuccess { _uiState.update { it.copy(isSending = false) } }
                .onFailure { e ->
                    _uiState.update { it.copy(text = text, isSending = false) }
                    _messages.send(e.message ?: "No se pudo enviar el mensaje")
                }
        }
    }
}
