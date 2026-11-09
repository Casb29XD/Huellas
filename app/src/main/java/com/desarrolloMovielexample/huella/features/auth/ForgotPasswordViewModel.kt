package com.desarrolloMovielexample.huella.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.utils.UiText
import com.desarrolloMovielexample.huella.core.utils.isValidEmail
import com.desarrolloMovielexample.huella.core.utils.toUiText
import com.desarrolloMovielexample.huella.core.utils.uiText
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

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: UiText? = null,
    val isLoading: Boolean = false,
    val emailSent: Boolean = false,
)

class ForgotPasswordViewModel(private val repo: HuellaRepository = SupabaseRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    private val _messages = Channel<UiText>(Channel.BUFFERED)
    val messages: Flow<UiText> = _messages.receiveAsFlow()

    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, emailError = null) }

    /** Validates and sends; also used by "Reenviar enlace" in the success state. */
    fun sendLink() {
        val s = _uiState.value
        if (s.isLoading) return
        val error = when {
            s.email.isBlank() -> uiText(R.string.error_email_empty)
            !isValidEmail(s.email) -> uiText(R.string.error_email_invalid)
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(emailError = error) }
            _messages.trySend(error)
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repo.sendPasswordReset(s.email.trim())
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, emailSent = true) }
                    _messages.trySend(uiText(R.string.msg_link_sent, s.email.trim()))
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, emailError = e.toUiText(R.string.error_send_link)) }
                    _messages.trySend(e.toUiText(R.string.error_send_link))
                }
        }
    }

    fun editEmail() = _uiState.update { it.copy(emailSent = false) }
}
