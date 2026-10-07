package com.desarrolloMovielexample.huella.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desarrolloMovielexample.huella.domain.model.Level
import com.desarrolloMovielexample.huella.domain.model.PointsAction
import com.desarrolloMovielexample.huella.domain.repository.FakeRepository
import com.desarrolloMovielexample.huella.domain.repository.HuellaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class LevelsUiState(
    val currentLevel: Level? = null,
    val levels: List<Level> = Level.entries,
    val actions: List<PointsAction> = emptyList(),
)

class LevelsViewModel(private val repo: HuellaRepository = FakeRepository) : ViewModel() {
    val uiState: StateFlow<LevelsUiState> = repo.currentUser
        .map { LevelsUiState(currentLevel = it?.level, actions = repo.pointsActions) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            LevelsUiState(currentLevel = repo.currentUser.value?.level, actions = repo.pointsActions),
        )
}
