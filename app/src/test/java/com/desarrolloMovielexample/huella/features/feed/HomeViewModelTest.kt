package com.desarrolloMovielexample.huella.features.feed

import com.desarrolloMovielexample.huella.domain.model.Category
import com.desarrolloMovielexample.huella.domain.repository.FakeRepository
import com.desarrolloMovielexample.huella.domain.repository.HuellaRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    /** loadFeed espera a [gate]: simula un servidor lento. */
    private class PagedRepo : HuellaRepository by FakeRepository {
        val calls = mutableListOf<List<Any?>>()
        val gate = CompletableDeferred<Unit>()
        override suspend fun loadFeed(category: Category?, species: String?, city: String?, reset: Boolean): Result<Boolean> {
            calls += listOf(category, species, city, reset)
            gate.await()
            return Result.success(true)
        }
    }

    @Test
    fun `item 56 - el skeleton dura lo que la carga real, sin delay fijo`() = runTest(dispatcher) {
        val repo = PagedRepo()
        val vm = HomeViewModel(repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceTimeBy(10_000) // mucho más que el antiguo delay(600)
        assertTrue("sigue cargando mientras el servidor no responde", vm.uiState.value.isLoading)
        repo.gate.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.hasMore)
    }

    @Test
    fun `item 32 - filtros de servidor recargan desde la pagina 1 y loadMore pide la siguiente`() = runTest(dispatcher) {
        val repo = PagedRepo().apply { gate.complete(Unit) }
        val vm = HomeViewModel(repo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(listOf(null, null, null, true), repo.calls.single())
        vm.onCategorySelected(Category.PERDIDOS)
        advanceUntilIdle()
        assertEquals(listOf(Category.PERDIDOS, null, null, true), repo.calls.last())
        vm.loadMore()
        advanceUntilIdle()
        assertEquals(listOf(Category.PERDIDOS, null, null, false), repo.calls.last())
        vm.onQueryChange("luna") // el texto se filtra en memoria: no vuelve al servidor
        advanceUntilIdle()
        assertEquals(3, repo.calls.size)
    }
}
