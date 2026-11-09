package com.desarrolloMovielexample.huella.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.utils.UiText
import com.desarrolloMovielexample.huella.core.utils.isValidEmail
import com.desarrolloMovielexample.huella.core.utils.toUiText
import com.desarrolloMovielexample.huella.core.utils.uiText
import com.desarrolloMovielexample.huella.domain.model.Level
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

data class EditProfileUiState(
    val initials: String = "",
    val level: Level = Level.AMIGO_ANIMAL,
    val name: String = "",
    val email: String = "",
    val city: String = "",
    val phone: String = "",
    val bio: String = "",
    val cities: List<String> = emptyList(),
    val nameError: UiText? = null,
    val emailError: UiText? = null,
    val cityError: UiText? = null,
    val phoneError: UiText? = null,
    val isLoading: Boolean = false,
    val saved: Boolean = false,
)

class EditProfileViewModel(private val repo: HuellaRepository = SupabaseRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(
        repo.currentUser.value.let { u ->
            EditProfileUiState(
                initials = u?.initials.orEmpty(),
                level = u?.level ?: Level.AMIGO_ANIMAL,
                name = u?.name.orEmpty(),
                email = u?.email.orEmpty(),
                city = u?.city.orEmpty(),
                phone = u?.phone.orEmpty(),
                bio = u?.bio.orEmpty(),
                cities = repo.cities,
            )
        }
    )
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _messages = Channel<UiText>(Channel.BUFFERED)
    val messages: Flow<UiText> = _messages.receiveAsFlow()

    fun onNameChange(v: String) = _uiState.update { it.copy(name = v, nameError = null) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v, emailError = null) }
    fun onCityChange(v: String) = _uiState.update { it.copy(city = v, cityError = null) }
    fun onPhoneChange(v: String) = _uiState.update { it.copy(phone = v, phoneError = null) }
    fun onBioChange(v: String) = _uiState.update { it.copy(bio = v) }

    fun onPhotoClick() {
        _messages.trySend(uiText(R.string.msg_photo_soon))
    }

    fun save() {
        val s = _uiState.value
        if (s.isLoading) return
        val nameError = if (s.name.isBlank()) uiText(R.string.error_name_empty) else null
        val emailError = when {
            s.email.isBlank() -> uiText(R.string.error_email_empty)
            !isValidEmail(s.email) -> uiText(R.string.error_email_invalid)
            else -> null
        }
        val cityError = if (s.city.isBlank()) uiText(R.string.error_city_required) else null
        val digits = s.phone.count { it.isDigit() }
        val phoneError = if (s.phone.isNotBlank() && digits < 7) uiText(R.string.error_phone_invalid) else null
        if (nameError != null || emailError != null || cityError != null || phoneError != null) {
            _uiState.update { it.copy(nameError = nameError, emailError = emailError, cityError = cityError, phoneError = phoneError) }
            _messages.trySend(nameError ?: emailError ?: cityError ?: phoneError ?: uiText(R.string.msg_check_fields))
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repo.updateProfile(s.name, s.email, s.city, s.phone, s.bio)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, saved = true) }
                    _messages.send(uiText(R.string.msg_changes_saved))
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    _messages.send(e.toUiText(R.string.error_save_changes))
                }
        }
    }
}
