package com.desarrolloMovielexample.huella.features.createpost

import android.net.Uri
import com.desarrolloMovielexample.huella.domain.model.Publication
import com.desarrolloMovielexample.huella.domain.model.PublicationDraft
import com.desarrolloMovielexample.huella.domain.repository.FakeRepository
import com.desarrolloMovielexample.huella.domain.repository.HuellaRepository
import com.desarrolloMovielexample.huella.features.feed.PublicationDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CreatePostViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `item 14 - el asistente guarda una sola foto y la nueva reemplaza a la anterior`() {
        val vm = CreatePostViewModel(FakeRepository)
        vm.addPhotos(listOf(Uri.parse("content://a"), Uri.parse("content://b"), Uri.parse("content://c")))
        assertEquals(listOf(Uri.parse("content://a")), vm.uiState.value.photos)
        vm.addPhotos(listOf(Uri.parse("content://d")))
        assertEquals(listOf(Uri.parse("content://d")), vm.uiState.value.photos)
    }

    /** Registra qué escritura hace el ViewModel sin tocar los datos compartidos de FakeRepository. */
    private class RecordingRepo : HuellaRepository by FakeRepository {
        var creates = 0
        val updates = mutableListOf<Pair<String, PublicationDraft>>()
        override suspend fun createPublication(draft: PublicationDraft): Result<Publication> {
            creates++
            return Result.failure(AssertionError("editar no debe crear"))
        }
        override suspend fun updatePublication(publicationId: String, draft: PublicationDraft): Result<Publication> {
            updates += publicationId to draft
            return Result.success(FakeRepository.getPublication(publicationId)!!)
        }
    }

    @Test
    fun `item 9 - editar precarga la publicacion y guarda con updatePublication, no con createPublication`() = runTest(dispatcher) {
        val original = FakeRepository.getPublication("101")!!
        val repo = RecordingRepo()
        val vm = CreatePostViewModel(repo)
        vm.loadForEdit("101")
        val s = vm.uiState.value
        assertEquals("101", s.editingId)
        assertEquals(original.title, s.title)
        assertEquals(original.description, s.description)
        assertEquals(listOf(Uri.parse(original.imageUrl)), s.photos)

        vm.onTitle("Max, beagle juguetón y vacunado")
        vm.publish()
        advanceUntilIdle()

        assertEquals(0, repo.creates)
        assertEquals(1, repo.updates.size)
        val (id, draft) = repo.updates.single()
        assertEquals("101", id)
        assertEquals("Max, beagle juguetón y vacunado", draft.title)
        assertEquals(original.imageUrl, draft.imageUrl) // la foto existente no se vuelve a subir
        assertTrue(vm.uiState.value.published)
    }

    @Test
    fun `item 9 - el moderador que no es autor no ve Editar`() = runTest(dispatcher) {
        // Mariana es moderadora; la publicación "1" es de Camila. Login explícito: FakeRepository es compartido.
        FakeRepository.login("mariana@correo.com", "12345678")
        val vm = PublicationDetailViewModel("1", FakeRepository)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isAuthorOrMod)
        assertFalse(vm.uiState.value.isAuthor)
        val own = PublicationDetailViewModel("101", FakeRepository)
        advanceUntilIdle()
        assertTrue(own.uiState.value.isAuthor)
    }

    @Test
    fun `item 33 - la ciudad por defecto es la del perfil, no Medellin fijo`() = runTest(dispatcher) {
        FakeRepository.login("andres@correo.com", "12345678") // Bogotá
        assertEquals("Bogotá, Cundinamarca", CreatePostViewModel(FakeRepository).uiState.value.city)
        FakeRepository.login("mariana@correo.com", "12345678")
    }
}
