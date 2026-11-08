package com.desarrolloMovielexample.huella.features.createpost

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.utils.UiText
import com.desarrolloMovielexample.huella.core.utils.toUiText
import com.desarrolloMovielexample.huella.core.utils.uiText
import com.desarrolloMovielexample.huella.domain.model.Category
import com.desarrolloMovielexample.huella.domain.model.PublicationDraft
import com.desarrolloMovielexample.huella.data.remote.SupabaseRepository
import com.desarrolloMovielexample.huella.domain.repository.HuellaRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ponytail: publications.image_url guarda una sola foto (ítem 14); varias fotos piden image_urls TEXT[].
const val MAX_PHOTOS = 1

/** Defaults mirror the design prototype (`pub` initial state in index.html). */
data class CreatePostUiState(
    val step: Int = 1,
    val category: Category = Category.ADOPCION,
    val photos: List<Uri> = emptyList(),
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
    val breedError: UiText? = null,
    val ageError: UiText? = null,
    val titleError: UiText? = null,
    val descriptionError: UiText? = null,
    val eventDateError: UiText? = null,
    /** Id de la publicación que se edita; null = publicación nueva. */
    val editingId: String? = null,
    val isLoading: Boolean = false,
    val published: Boolean = false,
) {
    val needsDate: Boolean get() = !category.isAdoption
    @get:StringRes
    val dateLabel: Int
        get() = when (category) {
            Category.VETERINARIA -> R.string.create_date_vet
            Category.PERDIDOS -> R.string.create_date_lost
            else -> R.string.create_date_found
        }

    @get:StringRes
    val stepTitle: Int get() = listOf(R.string.create_step_1, R.string.create_step_2, R.string.create_step_3)[step - 1]
}

class CreatePostViewModel(private val repo: HuellaRepository = SupabaseRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    private val _messages = Channel<UiText>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    val cities: List<String> get() = repo.cities
    val species: List<String> get() = repo.species
    val breedSuggestions: List<String> get() = repo.breedSuggestions

    /** Modo edición (ítem 9): precarga el borrador con la publicación del autor; publish() hará PATCH. */
    fun loadForEdit(publicationId: String) {
        if (_uiState.value.editingId == publicationId) return
        val p = repo.getPublication(publicationId)
        if (p == null) {
            _messages.trySend(uiText(R.string.detail_unavailable))
            return
        }
        _uiState.update {
            it.copy(
                editingId = p.id, category = p.category, photos = listOfNotNull(p.imageUrl?.let(Uri::parse)),
                species = p.species, breed = p.breed, size = p.size, vaccinated = p.vaccinated,
                sterilized = p.sterilized == true, age = p.age.orEmpty(), sex = p.sex ?: it.sex, title = p.title,
                description = p.description, city = repo.cities.firstOrNull { c -> c.substringBefore(",") == p.city } ?: p.city,
                eventDate = p.eventDate.orEmpty(),
            )
        }
    }

    fun onCategory(c: Category) = _uiState.update { it.copy(category = c, eventDateError = null) }
    fun addPhotos(uris: List<Uri>) = _uiState.update {
        it.copy(photos = (uris + it.photos).distinct().take(MAX_PHOTOS)) // la nueva reemplaza a la anterior
    }
    fun removePhoto(uri: Uri) = _uiState.update { it.copy(photos = it.photos - uri) }
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
            1 -> if (s.photos.isEmpty()) uiText(R.string.error_photo_required) else null
            2 -> {
                val breedErr = if (s.breed.isBlank()) uiText(R.string.error_breed_required) else null
                val ageErr = if (s.age.isBlank()) uiText(R.string.error_age_required) else null
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
        val titleErr = if (s.title.trim().length < 5) uiText(R.string.error_title_short) else null
        val descErr = if (s.description.trim().length < 20) uiText(R.string.error_description_short) else null
        val dateErr = if (s.needsDate && s.eventDate.isBlank()) {
            uiText(
                when (s.category) {
                    Category.VETERINARIA -> R.string.error_date_vet
                    Category.PERDIDOS -> R.string.error_date_lost
                    else -> R.string.error_date_found
                },
            )
        } else {
            null
        }
        _uiState.update { it.copy(titleError = titleErr, descriptionError = descErr, eventDateError = dateErr) }
        val error = titleErr ?: descErr ?: dateErr ?: if (s.city.isBlank()) uiText(R.string.error_city_select) else null
        if (error != null) {
            _messages.trySend(error)
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            // ponytail: `location` is not in PublicationDraft yet (no maps this phase); add it with the map picker.
            val result = runCatching {
                // Al editar, la foto que ya está en pet-images (http/https) no se vuelve a subir.
                val photo = s.photos.first()
                val imageUrl = if (photo.scheme == "http" || photo.scheme == "https") photo.toString()
                else SupabaseRepository.uploadPublicationImage(photo)
                val draft = PublicationDraft(
                    category = s.category, title = s.title, description = s.description, species = s.species,
                    breed = s.breed, size = s.size, vaccinated = s.vaccinated, sterilized = s.sterilized,
                    age = s.age, sex = s.sex, city = s.city, eventDate = s.eventDate.takeIf { s.needsDate },
                    imageUrl = imageUrl,
                )
                val editingId = s.editingId
                if (editingId != null) repo.updatePublication(editingId, draft).getOrThrow()
                else repo.createPublication(draft).getOrThrow()
            }
            result.onSuccess { _uiState.update { it.copy(isLoading = false, published = true) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    _messages.trySend(e.toUiText(R.string.error_send_post))
                }
        }
    }
}
