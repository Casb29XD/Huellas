package com.desarrolloMovielexample.huella.features.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.utils.UiText
import com.desarrolloMovielexample.huella.core.utils.uiText
import com.desarrolloMovielexample.huella.domain.model.Category
import com.desarrolloMovielexample.huella.domain.model.Level
import com.desarrolloMovielexample.huella.domain.model.Publication
import com.desarrolloMovielexample.huella.data.remote.SupabaseRepository
import com.desarrolloMovielexample.huella.domain.repository.HuellaRepository
import com.desarrolloMovielexample.huella.core.utils.toUiText
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
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
    // ponytail: sin filtro de distancia hasta que haya geolocalización real (ítem 33).
) {
    val isActive: Boolean get() = species != null || size != null || onlyVaccinated || onlySterilized || city != null
}

data class HomeUiState(
    /** true hasta que termina la carga real de la primera página (ítem 56). */
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
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

class HomeViewModel(private val repo: HuellaRepository = SupabaseRepository) : ViewModel() {

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

    private val _messages = Channel<UiText>(Channel.BUFFERED)
    val messages: Flow<UiText> = _messages.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        reload()
    }

    /**
     * Primera página con categoría, especie y ciudad filtradas en el servidor (ítem 32). Búsqueda por texto,
     * tamaño y vacunado/esterilizado se aplican en memoria sobre las páginas cargadas (filterFeed).
     */
    private fun reload() {
        val c = controls.value
        loadJob?.cancel()
        controls.update { it.copy(isLoadingMore = false) }
        loadJob = viewModelScope.launch {
            repo.loadFeed(c.category, c.filters.species, c.filters.city, reset = true)
                .onSuccess { more -> controls.update { it.copy(isLoading = false, hasMore = more) } }
                .onFailure { e ->
                    controls.update { it.copy(isLoading = false, hasMore = false) }
                    _messages.trySend(e.toUiText(R.string.error_load_feed))
                }
        }
    }

    /** La lista llegó al final: pide la página siguiente si la hay. */
    fun loadMore() {
        val c = controls.value
        if (c.isLoading || c.isLoadingMore || !c.hasMore) return
        controls.update { it.copy(isLoadingMore = true) }
        loadJob = viewModelScope.launch {
            val more = repo.loadFeed(c.category, c.filters.species, c.filters.city, reset = false).getOrDefault(false)
            controls.update { it.copy(isLoadingMore = false, hasMore = more) }
        }
    }

    /** Recarga solo si cambió algún filtro que resuelve el servidor. */
    private fun setControls(transform: (HomeUiState) -> HomeUiState) {
        val before = controls.value
        controls.update(transform)
        val after = controls.value
        if (before.category != after.category || before.filters.species != after.filters.species || before.filters.city != after.filters.city) {
            reload()
        }
    }

    fun onQueryChange(query: String) = controls.update { it.copy(query = query) }

    fun onCategorySelected(category: Category?) = setControls { it.copy(category = category) }

    fun applyFilters(filters: FeedFilters) {
        setControls { it.copy(filters = filters) }
        _messages.trySend(uiText(if (filters.isActive) R.string.msg_filters_applied else R.string.msg_showing_all))
    }

    fun updateFilters(filters: FeedFilters) = setControls { it.copy(filters = filters) }

    /** "Limpiar filtros": query, category and sheet filters. */
    fun clearAll() {
        setControls { it.copy(query = "", category = null, filters = FeedFilters()) }
        _messages.trySend(uiText(R.string.msg_filters_cleared))
    }

    fun createAlert() {
        _messages.trySend(uiText(R.string.msg_alert_created))
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
