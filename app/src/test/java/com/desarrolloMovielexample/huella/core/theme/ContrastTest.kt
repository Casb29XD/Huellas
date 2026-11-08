package com.desarrolloMovielexample.huella.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test

/** WCAG AA (4.5:1 para texto pequeño) de los tokens de texto sobre los fondos, en tema claro y oscuro (ítems 58 y 59). */
class ContrastTest {
    @After fun reset() { HuellaColors.isDark = false }

    private fun ratio(a: Color, b: Color): Double {
        val (hi, lo) = listOf(a.luminance().toDouble(), b.luminance().toDouble()).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun check(dark: Boolean, pairs: () -> Map<String, Pair<Color, Color>>) {
        HuellaColors.isDark = dark
        val low = pairs().mapValues { (_, p) -> ratio(p.first, p.second) }.filterValues { it < 4.5 }
        assertTrue("contraste < 4.5 (dark=$dark): $low", low.isEmpty())
    }

    private fun textPairs() = with(HuellaColors) {
        listOf("TextPrimary" to TextPrimary, "TextBody" to TextBody, "TextSecondary" to TextSecondary, "TextTertiary" to TextTertiary)
            .flatMap { (n, c) -> listOf("$n/Background" to (c to Background), "$n/Surface" to (c to Surface)) }
            .toMap()
    }

    @Test fun `texto sobre fondos en tema claro`() = check(false, ::textPairs)
    @Test fun `texto sobre fondos en tema oscuro`() = check(true, ::textPairs)
}
