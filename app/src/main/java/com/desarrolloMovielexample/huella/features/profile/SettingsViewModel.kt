package com.desarrolloMovielexample.huella.features.profile

import androidx.lifecycle.ViewModel
import com.desarrolloMovielexample.huella.domain.repository.FakeRepository
import com.desarrolloMovielexample.huella.domain.repository.HuellaRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

// ponytail: toggles live in memory only; persist them (DataStore) when notifications are real.
data class SettingsUiState(val pushEnabled: Boolean = true, val emailDigestEnabled: Boolean = false)

class SettingsViewModel(private val repo: HuellaRepository = FakeRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    fun setPush(enabled: Boolean) = _uiState.update { it.copy(pushEnabled = enabled) }
    fun setEmailDigest(enabled: Boolean) = _uiState.update { it.copy(emailDigestEnabled = enabled) }

    fun onTerms() {
        _messages.trySend("Los términos estarán disponibles pronto")
    }

    fun logout() = repo.logout()
}
