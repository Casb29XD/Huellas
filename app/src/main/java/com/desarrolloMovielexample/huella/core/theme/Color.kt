package com.desarrolloMovielexample.huella.core.theme

import androidx.compose.ui.graphics.Color
import com.desarrolloMovielexample.huella.domain.model.Category
import com.desarrolloMovielexample.huella.domain.model.Level
import com.desarrolloMovielexample.huella.domain.model.PostStatus
import com.desarrolloMovielexample.huella.domain.model.RequestStatus

/** Design tokens taken 1:1 from the Huella design file. */
object HuellaColors {
    // Brand
    val Primary = Color(0xFF2E7D5B)
    val PrimaryDark = Color(0xFF1F5C41)
    val PrimaryContainer = Color(0xFFDCEBE2)   // selected pill / chip bg
    val PrimarySoft = Color(0xFFE4F2EA)        // "Aprobada", "Vacunado" bg
    val PrimaryTint = Color(0xFFF1F5F2)        // stripe light band
    val Secondary = Color(0xFFF4A261)
    val SecondaryContainer = Color(0xFFFCEBD9)

    // Surfaces
    val Background = Color(0xFFFAFAF7)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceMuted = Color(0xFFF1F2EE)
    val SurfaceAlt = Color(0xFFF6F7F4)

    // Text
    val TextPrimary = Color(0xFF1F2A24)
    val TextBody = Color(0xFF3F4A44)
    val TextSecondary = Color(0xFF5F6A63)
    val TextTertiary = Color(0xFF8A8F88)

    // Lines
    val Outline = Color(0xFFC7CBC6)
    val OutlineVariant = Color(0xFFE3E5DF)
    val Divider = Color(0xFFECEEEA)

    // Feedback
    val Error = Color(0xFFD64545)
    val ErrorDark = Color(0xFFB03030)
    val ErrorContainer = Color(0xFFFBE3E3)
    val WarningContainer = Color(0xFFFFF1CC)
    val WarningDark = Color(0xFF8A5A00)
    val Info = Color(0xFF3A7BD5)
    val InfoDark = Color(0xFF2A5FAB)
    val InfoContainer = Color(0xFFE3EDFA)
    val NoVaccineBg = Color(0xFFF7ECE2)
    val NoVaccineFg = Color(0xFF9A5B2B)
    val Disabled = Color(0xFFE3E5DF)
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
