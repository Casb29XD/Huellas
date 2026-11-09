package com.desarrolloMovielexample.huella.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.desarrolloMovielexample.huella.core.theme.HuellaTheme
import com.desarrolloMovielexample.huella.domain.repository.FakeRepository
import com.desarrolloMovielexample.huella.features.auth.LoginScreen
import com.desarrolloMovielexample.huella.features.auth.LoginViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Robolectric arranca en en-US y tomaría values-en; estas pruebas verifican el copy en español.
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es")
class LoginScreenRobolectricTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `enviar vacio muestra el error del campo correo`() {
        compose.setContent {
            HuellaTheme {
                LoginScreen(onLoginSuccess = {}, onRegister = {}, onForgotPassword = {}, viewModel = LoginViewModel(FakeRepository))
            }
        }
        compose.onNodeWithText("Ingresar").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("El correo electrónico es obligatorio").assertExists()
        compose.onNodeWithText("La contraseña es obligatoria").assertExists()
    }
}
