package com.desarrolloMovielexample.huella.features.moderation

import com.desarrolloMovielexample.huella.core.utils.UiText
import com.desarrolloMovielexample.huella.data.remote.AiReview
import com.desarrolloMovielexample.huella.domain.repository.FakeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ModerationDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(analyze: suspend (String) -> AiReview?) =
        ModerationDetailViewModel(FakeRepository, analyze).also { it.load("102") }

    @Test
    fun `not_configured muestra el aviso de DEEPSEEK_API_KEY`() = runTest(dispatcher) {
        val vm = vm { null }
        vm.analyzeWithAi()
        advanceUntilIdle()
        assertNull(vm.uiState.value.aiReview)
        assertEquals(AI_NOT_CONFIGURED, vm.uiState.value.aiMessage)
    }

    @Test
    fun `ok guarda la sugerencia y RECHAZAR preselecciona el motivo`() = runTest(dispatcher) {
        val review = AiReview("RECHAZAR", 0.9, listOf("Menciona precio"), "venta de animales")
        val vm = vm { id -> assertEquals("102", id); review }
        vm.analyzeWithAi()
        advanceUntilIdle()
        assertEquals(review, vm.uiState.value.aiReview)
        assertNull(vm.uiState.value.aiMessage)
        vm.openRejectSheet()
        assertEquals("Venta de animales", vm.uiState.value.rejectReason)
        assertEquals(false, vm.uiState.value.finished) // solo sugiere, no decide
    }

    @Test
    fun `error muestra el mensaje y no deja sugerencia`() = runTest(dispatcher) {
        val vm = vm { error("Solo moderadores") }
        vm.analyzeWithAi()
        advanceUntilIdle()
        assertNull(vm.uiState.value.aiReview)
        assertEquals(UiText.Raw("Solo moderadores"), vm.uiState.value.aiMessage)
        assertEquals(false, vm.uiState.value.aiLoading)
    }

    @Test
    fun `confirmar sin motivo deja error inline persistente`() = runTest(dispatcher) {
        val vm = vm { null }
        vm.openRejectSheet()
        vm.confirmReject()
        assertEquals(true, vm.uiState.value.reasonError)
        assertNull(vm.uiState.value.rejectReason)
    }
}
