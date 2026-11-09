package com.desarrolloMovielexample.huella.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.desarrolloMovielexample.huella.core.components.PublicationCard
import com.desarrolloMovielexample.huella.core.theme.HuellaTheme
import com.desarrolloMovielexample.huella.domain.repository.FakeRepository
import com.desarrolloMovielexample.huella.features.auth.OnboardingScreen
import com.desarrolloMovielexample.huella.features.createpost.CreatePostScreen
import com.desarrolloMovielexample.huella.features.createpost.CreatePostViewModel
import com.desarrolloMovielexample.huella.features.feed.PublicationDetailScreen
import com.desarrolloMovielexample.huella.features.feed.PublicationDetailViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Hallazgos visuales verificables sin capturas: títulos a dos líneas (45), onboarding estable (49), áreas táctiles (50),
 * grilla de categorías sin tarjeta huérfana (47). */
@RunWith(RobolectricTestRunner::class)
// NATIVE: el modo legacy de Robolectric no mide texto y todo cabe en una línea.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "es")
class VisualItemsRobolectricTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `item 45 - titulo largo de la tarjeta ocupa dos lineas con elipsis`() {
        val title = "Luna, perrita criolla muy cariñosa que busca un hogar responsable en el Valle de Aburrá"
        val pub = FakeRepository.publications.value.first().copy(title = title)
        compose.setContent { HuellaTheme(darkTheme = false) { PublicationCard(pub, onClick = {}) } }
        val layouts = mutableListOf<TextLayoutResult>()
        // Sin fusionar: el nodo clickable de la tarjeta fusiona varios textos.
        compose.onNodeWithText(title, useUnmergedTree = true).fetchSemanticsNode().config
            .getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
        val layout = layouts.single()
        assertEquals(2, layout.lineCount)
        assertEquals(true, layout.hasVisualOverflow)
    }

    @Test
    fun `item 49 y 50 - el titulo del onboarding no salta entre paginas y Omitir mide 48dp`() {
        compose.setContent { HuellaTheme(darkTheme = false) { OnboardingScreen(onFinish = {}) } }
        compose.onNodeWithText("Omitir").assertHeightIsAtLeast(48.dp)
        // Título de una línea (página 1) y de dos (página 2): con la altura fija del bloque de texto, empiezan igual.
        val top1 = compose.onNodeWithText("Adopta, no compres").getUnclippedBoundsInRoot().top
        compose.onNodeWithText("Siguiente").performClick()
        compose.waitForIdle()
        val top2 = compose.onNodeWithText("Ayuda a mascotas perdidas a volver a casa").getUnclippedBoundsInRoot().top
        assertEquals(top1.value, top2.value, 0.5f)
    }

    @Test
    fun `item 50 - Ver perfil del detalle mide al menos 48dp`() {
        val id = FakeRepository.publications.value.first().id
        compose.setContent {
            HuellaTheme(darkTheme = false) {
                PublicationDetailScreen(id, onBack = {}, onOpenAuthor = {}, onOpenMap = {}, viewModel = PublicationDetailViewModel(id, FakeRepository))
            }
        }
        // minimumInteractiveComponentSize amplía el área táctil (touchBounds), no los bounds del texto.
        val node = compose.onNodeWithText("Ver perfil").performScrollTo().fetchSemanticsNode()
        val height = with(compose.density) { node.touchBoundsInRoot.height.toDp() }
        assertTrue("área táctil de Ver perfil = $height", height >= 47.5.dp)
    }

    @Test
    fun `item 47 - categorias con filas de igual altura y Veterinaria ocupando la fila completa`() {
        compose.setContent { HuellaTheme(darkTheme = false) { CreatePostScreen(onBack = {}, onPublished = {}, viewModel = CreatePostViewModel(FakeRepository)) } }
        fun card(label: String) = compose.onNode(hasText(label) and hasClickAction()).getUnclippedBoundsInRoot()
        val adopcion = card("Adopción")
        val perdidos = card("Perdidos")
        val veterinaria = card("Veterinaria")
        assertEquals(adopcion.height.value, perdidos.height.value, 0.5f)
        assertTrue("Veterinaria ${veterinaria.width} vs ${adopcion.width}", veterinaria.width.value > adopcion.width.value * 1.8f)
    }
}
