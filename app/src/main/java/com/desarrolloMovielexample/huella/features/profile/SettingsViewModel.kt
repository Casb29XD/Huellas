package com.desarrolloMovielexample.huella.features.profile
import androidx.lifecycle.viewModelScope
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.utils.UiText
import com.desarrolloMovielexample.huella.core.utils.toUiText
import com.desarrolloMovielexample.huella.core.utils.uiText
import kotlinx.coroutines.launch

import androidx.lifecycle.ViewModel
import com.desarrolloMovielexample.huella.data.remote.SupabaseRepository
import com.desarrolloMovielexample.huella.domain.repository.HuellaRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class SettingsViewModel(private val repo: HuellaRepository = SupabaseRepository) : ViewModel() {
    private val _messages = Channel<UiText>(Channel.BUFFERED)
    val messages: Flow<UiText> = _messages.receiveAsFlow()

    fun logout() = repo.logout()

    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repo.deleteAccount()
                .onSuccess {
                    _messages.send(uiText(R.string.msg_account_deleted))
                    onSuccess()
                }
                .onFailure {
                    _messages.send(it.toUiText(R.string.error_delete_account))
                }
        }
    }
}

