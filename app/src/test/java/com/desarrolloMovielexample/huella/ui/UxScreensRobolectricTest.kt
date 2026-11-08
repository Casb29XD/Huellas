package com.desarrolloMovielexample.huella.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import com.desarrolloMovielexample.huella.core.theme.HuellaTheme
import com.desarrolloMovielexample.huella.domain.model.User
import com.desarrolloMovielexample.huella.domain.repository.FakeRepository
import com.desarrolloMovielexample.huella.features.auth.RegisterScreen
import com.desarrolloMovielexample.huella.features.auth.RegisterViewModel
import com.desarrolloMovielexample.huella.features.feed.HomeScreen
import com.desarrolloMovielexample.huella.features.feed.HomeViewModel
import com.desarrolloMovielexample.huella.features.feed.PublicationDetailScreen
import com.desarrolloMovielexample.huella.features.feed.PublicationDetailViewModel
import com.desarrolloMovielexample.huella.features.profile.SettingsScreen
import com.desarrolloMovielexample.huella.features.profile.SettingsViewModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

// Pantallas con FakeRepository; "es" para que LocalizedText no traduzca los textos.
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es")
class UxScreensRobolectricTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `home sin FAB y con margen inferior tras la ultima tarjeta`() {
        compose.setContent {
            HuellaTheme(darkTheme = false) {
                HomeScreen({}, {}, {}, {}, {}, viewModel = HomeViewModel(FakeRepository))
            }
        }
        // HomeViewModel still simulates the first load with delay(600) on the main looper.
        ShadowLooper.idleMainLooper(1, TimeUnit.SECONDS)
        compose.waitForIdle()

        compose.onAllNodesWithContentDescription("Crear publicación").assertCountEquals(0)

        val pubs = FakeRepository.publications.value.size
        // header, search, category chips, counter + one item per publication
        compose.onNodeWithTag("feed_list").performScrollToIndex(3 + pubs)
        val list = compose.onNodeWithTag("feed_list").getUnclippedBoundsInRoot()
        val lastCard = compose.onAllNodesWithTag("feed_card").onLast().getUnclippedBoundsInRoot()
        val gap = list.bottom - lastCard.bottom
        // 14dp card spacing + 24dp content padding; without bottom padding the gap would be only 14dp.
        assertTrue("gap after last card = $gap", gap >= 30.dp)
    }

    @Test
    fun `detalle muestra solo la foto real y ningun caption de depuracion`() {
        val id = FakeRepository.publications.value.first().id
        compose.setContent {
            HuellaTheme(darkTheme = false) {
                PublicationDetailScreen(
                    publicationId = id,
                    onBack = {},
                    onOpenAuthor = {},
                    onOpenMap = {},
                    viewModel = PublicationDetailViewModel(id, FakeRepository),
                )
            }
        }
        compose.onAllNodesWithTag("gallery_slide").assertCountEquals(1)
        compose.onAllNodes(hasText("foto 1/", substring = true)).assertCountEquals(0)
        compose.onAllNodes(hasText("foto:", substring = true)).assertCountEquals(0)
    }

    @Test
    fun `registro sin aceptar terminos muestra el error bajo la casilla`() {
        compose.setContent {
            HuellaTheme(darkTheme = false) {
                RegisterScreen(onBack = {}, onRegistered = {}, viewModel = RegisterViewModel(FakeRepository))
            }
        }
        compose.onNode(hasText("Crear cuenta") and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithText("Debes aceptar los términos y condiciones").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `ajustes muestra la cuenta y eliminar cuenta`() {
        val user = User(id = "u_test", name = "Ana Prueba", email = "ana@huella.test", city = "Medellín", points = 0, initials = "AP")
        compose.setContent {
            HuellaTheme(darkTheme = false) {
                SettingsScreen(
                    onBack = {},
                    onEditProfile = {},
                    onChangePassword = {},
                    onLoggedOut = {},
                    viewModel = SettingsViewModel(FakeRepository),
                    user = user,
                )
            }
        }
        compose.onNodeWithText("ana@huella.test").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Eliminar cuenta").performScrollTo().assertIsDisplayed()
    }
}
