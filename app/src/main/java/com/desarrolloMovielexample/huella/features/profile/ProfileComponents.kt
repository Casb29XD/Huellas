package com.desarrolloMovielexample.huella.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.core.theme.HuellaShapes
import com.desarrolloMovielexample.huella.core.theme.Manrope

/** White card with the design's 1dp #ECEEEA border (profile / settings / levels lists). */
internal fun Modifier.outlinedCard(): Modifier = this
    .clip(HuellaShapes.Card)
    .background(Color.White)
    .border(1.dp, HuellaColors.Divider, HuellaShapes.Card)

/** Light tint of a level color (#F5E6D8 for Protector). */
internal fun softTint(color: Color): Color = lerp(color, Color.White, 0.82f)

/** Big initials avatar (88dp with 4dp soft ring) used in Perfil propio / Perfil público / Editar perfil. */
@Composable
internal fun ProfileAvatar(
    initials: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    ringWidth: Dp = 4.dp,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .then(if (ringWidth > 0.dp) Modifier.border(ringWidth, softTint(color), CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials,
            color = Color.White,
            style = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = (size.value * 0.315f).sp),
        )
    }
}

/** Row of 3 stat boxes ("3 Publicaciones", "1 Adopciones concretadas", ...). */
@Composable
internal fun StatsRow(stats: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        stats.forEach { (value, label) ->
            Column(
                Modifier
                    .weight(1f)
                    .outlinedCard()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    value,
                    style = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
                    color = HuellaColors.TextPrimary,
                )
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
                    color = HuellaColors.TextTertiary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Uppercase 12/500 grey group caption ("HOY", "NOTIFICACIONES", "CUENTA"). */
@Composable
internal fun GroupCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.48.sp),
        color = HuellaColors.TextTertiary,
        modifier = modifier,
    )
}
