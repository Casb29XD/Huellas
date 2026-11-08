package com.desarrolloMovielexample.huella.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.utils.MIN_PASSWORD_LENGTH
import com.desarrolloMovielexample.huella.core.utils.UiText
import com.desarrolloMovielexample.huella.core.utils.isValidEmail
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

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val city: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val acceptedTerms: Boolean = false,
    val cities: List<String> = emptyList(),
    val nameError: UiText? = null,
    val emailError: UiText? = null,
    val cityError: UiText? = null,
    val passwordError: UiText? = null,
    val confirmError: UiText? = null,
    val termsError: Boolean = false,
    val isLoading: Boolean = false,
    val registered: Boolean = false,
)

class RegisterViewModel(private val repo: HuellaRepository = SupabaseRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState(cities = repo.cities, city = repo.cities.firstOrNull().orEmpty()))
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _messages = Channel<UiText>(Channel.BUFFERED)
    val messages: Flow<UiText> = _messages.receiveAsFlow()

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
        val nameError = if (s.name.isBlank()) uiText(R.string.error_name_required) else null
        val emailError = when {
            s.email.isBlank() -> uiText(R.string.error_email_required)
            !isValidEmail(s.email) -> uiText(R.string.error_email_invalid_example)
            else -> null
        }
        val cityError = if (s.city.isBlank()) uiText(R.string.error_city_required) else null
        val passwordError = if (!isValidPassword(s.password)) uiText(R.string.error_password_min, MIN_PASSWORD_LENGTH) else null
        val confirmError = when {
            s.confirmPassword.isEmpty() -> uiText(R.string.error_confirm_password)
            s.confirmPassword != s.password -> uiText(R.string.error_passwords_mismatch)
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
            !fieldsOk -> { _messages.trySend(uiText(R.string.msg_check_form)); return }
            !s.acceptedTerms -> { _messages.trySend(uiText(R.string.register_terms_required)); return }
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repo.register(s.name.trim(), s.email.trim(), s.city, s.password)
                .onSuccess {
                    _messages.trySend(uiText(R.string.msg_account_created))
                    _uiState.update { it.copy(isLoading = false, registered = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    _messages.trySend(e.toUiText(R.string.error_register))
                }
        }
    }
}
