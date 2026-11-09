package com.desarrolloMovielexample.huella.ui

import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.core.theme.HuellaTheme
import com.desarrolloMovielexample.huella.domain.repository.FakeRepository
import com.desarrolloMovielexample.huella.features.createpost.CreatePostScreen
import com.desarrolloMovielexample.huella.features.createpost.CreatePostViewModel
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Ítem 59: en modo oscuro las reglas del paso 3 se leen (antes 5C3D00 fijo sobre WarningContainer oscuro, 1.31:1). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es")
class DarkThemeRobolectricTest {
    @get:Rule
    val compose = createComposeRule()

    @After fun reset() { HuellaColors.isDark = false }

    private fun ratio(a: Color, b: Color): Double {
        val (hi, lo) = listOf(a.luminance().toDouble(), b.luminance().toDouble()).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    @Test
    fun `reglas de publicacion legibles en modo oscuro`() {
        val vm = CreatePostViewModel(FakeRepository).apply {
            addPhotos(listOf(Uri.parse("content://foto")))
            nextStep()
            onBreed("Criollo")
            onAge("2 años")
            nextStep()
        }
        compose.setContent { HuellaTheme(darkTheme = true) { CreatePostScreen(onBack = {}, onPublished = {}, viewModel = vm) } }
        val node = compose.onNodeWithText("revisadas por moderadores", substring = true).performScrollTo()
        // Color con el que se compuso el texto (TextLayoutResult) frente al fondo de la tarjeta de reglas.
        val layouts = mutableListOf<TextLayoutResult>()
        node.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
        val textColor = layouts.single().layoutInput.style.color
        val contrast = ratio(textColor, HuellaColors.WarningContainer)
        assertTrue("texto $textColor sobre ${HuellaColors.WarningContainer}: $contrast", contrast >= 4.5)
    }
}
