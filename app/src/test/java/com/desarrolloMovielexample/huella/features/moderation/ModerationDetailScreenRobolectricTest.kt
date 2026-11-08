package com.desarrolloMovielexample.huella.features.moderation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.desarrolloMovielexample.huella.core.theme.HuellaTheme
import com.desarrolloMovielexample.huella.data.remote.AiReview
import com.desarrolloMovielexample.huella.domain.repository.FakeRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es")
class ModerationDetailScreenRobolectricTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(analyze: suspend (String) -> AiReview?) {
        compose.setContent {
            HuellaTheme {
                ModerationDetailScreen(
                    itemId = "102",
                    onBack = {},
                    onDone = { _ -> },
                    viewModel = ModerationDetailViewModel(FakeRepository, analyze),
                )
            }
        }
    }

    @Test
    fun `tarjeta de IA muestra veredicto, riesgo y motivos`() {
        show { AiReview("RECHAZAR", 0.75, listOf("Menciona un precio"), "Venta de animales") }
        compose.onNodeWithText("Sugerencia de IA").performScrollTo()
        compose.onNodeWithText("Analizar con IA").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Sugiere rechazar").assertExists()
        compose.onNodeWithText("Riesgo: 75 %").assertExists()
        compose.onNodeWithText("• Menciona un precio").assertExists()
    }

    @Test
    fun `sin DEEPSEEK_API_KEY muestra el aviso`() {
        show { null }
        compose.onNodeWithText("Analizar con IA").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("La moderación con IA no está configurada (DEEPSEEK_API_KEY)").assertExists()
    }
}
