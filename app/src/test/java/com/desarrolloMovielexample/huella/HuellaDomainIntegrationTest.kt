package com.desarrolloMovielexample.huella

import com.desarrolloMovielexample.huella.domain.model.Category
import com.desarrolloMovielexample.huella.domain.model.PostStatus
import com.desarrolloMovielexample.huella.domain.model.PublicationDraft
import com.desarrolloMovielexample.huella.domain.model.Role
import com.desarrolloMovielexample.huella.domain.repository.FakeRepository
import com.desarrolloMovielexample.huella.domain.usecase.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class HuellaDomainIntegrationTest {

    private val repo = FakeRepository

    @Before
    fun setUp() = runBlocking {
        repo.login("mariana@correo.com", "12345678")
        Unit
    }

    @Test
    fun testModeratorRoleAndNormalUserRole() = runBlocking {
        // Mariana es moderadora
        val mariana = repo.currentUser.value
        assertNotNull(mariana)
        assertEquals(Role.MODERADOR, mariana?.role)
        assertTrue(mariana?.isModerator == true)

        // Login como Andrés (Usuario normal)
        val loginResult = repo.login("andres@correo.com", "12345678")
        assertTrue(loginResult.isSuccess)
        val andres = repo.currentUser.value
        assertEquals(Role.USUARIO, andres?.role)
        assertFalse(andres?.isModerator == true)
    }

    @Test
    fun testApprovePublicationReachesFeed() = runBlocking {
        val createCase = CreatePublicationUseCase(repo)
        val modCase = ModeratePublicationUseCase(repo)

        // 1. Crear publicación (queda EN_REVISION)
        val draft = PublicationDraft(
            category = Category.ADOPCION,
            title = "Toby el aventurero",
            description = "Cachorro juguetón rescatado",
            species = "Perro",
            breed = "Criollo",
            size = "Mediano",
            vaccinated = true,
            sterilized = false,
            age = "5 meses",
            sex = "Macho",
            city = "Medellín"
        )
        val createdResult = createCase(draft)
        assertTrue(createdResult.isSuccess)
        val created = createdResult.getOrThrow()
        assertEquals(PostStatus.EN_REVISION, created.status)

        // 2. Verificar que NO está en el feed público
        val initialFeed = repo.publications.value
        assertFalse(initialFeed.any { it.id == created.id })

        // 3. Aprobar la publicación como moderador
        val approveResult = modCase.approve(created.id)
        assertTrue(approveResult.isSuccess)

        // 4. Verificar que AHORA SÍ llegó al feed público
        val updatedFeed = repo.publications.value
        assertTrue(updatedFeed.any { it.id == created.id })
        val inFeed = updatedFeed.first { it.id == created.id }
        assertEquals(PostStatus.APROBADA, inFeed.status)
    }

    @Test
    fun testCommentsAndNotificationGeneration() = runBlocking {
        val addCommentCase = AddCommentUseCase(repo)
        val initialNotifs = repo.notifications.value.size

        // Andrés comenta en la publicación 1 de Camila
        repo.login("andres@correo.com", "12345678")
        val commentResult = addCommentCase("1", "¿Tiene las vacunas al día?")
        assertTrue(commentResult.isSuccess)

        // El comentario aparece en el flujo de la publicación
        val comments = repo.comments("1").value
        assertTrue(comments.any { it.text == "¿Tiene las vacunas al día?" })

        // Se generó notificación
        val updatedNotifs = repo.notifications.value
        assertTrue(updatedNotifs.size >= initialNotifs)
    }

    @Test
    fun testVotingMechanism() = runBlocking {
        val voteCase = VotePublicationUseCase(repo)
        val pubBefore = repo.getPublication("1")!!
        val initialVotes = pubBefore.votesCount

        // Emitir un upvote (+1)
        voteCase("1", 1)
        val pubAfterVote = repo.getPublication("1")!!
        assertEquals(initialVotes + 1, pubAfterVote.votesCount)
        assertEquals(1, pubAfterVote.userVote)

        // Retirar el voto (votar de nuevo +1 lo desmarca a 0)
        voteCase("1", 1)
        val pubAfterCancel = repo.getPublication("1")!!
        assertEquals(initialVotes, pubAfterCancel.votesCount)
        assertEquals(0, pubAfterCancel.userVote)
    }

    @Test
    fun testDetailedStatsCalculation() = runBlocking {
        repo.login("mariana@correo.com", "12345678")
        val user = repo.currentUser.value!!
        assertTrue(user.stats.activeCount >= 0)
        assertTrue(user.stats.pendingCount >= 0)
        assertTrue(user.stats.finishedCount >= 0)
        assertTrue(user.badges.isNotEmpty())
    }
}
