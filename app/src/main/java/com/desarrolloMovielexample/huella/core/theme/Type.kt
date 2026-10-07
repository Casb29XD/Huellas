package com.desarrolloMovielexample.huella.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.desarrolloMovielexample.huella.R

val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
)

private fun style(size: Int, weight: FontWeight, lineHeight: Float, letterSpacing: Float = 0f) = TextStyle(
    fontFamily = Manrope,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

/**
 * Design scale: titles 22/500, subtitles 16/500, body 14/400, labels 12/500.
 * titleLarge = screen titles, titleMedium = section/card titles, bodyMedium = body,
 * labelMedium = field labels / chips, labelLarge = buttons (15/500).
 */
val HuellaTypography = Typography(
    displayLarge = style(48, FontWeight.Medium, 56f),
    displayMedium = style(40, FontWeight.Medium, 48f),
    displaySmall = style(32, FontWeight.Medium, 40f),
    headlineLarge = style(30, FontWeight.Medium, 38f),
    headlineMedium = style(26, FontWeight.Medium, 34f),
    headlineSmall = style(24, FontWeight.Medium, 32f),
    titleLarge = style(22, FontWeight.Medium, 28f),
    titleMedium = style(16, FontWeight.Medium, 21f),
    titleSmall = style(15, FontWeight.Medium, 20f),
    bodyLarge = style(16, FontWeight.Normal, 24f),
    bodyMedium = style(14, FontWeight.Normal, 21f),
    bodySmall = style(12, FontWeight.Normal, 16f),
    labelLarge = style(15, FontWeight.Medium, 20f),
    labelMedium = style(12, FontWeight.Medium, 16f),
    labelSmall = style(11, FontWeight.Medium, 14f),
)
