package com.desarrolloMovielexample.huella.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desarrolloMovielexample.huella.core.utils.MIN_PASSWORD_LENGTH
import com.desarrolloMovielexample.huella.core.utils.isValidPassword
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

data class SetPasswordUiState(
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val isLoading: Boolean = false,
    val saved: Boolean = false,
)

/** Nueva contraseña tras el enlace de recuperación (ítem 11) o desde Ajustes con sesión iniciada. */
class SetPasswordViewModel(private val repo: HuellaRepository = SupabaseRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SetPasswordUiState())
    val uiState: StateFlow<SetPasswordUiState> = _uiState.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    fun onPasswordChange(v: String) = _uiState.update { it.copy(password = v, passwordError = null) }
    fun onConfirmChange(v: String) = _uiState.update { it.copy(confirmPassword = v, confirmError = null) }
    fun togglePasswordVisibility() = _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun save() {
        val s = _uiState.value
        if (s.isLoading) return
        // Mismas reglas que el registro.
        val passwordError = if (!isValidPassword(s.password)) "La contraseña debe tener al menos $MIN_PASSWORD_LENGTH caracteres" else null
        val confirmError = when {
            s.confirmPassword.isEmpty() -> "Debes confirmar tu contraseña"
            s.confirmPassword != s.password -> "Las contraseñas no coinciden"
            else -> null
        }
        if (passwordError != null || confirmError != null) {
            _uiState.update { it.copy(passwordError = passwordError, confirmError = confirmError) }
            _messages.trySend(passwordError ?: confirmError!!)
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repo.updatePassword(s.password)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, saved = true) }
                    _messages.send("Contraseña actualizada")
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    _messages.send(e.message ?: "No pudimos guardar la contraseña")
                }
        }
    }
}
