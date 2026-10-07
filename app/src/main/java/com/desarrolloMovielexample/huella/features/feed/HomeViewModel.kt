package com.desarrolloMovielexample.huella.features.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desarrolloMovielexample.huella.domain.model.Category
import com.desarrolloMovielexample.huella.domain.model.Level
import com.desarrolloMovielexample.huella.domain.model.Publication
import com.desarrolloMovielexample.huella.domain.repository.FakeRepository
import com.desarrolloMovielexample.huella.domain.repository.HuellaRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer

/** Filters chosen in the "Filtros" bottom sheet. null / false = not applied. */
data class FeedFilters(
    val species: String? = null,
    val size: String? = null,
    val onlyVaccinated: Boolean = false,
    val onlySterilized: Boolean = false,
    val city: String? = null,          // full name, e.g. "Medellín, Antioquia"
    val distanceKm: Float = 10f,       // ponytail: shown only, no geo filtering until maps phase
) {
    val isActive: Boolean get() = species != null || size != null || onlyVaccinated || onlySterilized || city != null
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val userFirstName: String = "",
    val userInitials: String = "",
    val userLevel: Level = Level.AMIGO_ANIMAL,
    val hasUnreadNotifications: Boolean = false,
    val query: String = "",
    val category: Category? = null,    // null = "Todas"
    val filters: FeedFilters = FeedFilters(),
    val publications: List<Publication> = emptyList(),
    val cities: List<String> = emptyList(),
    val species: List<String> = emptyList(),
) {
    val isSearching: Boolean get() = query.isNotBlank() || filters.isActive || category != null
}

class HomeViewModel(private val repo: HuellaRepository = FakeRepository) : ViewModel() {

    private val controls = MutableStateFlow(HomeUiState(cities = repo.cities, species = repo.species))

    val uiState: StateFlow<HomeUiState> = combine(
        controls, repo.publications, repo.currentUser, repo.notifications,
    ) { c, pubs, user, notifications ->
        c.copy(
            userFirstName = user?.name?.substringBefore(" ").orEmpty(),
            userInitials = user?.initials.orEmpty(),
            userLevel = user?.level ?: Level.AMIGO_ANIMAL,
            hasUnreadNotifications = notifications.any { !it.read },
            publications = filterFeed(pubs, c.query, c.category, c.filters),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), controls.value)

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    init {
        // ponytail: simulated first load so the skeleton state is visible; real fetch replaces this.
        viewModelScope.launch {
            delay(600)
            controls.update { it.copy(isLoading = false) }
        }
    }

    fun onQueryChange(query: String) = controls.update { it.copy(query = query) }

    fun onCategorySelected(category: Category?) = controls.update { it.copy(category = category) }

    fun applyFilters(filters: FeedFilters) {
        controls.update { it.copy(filters = filters) }
        _messages.trySend(if (filters.isActive) "Filtros aplicados" else "Mostrando todas las publicaciones")
    }

    fun updateFilters(filters: FeedFilters) = controls.update { it.copy(filters = filters) }

    /** "Limpiar filtros": query, category and sheet filters. */
    fun clearAll() {
        controls.update { it.copy(query = "", category = null, filters = FeedFilters()) }
        _messages.trySend("Filtros eliminados")
    }

    fun createAlert() {
        _messages.trySend("Listo, te avisaremos cuando haya mascotas para esta búsqueda")
    }
}

private fun String.normalized(): String =
    Normalizer.normalize(lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

/** Every word of [query] must appear in title / species / breed / city / category. */
internal fun filterFeed(
    pubs: List<Publication>,
    query: String,
    category: Category?,
    filters: FeedFilters,
): List<Publication> {
    val words = query.normalized().split(" ").filter { it.isNotBlank() }
    val city = filters.city?.substringBefore(",")
    return pubs.filter { p ->
        val haystack = "${p.title} ${p.species} ${p.breed} ${p.city} ${p.neighborhood} ${p.category.label}".normalized()
        (category == null || p.category == category) &&
            words.all { it in haystack } &&
            (filters.species == null || p.species == filters.species) &&
            (filters.size == null || p.size == filters.size) &&
            (!filters.onlyVaccinated || p.vaccinated) &&
            (!filters.onlySterilized || p.sterilized == true) &&
            (city == null || p.city == city)
    }
}
