package com.desarrolloMovielexample.huella.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desarrolloMovielexample.huella.core.utils.MIN_PASSWORD_LENGTH
import com.desarrolloMovielexample.huella.core.utils.isValidEmail
import com.desarrolloMovielexample.huella.core.utils.isValidPassword
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

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val city: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val acceptedTerms: Boolean = false,
    val cities: List<String> = emptyList(),
    val nameError: String? = null,
    val emailError: String? = null,
    val cityError: String? = null,
    val passwordError: String? = null,
    val confirmError: String? = null,
    val termsError: Boolean = false,
    val isLoading: Boolean = false,
    val registered: Boolean = false,
)

class RegisterViewModel(private val repo: HuellaRepository = FakeRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState(cities = repo.cities, city = repo.cities.firstOrNull().orEmpty()))
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    fun onNameChange(v: String) = _uiState.update { it.copy(name = v, nameError = null) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, emailError = null) }
    fun onCityChange(v: String) = _uiState.update { it.copy(city = v, cityError = null) }
    fun onPasswordChange(v: String) = _uiState.update { it.copy(password = v, passwordError = null) }
    fun onConfirmChange(v: String) = _uiState.update { it.copy(confirmPassword = v, confirmError = null) }
    fun togglePasswordVisibility() = _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    fun toggleTerms() = _uiState.update { it.copy(acceptedTerms = !it.acceptedTerms, termsError = false) }

    fun register() {
        val s = _uiState.value
        if (s.isLoading) return
        val nameError = if (s.name.isBlank()) "Ingresa tu nombre" else null
        val emailError = when {
            s.email.isBlank() -> "Ingresa tu correo"
            !isValidEmail(s.email) -> "Ingresa un correo válido"
            else -> null
        }
        val cityError = if (s.city.isBlank()) "Selecciona tu ciudad" else null
        val passwordError = if (!isValidPassword(s.password)) "Mínimo $MIN_PASSWORD_LENGTH caracteres" else null
        val confirmError = when {
            s.confirmPassword.isEmpty() -> "Repite tu contraseña"
            s.confirmPassword != s.password -> "Las contraseñas no coinciden"
            else -> null
        }
        val fieldsOk = listOf(nameError, emailError, cityError, passwordError, confirmError).all { it == null }
        _uiState.update {
            it.copy(
                nameError = nameError, emailError = emailError, cityError = cityError,
                passwordError = passwordError, confirmError = confirmError, termsError = !s.acceptedTerms,
            )
        }
        when {
            !fieldsOk -> { _messages.trySend("Revisa los campos marcados"); return }
            !s.acceptedTerms -> { _messages.trySend("Debes aceptar los términos y condiciones"); return }
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repo.register(s.name.trim(), s.email.trim(), s.city, s.password)
                .onSuccess {
                    _messages.trySend("¡Cuenta creada! Bienvenido a Huella")
                    _uiState.update { it.copy(isLoading = false, registered = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    _messages.trySend(e.message ?: "No pudimos crear tu cuenta")
                }
        }
    }
}
