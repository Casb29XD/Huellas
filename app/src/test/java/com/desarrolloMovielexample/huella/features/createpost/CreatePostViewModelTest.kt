package com.desarrolloMovielexample.huella.features.createpost

import android.net.Uri
import com.desarrolloMovielexample.huella.domain.repository.FakeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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
}
