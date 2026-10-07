package com.desarrolloMovielexample.huella.features.createpost

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.desarrolloMovielexample.huella.domain.model.Category
import com.desarrolloMovielexample.huella.domain.model.PublicationDraft
import com.desarrolloMovielexample.huella.domain.repository.FakeRepository
import com.desarrolloMovielexample.huella.domain.repository.HuellaRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val MAX_PHOTOS = 5

/** Defaults mirror the design prototype (`pub` initial state in index.html). */
data class CreatePostUiState(
    val step: Int = 1,
    val category: Category = Category.ADOPCION,
    val photos: List<Int> = listOf(1, 2),          // photo seeds (random picsum image per seed)
    val nextPhotoSeed: Int = 3,
    val species: String = "Perro",
    val breed: String = "",
    val size: String = "Mediano",
    val vaccinated: Boolean = true,
    val sterilized: Boolean = false,
    val age: String = "",
    val sex: String = "Hembra",
    val title: String = "",
    val description: String = "",
    val city: String = "Medellín, Antioquia",
    val location: String = "",
    val eventDate: String = "",
    val breedError: String? = null,
    val ageError: String? = null,
    val titleError: String? = null,
    val descriptionError: String? = null,
    val eventDateError: String? = null,
    val isLoading: Boolean = false,
    val published: Boolean = false,
) {
    val needsDate: Boolean get() = !category.isAdoption
    val dateLabel: String
        get() = when (category) {
            Category.VETERINARIA -> "Fecha de la jornada"
            Category.PERDIDOS -> "Fecha de la pérdida"
            else -> "Fecha del hallazgo"
        }
    val stepTitle: String get() = listOf("Categoría y fotos", "Datos de la mascota", "Descripción y ubicación")[step - 1]
}

class CreatePostViewModel(private val repo: HuellaRepository = FakeRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    val cities: List<String> get() = repo.cities
    val species: List<String> get() = repo.species
    val breedSuggestions: List<String> get() = repo.breedSuggestions

    fun onCategory(c: Category) = _uiState.update { it.copy(category = c, eventDateError = null) }
    fun addPhoto() = _uiState.update {
        if (it.photos.size >= MAX_PHOTOS) it else it.copy(photos = it.photos + it.nextPhotoSeed, nextPhotoSeed = it.nextPhotoSeed + 1)
    }
    fun removePhoto(seed: Int) = _uiState.update { it.copy(photos = it.photos - seed) }
    fun onSpecies(v: String) = _uiState.update { it.copy(species = v) }
    fun onBreed(v: String) = _uiState.update { it.copy(breed = v, breedError = null) }
    fun onSize(v: String) = _uiState.update { it.copy(size = v) }
    fun onVaccinated(v: Boolean) = _uiState.update { it.copy(vaccinated = v) }
    fun onSterilized(v: Boolean) = _uiState.update { it.copy(sterilized = v) }
    fun onAge(v: String) = _uiState.update { it.copy(age = v, ageError = null) }
    fun onSex(v: String) = _uiState.update { it.copy(sex = v) }
    fun onTitle(v: String) = _uiState.update { it.copy(title = v, titleError = null) }
    fun onDescription(v: String) = _uiState.update { it.copy(description = v, descriptionError = null) }
    fun onCity(v: String) = _uiState.update { it.copy(city = v) }
    fun onLocation(v: String) = _uiState.update { it.copy(location = v) }
    fun onEventDate(v: String) = _uiState.update { it.copy(eventDate = v, eventDateError = null) }

    /** Returns false when already on step 1 (caller leaves the screen). */
    fun previousStep(): Boolean {
        val s = _uiState.value
        if (s.step == 1) return false
        _uiState.update { it.copy(step = it.step - 1) }
        return true
    }

    fun nextStep() {
        val s = _uiState.value
        val error = when (s.step) {
            1 -> if (s.photos.isEmpty()) "Agrega al menos una foto" else null
            2 -> {
                val breedErr = if (s.breed.isBlank()) "Escribe la raza aproximada" else null
                val ageErr = if (s.age.isBlank()) "Escribe la edad aproximada" else null
                _uiState.update { it.copy(breedError = breedErr, ageError = ageErr) }
                breedErr ?: ageErr
            }
            else -> null
        }
        if (error != null) {
            _messages.trySend(error)
            return
        }
        _uiState.update { it.copy(step = (it.step + 1).coerceAtMost(3)) }
    }

    fun publish() {
        val s = _uiState.value
        if (s.isLoading) return
        val titleErr = if (s.title.trim().length < 5) "El título debe tener al menos 5 caracteres" else null
        val descErr = if (s.description.trim().length < 20) "La descripción debe tener al menos 20 caracteres" else null
        val dateErr = if (s.needsDate && s.eventDate.isBlank()) "Indica la ${s.dateLabel.lowercase()}" else null
        _uiState.update { it.copy(titleError = titleErr, descriptionError = descErr, eventDateError = dateErr) }
        val error = titleErr ?: descErr ?: dateErr ?: if (s.city.isBlank()) "Selecciona una ciudad" else null
        if (error != null) {
            _messages.trySend(error)
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            // ponytail: `location` is not in PublicationDraft yet (no maps this phase); add it with the map picker.
            val result = repo.createPublication(
                PublicationDraft(
                    category = s.category, title = s.title, description = s.description, species = s.species,
                    breed = s.breed, size = s.size, vaccinated = s.vaccinated, sterilized = s.sterilized,
                    age = s.age, sex = s.sex, city = s.city, eventDate = s.eventDate.takeIf { s.needsDate },
                ),
            )
            result.onSuccess { _uiState.update { it.copy(isLoading = false, published = true) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    _messages.trySend(e.message ?: "No pudimos enviar la publicación")
                }
        }
    }
}
