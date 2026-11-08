package com.desarrolloMovielexample.huella.core.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.desarrolloMovielexample.huella.domain.model.Category
import com.desarrolloMovielexample.huella.domain.model.Level
import com.desarrolloMovielexample.huella.domain.model.PostStatus
import com.desarrolloMovielexample.huella.domain.model.RequestStatus

/**
 * Design tokens taken 1:1 from the Huella design file. Neutral / container tokens switch to a dark
 * counterpart when [HuellaTheme] runs in dark mode; brand, category and level colors stay the same.
 */
object HuellaColors {
    // ponytail: one global flag set by HuellaTheme (snapshot state, so readers recompose); fine for one theme per app.
    var isDark by mutableStateOf(false)
        internal set

    private fun pick(light: Color, dark: Color): Color = if (isDark) dark else light

    // Brand
    val Primary = Color(0xFF2E7D5B)
    val PrimaryDark get() = pick(Color(0xFF1F5C41), Color(0xFF9ED7B8))
    val PrimaryContainer get() = pick(Color(0xFFDCEBE2), Color(0xFF1F3D2E))   // selected pill / chip bg
    val PrimarySoft get() = pick(Color(0xFFE4F2EA), Color(0xFF1D3A2B))        // "Aprobada", "Vacunado" bg
    val PrimaryTint get() = pick(Color(0xFFF1F5F2), Color(0xFF1E2621))        // stripe light band
    val Secondary = Color(0xFFF4A261)
    val SecondaryContainer get() = pick(Color(0xFFFCEBD9), Color(0xFF3D2C1C))

    // Surfaces
    val Background get() = pick(Color(0xFFFAFAF7), Color(0xFF121714))
    val Surface get() = pick(Color(0xFFFFFFFF), Color(0xFF1B211D))
    val SurfaceMuted get() = pick(Color(0xFFF1F2EE), Color(0xFF242B26))
    val SurfaceAlt get() = pick(Color(0xFFF6F7F4), Color(0xFF1F2621))

    // Text
    val TextPrimary get() = pick(Color(0xFF1F2A24), Color(0xFFE6ECE7))
    val TextBody get() = pick(Color(0xFF3F4A44), Color(0xFFCCD4CE))
    val TextSecondary get() = pick(Color(0xFF5F6A63), Color(0xFFA9B3AC))
    // Claro: 0xFF686E67 da 5.0:1 sobre Background (antes 8A8F88, 3.16:1); WCAG AA pide 4.5:1 en texto pequeño (ítem 58).
    val TextTertiary get() = pick(Color(0xFF686E67), Color(0xFF8C968F))

    // Lines
    val Outline get() = pick(Color(0xFFC7CBC6), Color(0xFF4A534D))
    val OutlineVariant get() = pick(Color(0xFFE3E5DF), Color(0xFF333B36))
    val Divider get() = pick(Color(0xFFECEEEA), Color(0xFF2A312C))

    // Feedback
    val Error = Color(0xFFD64545)
    val ErrorDark get() = pick(Color(0xFFB03030), Color(0xFFF08A8A))
    val ErrorContainer get() = pick(Color(0xFFFBE3E3), Color(0xFF3D1F1F))
    val WarningContainer get() = pick(Color(0xFFFFF1CC), Color(0xFF3A3018))
    val WarningDark get() = pick(Color(0xFF8A5A00), Color(0xFFE6B85C))
    val Info = Color(0xFF3A7BD5)
    val InfoDark get() = pick(Color(0xFF2A5FAB), Color(0xFF8DB4EE))
    val InfoContainer get() = pick(Color(0xFFE3EDFA), Color(0xFF1D2B3F))
    val NoVaccineBg get() = pick(Color(0xFFF7ECE2), Color(0xFF3A2A1E))
    val NoVaccineFg get() = pick(Color(0xFF9A5B2B), Color(0xFFE0A47A))
    val Disabled get() = pick(Color(0xFFE3E5DF), Color(0xFF333B36))
    val Scrim = Color(0x8C1F2A24)

    // Categories
    val CatAdopcion = Color(0xFF2E7D5B)
    val CatPerdidos = Color(0xFFD64545)
    val CatEncontrados = Color(0xFF3A7BD5)
    val CatTemporal = Color(0xFF7B5EA7)
    val CatVeterinaria = Color(0xFF1F9E89)

    // Levels
    val LevelAmigoAnimal = Color(0xFF8A8F88)
    val LevelProtector = Color(0xFFB87333)
    val LevelGuardian = Color(0xFF7A9CC6)
    val LevelHeroe = Color(0xFFE0A526)
}

val Category.color: Color
    get() = when (this) {
        Category.ADOPCION -> HuellaColors.CatAdopcion
        Category.PERDIDOS -> HuellaColors.CatPerdidos
        Category.ENCONTRADOS -> HuellaColors.CatEncontrados
        Category.TEMPORAL -> HuellaColors.CatTemporal
        Category.VETERINARIA -> HuellaColors.CatVeterinaria
    }

val Level.color: Color
    get() = when (this) {
        Level.AMIGO_ANIMAL -> HuellaColors.LevelAmigoAnimal
        Level.PROTECTOR -> HuellaColors.LevelProtector
        Level.GUARDIAN -> HuellaColors.LevelGuardian
        Level.HEROE -> HuellaColors.LevelHeroe
    }

/** (background, foreground) */
val PostStatus.colors: Pair<Color, Color>
    get() = when (this) {
        PostStatus.EN_REVISION -> HuellaColors.WarningContainer to HuellaColors.WarningDark
        PostStatus.APROBADA -> HuellaColors.PrimarySoft to HuellaColors.Primary
        PostStatus.RECHAZADA -> HuellaColors.ErrorContainer to HuellaColors.ErrorDark
    }

/** (background, foreground) */
val RequestStatus.colors: Pair<Color, Color>
    get() = when (this) {
        RequestStatus.PENDIENTE -> HuellaColors.WarningContainer to HuellaColors.WarningDark
        RequestStatus.ACEPTADA -> HuellaColors.PrimarySoft to HuellaColors.Primary
        RequestStatus.RECHAZADA -> HuellaColors.ErrorContainer to HuellaColors.ErrorDark
    }

/** Light stripe tone stored in the domain as ARGB Long. */
fun toneColor(argb: Long): Color = Color(argb)
