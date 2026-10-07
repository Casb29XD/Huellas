package com.desarrolloMovielexample.huella.core.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.core.theme.HuellaShapes
import com.desarrolloMovielexample.huella.core.theme.color
import com.desarrolloMovielexample.huella.core.theme.colors
import com.desarrolloMovielexample.huella.domain.model.Category
import com.desarrolloMovielexample.huella.domain.model.Level
import com.desarrolloMovielexample.huella.domain.model.PostStatus
import com.desarrolloMovielexample.huella.domain.model.RequestStatus

/** Tinted PNG icon from res/drawable (design icons are alpha masks, e.g. R.drawable.icon_animal_perro). */
@Composable
fun HuellaIcon(@DrawableRes res: Int, modifier: Modifier = Modifier, tint: Color = HuellaColors.TextSecondary, size: Dp = 24.dp) {
    Icon(painterResource(res), contentDescription = null, tint = tint, modifier = modifier.size(size))
}

/** "Chip de categoría": solid category color, white 12/500 text. [small] = 10sp version used on compact cards. */
@Composable
fun CategoryChip(category: Category, modifier: Modifier = Modifier, small: Boolean = false) {
    Text(
        text = category.label,
        color = Color.White,
        style = if (small) TextStyle(fontSize = 10.sp, lineHeight = 10.sp) else MaterialTheme.typography.labelMedium.copy(lineHeight = 12.sp),
        fontFamily = MaterialTheme.typography.labelMedium.fontFamily,
        fontWeight = MaterialTheme.typography.labelMedium.fontWeight,
        modifier = modifier
            .background(category.color, if (small) HuellaShapes.Tag else HuellaShapes.Chip)
            .padding(horizontal = if (small) 7.dp else 10.dp, vertical = if (small) 5.dp else 7.dp),
    )
}

/**
 * "Badge de nivel": paw icon + level name. Solid (white text on level color) by default;
 * [soft] = tinted background + colored text (feed header / public profile).
 */
@Composable
fun LevelBadge(level: Level, modifier: Modifier = Modifier, soft: Boolean = false) {
    val bg = if (soft) level.color.copy(alpha = 0.15f) else level.color
    val fg = if (soft) level.color else Color.White
    Row(
        modifier
            .background(bg, RoundedCornerShape(12.dp))
            .padding(start = 8.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        HuellaIcon(R.drawable.icon_animal_otro, tint = fg, size = 18.dp)
        Text(level.label, color = fg, style = MaterialTheme.typography.labelMedium)
    }
}

/** Generic small status tag (bg + fg + optional icon). */
@Composable
fun StatusTag(
    text: String,
    background: Color,
    content: Color,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    small: Boolean = false,
) {
    Row(
        modifier
            .background(background, HuellaShapes.Tag)
            .padding(horizontal = if (small) 8.dp else 10.dp, vertical = if (small) 5.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) HuellaIcon(icon, tint = content, size = 18.dp)
        Text(
            text,
            color = content,
            style = if (small) MaterialTheme.typography.labelSmall.copy(lineHeight = 11.sp) else MaterialTheme.typography.labelMedium,
            maxLines = 1,
        )
    }
}

/** "Etiqueta de estado de moderación": En revisión / Aprobada / Rechazada with icon. [small] = no icon (compact cards). */
@Composable
fun ModerationStatusTag(status: PostStatus, modifier: Modifier = Modifier, small: Boolean = false) {
    val (bg, fg) = status.colors
    val icon = when (status) {
        PostStatus.EN_REVISION -> R.drawable.icon_estado_en_revision
        PostStatus.APROBADA -> R.drawable.icon_estado_aprobada
        PostStatus.RECHAZADA -> R.drawable.icon_estado_rechazada
    }
    StatusTag(status.label, bg, fg, modifier, icon = if (small) null else icon, small = small)
}

/** Pendiente / Aceptada / Rechazada (Mis solicitudes · Enviadas). */
@Composable
fun RequestStatusTag(status: RequestStatus, modifier: Modifier = Modifier) {
    val (bg, fg) = status.colors
    StatusTag(status.label, bg, fg, modifier, small = true)
}

/**
 * Selectable chip used in filters / create post ("Perro", "Gato"...): 34dp, radius 10.
 * Selected = #DCEBE2 bg + green border/text. [onRemove] adds the trailing "×" (active search filters).
 */
@Composable
fun HuellaFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes leadingIcon: Int? = null,
    onRemove: (() -> Unit)? = null,
) {
    val fg = if (selected) HuellaColors.Primary else HuellaColors.TextBody
    Row(
        modifier
            .height(34.dp)
            .clip(HuellaShapes.FilterChip)
            .background(if (selected) HuellaColors.PrimaryContainer else Color.White)
            .border(BorderStroke(1.dp, if (selected) HuellaColors.Primary else HuellaColors.Outline), HuellaShapes.FilterChip)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (leadingIcon != null) HuellaIcon(leadingIcon, tint = fg, size = 20.dp)
        Text(text, color = fg, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), fontWeight = MaterialTheme.typography.labelMedium.fontWeight)
        if (onRemove != null) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "Quitar",
                tint = fg,
                modifier = Modifier
                    .size(16.dp)
                    .clickable(onClick = onRemove),
            )
        }
    }
}
