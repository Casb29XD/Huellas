package com.desarrolloMovielexample.huella.core.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.core.theme.Manrope
import com.desarrolloMovielexample.huella.core.theme.color
import com.desarrolloMovielexample.huella.core.theme.toneColor
import com.desarrolloMovielexample.huella.domain.model.Level
import com.desarrolloMovielexample.huella.domain.model.Publication
import kotlin.math.sqrt

/**
 * Design photo placeholder: 135° stripes, 8dp [tone] / 8dp #F1F5F2, optional monospace label
 * ("foto: Luna busca un hogar tranquilo") at [labelAlignment].
 */
@Composable
fun StripedPlaceholder(
    tone: Color,
    modifier: Modifier = Modifier,
    label: String? = null,
    labelAlignment: Alignment = Alignment.BottomEnd,
) {
    Box(modifier.clipToBounds()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(HuellaColors.PrimaryTint)
            val band = 8.dp.toPx()
            val step = band * 2 * sqrt(2f)
            var c = 0f
            val limit = size.width + size.height
            while (c <= limit + step) {
                // line x + y = c, drawn as a band of width [band]
                drawLine(
                    color = tone,
                    start = Offset(c, 0f),
                    end = Offset(c - size.height, size.height),
                    strokeWidth = band,
                )
                c += step
            }
        }
        if (label != null) {
            Text(
                label,
                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = HuellaColors.TextSecondary),
                maxLines = 1,
                modifier = Modifier
                    .align(labelAlignment)
                    .padding(8.dp)
                    .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

/**
 * Publication photo: striped placeholder (design look) with the random photo from Coil on top.
 * Set [showPhoto] = false to render exactly the design placeholder.
 */
@Composable
fun PetImage(
    imageUrl: String?,
    tone: Color,
    modifier: Modifier = Modifier,
    label: String? = null,
    showPhoto: Boolean = true,
    contentDescription: String? = null,
) {
    Box(modifier.clipToBounds()) {
        StripedPlaceholder(tone = tone, label = label, modifier = Modifier.fillMaxSize())
        if (showPhoto && imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Convenience overload for a [Publication]. Label "foto: <title>" is only visible while the photo is missing. */
@Composable
fun PetImage(
    publication: Publication,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    showPhoto: Boolean = true,
) {
    PetImage(
        imageUrl = publication.imageUrl,
        tone = toneColor(publication.tone),
        label = if (showLabel) "foto: ${publication.photoLabel}" else null,
        showPhoto = showPhoto,
        contentDescription = publication.title,
        modifier = modifier,
    )
}

/** Circle with initials (white 500). Color defaults to the person's level color, as in the design. */
@Composable
fun Avatar(
    initials: String,
    modifier: Modifier = Modifier,
    color: Color = HuellaColors.LevelProtector,
    size: Dp = 40.dp,
    borderColor: Color? = null,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .then(if (borderColor != null) Modifier.border(3.dp, borderColor, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials,
            color = Color.White,
            style = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = (size.value * 0.36f).sp),
        )
    }
}

@Composable
fun Avatar(initials: String, level: Level, modifier: Modifier = Modifier, size: Dp = 40.dp) =
    Avatar(initials = initials, color = level.color, size = size, modifier = modifier)
