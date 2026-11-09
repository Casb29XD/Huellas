package com.desarrolloMovielexample.huella.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Material scheme built from the current [HuellaColors] tokens (call after setting HuellaColors.isDark). */
private fun huellaColorScheme(dark: Boolean): ColorScheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
    primary = HuellaColors.Primary,
    onPrimary = Color.White,
    primaryContainer = HuellaColors.PrimaryContainer,
    onPrimaryContainer = HuellaColors.PrimaryDark,
    inversePrimary = HuellaColors.PrimaryContainer,
    secondary = HuellaColors.Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = HuellaColors.SecondaryContainer,
    onSecondaryContainer = HuellaColors.LevelProtector,
    tertiary = HuellaColors.CatVeterinaria,
    onTertiary = Color.White,
    background = HuellaColors.Background,
    onBackground = HuellaColors.TextPrimary,
    surface = HuellaColors.Surface,
    onSurface = HuellaColors.TextPrimary,
    surfaceVariant = HuellaColors.SurfaceMuted,
    onSurfaceVariant = HuellaColors.TextSecondary,
    surfaceTint = Color.Transparent,
    inverseSurface = HuellaColors.TextPrimary,
    inverseOnSurface = HuellaColors.Background,
    error = HuellaColors.Error,
    onError = Color.White,
    errorContainer = HuellaColors.ErrorContainer,
    onErrorContainer = HuellaColors.ErrorDark,
    outline = HuellaColors.Outline,
    outlineVariant = HuellaColors.OutlineVariant,
    scrim = Color.Black,
    surfaceBright = HuellaColors.Surface,
    surfaceDim = HuellaColors.SurfaceMuted,
    surfaceContainerLowest = HuellaColors.Surface,
    surfaceContainerLow = HuellaColors.Surface,
    surfaceContainer = HuellaColors.Surface,
    surfaceContainerHigh = HuellaColors.Surface,
    surfaceContainerHighest = HuellaColors.SurfaceMuted,
)

private val OnSecondary = Color(0xFF1F2A24)

/** Radii: 12dp cards / fields, 24dp buttons. */
object HuellaShapes {
    val Tag = RoundedCornerShape(6.dp)
    val Chip = RoundedCornerShape(8.dp)
    val FilterChip = RoundedCornerShape(10.dp)
    val Card = RoundedCornerShape(12.dp)
    val Field = RoundedCornerShape(12.dp)
    val Button = RoundedCornerShape(24.dp)
    val Sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val Dialog = RoundedCornerShape(24.dp)
}

private val MaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Follows the system light/dark setting. Dynamic color disabled on purpose (brand palette). */
@Composable
fun HuellaTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    // Set before anything below reads the tokens, so the first frame already has the right palette.
    if (HuellaColors.isDark != darkTheme) HuellaColors.isDark = darkTheme
    val colorScheme = remember(darkTheme) { huellaColorScheme(darkTheme) }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = HuellaTypography,
        shapes = MaterialShapes,
        content = content,
    )
}
