package com.desarrolloMovielexample.huella.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.utils.MIN_PASSWORD_LENGTH
import com.desarrolloMovielexample.huella.core.utils.UiText
import com.desarrolloMovielexample.huella.core.utils.isValidPassword
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

data class SetPasswordUiState(
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val passwordError: UiText? = null,
    val confirmError: UiText? = null,
    val isLoading: Boolean = false,
    val saved: Boolean = false,
)

/** Nueva contraseña tras el enlace de recuperación (ítem 11) o desde Ajustes con sesión iniciada. */
class SetPasswordViewModel(private val repo: HuellaRepository = SupabaseRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SetPasswordUiState())
    val uiState: StateFlow<SetPasswordUiState> = _uiState.asStateFlow()

    private val _messages = Channel<UiText>(Channel.BUFFERED)
    val messages: Flow<UiText> = _messages.receiveAsFlow()

    fun onPasswordChange(v: String) = _uiState.update { it.copy(password = v, passwordError = null) }
    fun onConfirmChange(v: String) = _uiState.update { it.copy(confirmPassword = v, confirmError = null) }
    fun togglePasswordVisibility() = _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun save() {
        val s = _uiState.value
        if (s.isLoading) return
        // Mismas reglas que el registro.
        val passwordError = if (!isValidPassword(s.password)) uiText(R.string.error_password_min, MIN_PASSWORD_LENGTH) else null
        val confirmError = when {
            s.confirmPassword.isEmpty() -> uiText(R.string.error_confirm_password)
            s.confirmPassword != s.password -> uiText(R.string.error_passwords_mismatch)
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
                    _messages.send(uiText(R.string.msg_password_updated))
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    _messages.send(e.toUiText(R.string.error_save_password))
                }
        }
    }
}
