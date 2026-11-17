package com.desarrolloMovielexample.huella.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.desarrolloMovielexample.huella.core.components.ColombiaLocationFields
import com.desarrolloMovielexample.huella.core.theme.HuellaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "es")
class ColombiaLocationFieldsRobolectricTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `departamento y municipio dan Municipio, Departamento y cambiar departamento limpia la ciudad`() {
        var city = "Medellín, Antioquia"
        compose.setContent {
            HuellaTheme {
                var value by remember { mutableStateOf(city) }
                ColombiaLocationFields(
                    value = value,
                    onValueChange = { value = it; city = it },
                    label = "Departamento",
                    municipalityLabel = "Ciudad",
                )
            }
        }
        compose.onNodeWithText("Antioquia").performClick()
        compose.onNodeWithText("Amazonas").performClick()
        compose.runOnIdle { assertEquals("", city) }

        compose.onNodeWithText("Selecciona", substring = true).performClick()
        compose.onNodeWithText("Leticia").performClick()
        compose.runOnIdle { assertEquals("Leticia, Amazonas", city) }
    }
}
