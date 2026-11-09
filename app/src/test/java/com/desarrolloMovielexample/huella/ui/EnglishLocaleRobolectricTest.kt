package com.desarrolloMovielexample.huella.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.desarrolloMovielexample.huella.core.theme.HuellaTheme
import com.desarrolloMovielexample.huella.domain.model.User
import com.desarrolloMovielexample.huella.domain.repository.FakeRepository
import com.desarrolloMovielexample.huella.features.auth.LoginScreen
import com.desarrolloMovielexample.huella.features.auth.LoginViewModel
import com.desarrolloMovielexample.huella.features.feed.HomeScreen
import com.desarrolloMovielexample.huella.features.feed.HomeViewModel
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.components.PublicationCard
import com.desarrolloMovielexample.huella.core.utils.AppError
import com.desarrolloMovielexample.huella.data.remote.SupabaseRepository
import com.desarrolloMovielexample.huella.data.remote.relativeTimeText
import com.desarrolloMovielexample.huella.domain.repository.HuellaRepository
import com.desarrolloMovielexample.huella.features.auth.ForgotPasswordScreen
import com.desarrolloMovielexample.huella.features.auth.ForgotPasswordViewModel
import com.desarrolloMovielexample.huella.features.profile.LevelsScreen
import com.desarrolloMovielexample.huella.features.profile.LevelsViewModel
import com.desarrolloMovielexample.huella.features.profile.SettingsScreen
import com.desarrolloMovielexample.huella.features.profile.SettingsViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

// Locale fijo en inglés: los textos, errores de ViewModel y descripciones salen de values-en (ítems 37 y 58).
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en")
class EnglishLocaleRobolectricTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `login en ingles incluye los errores del ViewModel`() {
        compose.setContent {
            HuellaTheme(darkTheme = false) {
                LoginScreen(onLoginSuccess = {}, onRegister = {}, onForgotPassword = {}, viewModel = LoginViewModel(FakeRepository))
            }
        }
        compose.onNodeWithText("Welcome back").assertExists()
        compose.onNodeWithContentDescription("Show password").assertExists()
        compose.onNode(hasText("Sign in") and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithText("Email is required").assertExists()
        compose.onNodeWithText("Password is required").assertExists()
    }

    @Test
    fun `home vacio en ingles`() {
        val vm = HomeViewModel(FakeRepository)
        compose.setContent {
            HuellaTheme(darkTheme = false) {
                HomeScreen({}, {}, {}, {}, {}, viewModel = vm)
            }
        }
        ShadowLooper.idleMainLooper(1, TimeUnit.SECONDS)
        vm.onQueryChange("zzzz-sin-resultados")
        compose.waitForIdle()
        compose.onNodeWithText("No pets found").assertExists()
        compose.onNodeWithText("Clear filters").assertExists()
        compose.onNodeWithContentDescription("Clear search").assertExists()
    }

    @Test
    fun `ajustes en ingles`() {
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
        compose.onNodeWithText("Settings").assertExists()
        compose.onNodeWithText("Delete account").performScrollTo().assertExists()
        compose.onNodeWithText("Sign out").performScrollTo().assertExists()
        compose.onNodeWithContentDescription("Back").assertExists()
    }

    @Test
    fun `niveles en ingles con todas las acciones que otorga el servidor (item 15)`() {
        compose.setContent {
            HuellaTheme(darkTheme = false) { LevelsScreen(onBack = {}, viewModel = LevelsViewModel(SupabaseRepository)) }
        }
        listOf("Create a post", "Post approved", "Comment on a post", "Report a sighting", "Adoption completed").forEach {
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(it))
            compose.onNodeWithText(it).assertExists()
        }
    }

    @Test
    fun `tarjeta con tiempo relativo y error del repositorio en ingles (item 37)`() {
        val now = System.currentTimeMillis()
        val pub = FakeRepository.publications.value.first().copy(timeAgo = relativeTimeText(now - 2 * 3_600_000L, now))
        // El repositorio falla con un AppError: el texto sale de values-en, no un literal en español.
        val failing = object : HuellaRepository by FakeRepository {
            override suspend fun sendPasswordReset(email: String): Result<Unit> = Result.failure(AppError(R.string.err_no_connection))
        }
        val vm = ForgotPasswordViewModel(failing)
        compose.setContent {
            HuellaTheme(darkTheme = false) {
                androidx.compose.foundation.layout.Column {
                    PublicationCard(publication = pub, onClick = {})
                    ForgotPasswordScreen(onBack = {}, viewModel = vm)
                }
            }
        }
        compose.onNodeWithText("2 h ago").assertExists()
        vm.onEmailChange("ana@huella.test")
        vm.sendLink()
        ShadowLooper.idleMainLooper(1, TimeUnit.SECONDS)
        compose.waitForIdle()
        compose.onAllNodesWithText("We could not reach the server. Check your connection").onFirst().assertExists()
        compose.onAllNodesWithText("hace", substring = true).assertCountEquals(0)
    }
}
