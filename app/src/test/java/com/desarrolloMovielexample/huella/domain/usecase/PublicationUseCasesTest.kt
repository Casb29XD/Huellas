package com.desarrolloMovielexample.huella.domain.usecase

import com.desarrolloMovielexample.huella.domain.model.Category
import com.desarrolloMovielexample.huella.domain.model.PostStatus
import com.desarrolloMovielexample.huella.domain.model.PublicationDraft
import com.desarrolloMovielexample.huella.domain.repository.FakeRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PublicationUseCasesTest {

    private val repo = FakeRepository

    @Before
    fun setUp() = runBlocking {
        repo.login("mariana@correo.com", "12345678")
        Unit
    }

    @Test
    fun createPublicationValidation() = runBlocking {
        val createCase = CreatePublicationUseCase(repo)

        // 1. Título vacío debe fallar
        val invalidDraft1 = PublicationDraft(
            category = Category.ADOPCION,
            title = "",
            description = "Descripción válida",
            species = "Perro",
            breed = "Criollo",
            size = "Mediano",
            vaccinated = true,
            sterilized = true,
            age = "1 año",
            sex = "Macho",
            city = "Bogotá"
        )
        val failResult1 = createCase(invalidDraft1)
        assertTrue(failResult1.isFailure)
        assertEquals("El título es obligatorio", failResult1.exceptionOrNull()?.message)

        // 2. Descripción vacía debe fallar
        val invalidDraft2 = invalidDraft1.copy(title = "Título válido", description = "")
        val failResult2 = createCase(invalidDraft2)
        assertTrue(failResult2.isFailure)
        assertEquals("La descripción es obligatoria", failResult2.exceptionOrNull()?.message)

        // 3. Draft completo debe ser exitoso
        val validDraft = invalidDraft1.copy(title = "Perrito Rescatado", description = "Muy cariñoso")
        val successResult = createCase(validDraft)
        assertTrue(successResult.isSuccess)
        val created = successResult.getOrThrow()
        assertEquals("Perrito Rescatado", created.title)
        assertEquals(PostStatus.EN_REVISION, created.status)
    }

    @Test
    fun updatePublicationValidation() = runBlocking {
        val updateCase = UpdatePublicationUseCase(repo)

        val emptyTitleDraft = PublicationDraft(
            category = Category.ADOPCION,
            title = "",
            description = "Descripción",
            species = "Perro",
            breed = "Criollo",
            size = "Grande",
            vaccinated = true,
            sterilized = true,
            age = "2 años",
            sex = "Hembra",
            city = "Cali"
        )
        val failResult = updateCase("1", emptyTitleDraft)
        assertTrue(failResult.isFailure)
        assertEquals("El título es obligatorio", failResult.exceptionOrNull()?.message)

        val validUpdateDraft = emptyTitleDraft.copy(title = "Título Actualizado")
        val successResult = updateCase("1", validUpdateDraft)
        assertTrue(successResult.isSuccess)
        assertEquals("Título Actualizado", repo.getPublication("1")?.title)
    }

    @Test
    fun commentValidationAndDeletion() = runBlocking {
        val addCommentCase = AddCommentUseCase(repo)
        val deleteCommentCase = DeleteCommentUseCase(repo)

        // Comentario vacío
        val blankResult = addCommentCase("1", "   ")
        assertTrue(blankResult.isFailure)
        assertEquals("El comentario no puede estar vacío", blankResult.exceptionOrNull()?.message)

        // Comentario que excede 500 caracteres
        val longText = "a".repeat(501)
        val longResult = addCommentCase("1", longText)
        assertTrue(longResult.isFailure)
        assertEquals("Máximo 500 caracteres", longResult.exceptionOrNull()?.message)

        // Comentario válido
        val validResult = addCommentCase("1", "¡Qué hermoso ejemplar!")
        assertTrue(validResult.isSuccess)
        val comment = validResult.getOrThrow()
        assertEquals("¡Qué hermoso ejemplar!", comment.text)

        // Borrar el comentario recién creado
        val deleteResult = deleteCommentCase("1", comment.id)
        assertTrue(deleteResult.isSuccess)
        val currentComments = repo.comments("1").value
        assertFalse(currentComments.any { it.id == comment.id })
    }

    @Test
    fun deletePublicationFlow() = runBlocking {
        val createCase = CreatePublicationUseCase(repo)
        val deleteCase = DeletePublicationUseCase(repo)

        val draft = PublicationDraft(
            category = Category.PERDIDOS,
            title = "Mascota temporal para borrar",
            description = "Búsqueda urgente",
            species = "Gato",
            breed = "Siamés",
            size = "Pequeño",
            vaccinated = true,
            sterilized = false,
            age = "3 años",
            sex = "Hembra",
            city = "Medellín"
        )
        val created = createCase(draft).getOrThrow()
        assertNotNull(repo.getPublication(created.id))

        val deleteResult = deleteCase(created.id)
        assertTrue(deleteResult.isSuccess)
        assertNull(repo.getPublication(created.id))
    }

    @Test
    fun moderationRejectFlow() = runBlocking {
        val createCase = CreatePublicationUseCase(repo)
        val modCase = ModeratePublicationUseCase(repo)

        val draft = PublicationDraft(
            category = Category.ADOPCION,
            title = "Publicación a rechazar",
            description = "Información incompleta",
            species = "Perro",
            breed = "Criollo",
            size = "Mediano",
            vaccinated = false,
            sterilized = false,
            age = "Desconocida",
            sex = "Macho",
            city = "Cartagena"
        )
        val created = createCase(draft).getOrThrow()

        val rejectResult = modCase.reject(created.id, "Información insuficiente", "Por favor adjuntar fotos claras")
        assertTrue(rejectResult.isSuccess)

        val updated = repo.getPublication(created.id)
        assertEquals(PostStatus.RECHAZADA, updated?.status)
    }

    @Test
    fun deleteAccountFlow() = runBlocking {
        val deleteAccountCase = DeleteAccountUseCase(repo)
        val result = deleteAccountCase()
        assertTrue(result.isSuccess)
        assertNull(repo.currentUser.value)
    }
}
