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

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val emailError: UiText? = null,
    val passwordError: UiText? = null,
    val isLoading: Boolean = false,
    val loggedIn: Boolean = false,
)

class LoginViewModel(private val repo: HuellaRepository = SupabaseRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _messages = Channel<UiText>(Channel.BUFFERED)
    val messages: Flow<UiText> = _messages.receiveAsFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, emailError = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, passwordError = null) }
    fun togglePasswordVisibility() = _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun beginGoogleSignIn(): Boolean {
        if (_uiState.value.isLoading) return false
        _uiState.update { it.copy(isLoading = true) }
        return true
    }

    fun cancelGoogleSignIn() {
        _uiState.update { it.copy(isLoading = false) }
    }

    fun googleSignInConfigurationMissing() {
        _messages.trySend(uiText(R.string.error_google_config))
    }

    fun googleSignInFailed() {
        _uiState.update { it.copy(isLoading = false) }
        _messages.trySend(uiText(R.string.error_google_sign_in))
    }

    fun loginWithGoogle(idToken: String, nonce: String) {
        viewModelScope.launch {
            repo.loginWithGoogle(idToken, nonce)
                .onSuccess { user ->
                    _messages.trySend(uiText(R.string.msg_hello, user.name.substringBefore(' ')))
                    _uiState.update { it.copy(isLoading = false, loggedIn = true) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    _messages.trySend(error.toUiText(R.string.error_google_sign_in))
                }
        }
    }

    fun login() {
        val s = _uiState.value
        if (s.isLoading) return
        val emailError = when {
            s.email.isBlank() -> uiText(R.string.error_email_required)
            !isValidEmail(s.email) -> uiText(R.string.error_email_invalid_example)
            else -> null
        }
        val passwordError = if (s.password.isEmpty()) uiText(R.string.error_password_required) else null
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            _messages.trySend(uiText(R.string.msg_check_form))
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repo.login(s.email.trim(), s.password)
                .onSuccess { user ->
                    _messages.trySend(uiText(R.string.msg_hello, user.name.substringBefore(' ')))
                    _uiState.update { it.copy(isLoading = false, loggedIn = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    _messages.trySend(e.toUiText(R.string.error_login))
                }
        }
    }
}
