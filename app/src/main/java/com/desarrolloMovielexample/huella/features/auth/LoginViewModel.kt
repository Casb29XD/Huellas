package com.desarrolloMovielexample.huella.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desarrolloMovielexample.huella.core.utils.isValidEmail
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

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val loggedIn: Boolean = false,
)

class LoginViewModel(private val repo: HuellaRepository = FakeRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, emailError = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, passwordError = null) }
    fun togglePasswordVisibility() = _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    fun onGoogleClick() { _messages.trySend("Próximamente") }

    fun login() {
        val s = _uiState.value
        if (s.isLoading) return
        val emailError = when {
            s.email.isBlank() -> "Ingresa tu correo"
            !isValidEmail(s.email) -> "Ingresa un correo válido"
            else -> null
        }
        val passwordError = if (s.password.isEmpty()) "Ingresa tu contraseña" else null
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            _messages.trySend("Revisa los campos marcados")
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repo.login(s.email.trim(), s.password)
                .onSuccess { user ->
                    _messages.trySend("¡Hola, ${user.name.substringBefore(' ')}!")
                    _uiState.update { it.copy(isLoading = false, loggedIn = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    _messages.trySend(e.message ?: "No pudimos iniciar sesión")
                }
        }
    }
}
