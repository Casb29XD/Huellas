package com.desarrolloMovielexample.huella.features.profile

import androidx.lifecycle.ViewModel
import com.desarrolloMovielexample.huella.domain.model.Publication
import com.desarrolloMovielexample.huella.domain.model.User
import com.desarrolloMovielexample.huella.domain.repository.FakeRepository
import com.desarrolloMovielexample.huella.domain.repository.HuellaRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

data class PublicProfileUiState(
    val isLoaded: Boolean = false,
    val user: User? = null,
    val publications: List<Publication> = emptyList(),
)

class PublicProfileViewModel(private val repo: HuellaRepository = FakeRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(PublicProfileUiState())
    val uiState: StateFlow<PublicProfileUiState> = _uiState.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    private var loadedId: String? = null

    fun load(userId: String) {
        if (loadedId == userId) return
        loadedId = userId
        _uiState.value = PublicProfileUiState(true, repo.getUser(userId), repo.publicationsByAuthor(userId))
    }

    fun report() {
        _messages.trySend("Gracias, revisaremos este perfil")
    }
}
