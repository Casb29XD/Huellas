package com.desarrolloMovielexample.huella.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desarrolloMovielexample.huella.domain.model.PointsEntry
import com.desarrolloMovielexample.huella.domain.model.Publication
import com.desarrolloMovielexample.huella.domain.model.User
import com.desarrolloMovielexample.huella.domain.repository.FakeRepository
import com.desarrolloMovielexample.huella.domain.repository.HuellaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ProfileUiState(
    val user: User? = null,
    val publications: List<Publication> = emptyList(),
    val history: List<PointsEntry> = emptyList(),
    val selectedTab: Int = 0,
)

class ProfileViewModel(private val repo: HuellaRepository = FakeRepository) : ViewModel() {
    private val tab = MutableStateFlow(0)

    val uiState: StateFlow<ProfileUiState> =
        combine(repo.currentUser, repo.myPublications, tab) { user, pubs, t ->
            ProfileUiState(user, pubs, repo.pointsHistory, t)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ProfileUiState(repo.currentUser.value, repo.myPublications.value, repo.pointsHistory, 0),
        )

    fun selectTab(index: Int) {
        tab.value = index
    }
}
