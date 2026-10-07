package com.desarrolloMovielexample.huella.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.core.theme.HuellaShapes
import com.desarrolloMovielexample.huella.domain.model.Category
import com.desarrolloMovielexample.huella.domain.model.Publication

/** Icon for an animal type ("Perro", "Gato", ...). */
fun speciesIcon(species: String): Int = when (species) {
    "Perro" -> R.drawable.icon_animal_perro
    "Gato" -> R.drawable.icon_animal_gato
    "Ave" -> R.drawable.icon_animal_ave
    "Conejo" -> R.drawable.icon_animal_conejo
    "Roedor" -> R.drawable.icon_animal_roedor
    else -> R.drawable.icon_animal_otro
}

fun sizeIcon(size: String): Int = when (size) {
    "Pequeño" -> R.drawable.icon_attr_tamano_pequeno
    "Grande" -> R.drawable.icon_attr_tamano_grande
    else -> R.drawable.icon_attr_tamano_mediano
}

fun sexIcon(sex: String?): Int = when (sex) {
    "Macho" -> R.drawable.icon_attr_sexo_macho
    "Hembra" -> R.drawable.icon_attr_sexo_hembra
    else -> R.drawable.icon_attr_sexo_desconocido
}

fun categoryIcon(category: Category, filled: Boolean = false): Int = when (category) {
    Category.ADOPCION -> if (filled) R.drawable.icon_cat_adopcion_filled else R.drawable.icon_cat_adopcion_outline
    Category.PERDIDOS -> if (filled) R.drawable.icon_cat_perdidos_filled else R.drawable.icon_cat_perdidos_outline
    Category.ENCONTRADOS -> if (filled) R.drawable.icon_cat_encontrados_filled else R.drawable.icon_cat_encontrados_outline
    Category.TEMPORAL -> if (filled) R.drawable.icon_cat_temporal_filled else R.drawable.icon_cat_temporal_outline
    Category.VETERINARIA -> if (filled) R.drawable.icon_cat_veterinaria_filled else R.drawable.icon_cat_veterinaria_outline
}

/** White card, radius 12, hairline shadow (design: 0 1px 2px + 1px ring). */
fun Modifier.huellaCard(): Modifier = this
    .shadow(1.dp, HuellaShapes.Card, clip = false, ambientColor = HuellaColors.TextPrimary, spotColor = HuellaColors.TextPrimary)
    .clip(HuellaShapes.Card)
    .background(Color.White)
    .border(1.dp, HuellaColors.TextPrimary.copy(alpha = 0.05f), HuellaShapes.Card)

@Composable
private fun VaccineBadge(vaccinated: Boolean, compact: Boolean) {
    val fg = if (vaccinated) HuellaColors.Primary else HuellaColors.NoVaccineFg
    val icon = if (vaccinated) R.drawable.icon_attr_vacunado else R.drawable.icon_attr_sin_vacunas
    val text = if (vaccinated) "Vacunado" else "Sin vacunas"
    Row(
        if (compact) Modifier else Modifier
            .background(if (vaccinated) HuellaColors.PrimarySoft else HuellaColors.NoVaccineBg, HuellaShapes.Tag)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 4.dp),
    ) {
        HuellaIcon(icon, tint = fg, size = if (compact) 17.dp else 18.dp)
        Text(text, color = fg, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun CityAndTime(publication: Publication, compact: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (compact) Arrangement.spacedBy(10.dp) else Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            HuellaIcon(R.drawable.icon_attr_ubicacion, tint = HuellaColors.TextTertiary, size = if (compact) 18.dp else 20.dp)
            Text(publication.city, style = MaterialTheme.typography.bodySmall, color = HuellaColors.TextTertiary)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = HuellaColors.TextTertiary, modifier = Modifier.size(if (compact) 14.dp else 15.dp))
            Text(publication.timeAgo, style = MaterialTheme.typography.bodySmall, color = HuellaColors.TextTertiary)
        }
    }
}

/** "Tarjeta de publicación" (vertical): 4:3 photo with category chip, title 16/500, attributes, city + time. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PublicationCard(
    publication: Publication,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showPhoto: Boolean = true,
) {
    Column(
        modifier
            .fillMaxWidth()
            .huellaCard()
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f),
        ) {
            PetImage(publication, Modifier.matchParentSize(), showPhoto = showPhoto)
            CategoryChip(publication.category, Modifier.padding(10.dp))
        }
        Column(
            Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(publication.title, style = MaterialTheme.typography.titleMedium, color = HuellaColors.TextPrimary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                AttributeInline(speciesIcon(publication.species), publication.species)
                AttributeInline(R.drawable.icon_attr_raza_aproximada, publication.breed)
                AttributeInline(sizeIcon(publication.size), publication.size)
                if (publication.category != Category.VETERINARIA) VaccineBadge(publication.vaccinated, compact = false)
            }
            CityAndTime(publication, compact = false, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun AttributeInline(icon: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        HuellaIcon(icon, tint = HuellaColors.TextSecondary, size = 21.dp)
        Text(text, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = HuellaColors.TextSecondary)
    }
}

/**
 * "Variante horizontal compacta": 96dp thumb with small category chip, title 15/500 (ellipsis),
 * "Perro · Labrador mestizo · Grande", vaccine, city + time. [trailingTag] e.g. a status tag next to the title.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PublicationRowCard(
    publication: Publication,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showPhoto: Boolean = true,
    trailingTag: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .huellaCard()
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(10.dp)),
        ) {
            PetImage(publication, Modifier.matchParentSize(), showLabel = false, showPhoto = showPhoto)
            CategoryChip(publication.category, Modifier.padding(6.dp), small = true)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    publication.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = HuellaColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                trailingTag?.invoke()
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                val dot = @Composable { Text("·", style = MaterialTheme.typography.bodySmall, color = HuellaColors.Outline) }
                Text(publication.species, style = MaterialTheme.typography.bodySmall, color = HuellaColors.TextSecondary)
                dot()
                Text(publication.breed, style = MaterialTheme.typography.bodySmall, color = HuellaColors.TextSecondary)
                dot()
                Text(publication.size, style = MaterialTheme.typography.bodySmall, color = HuellaColors.TextSecondary)
                if (publication.category != Category.VETERINARIA) VaccineBadge(publication.vaccinated, compact = true)
            }
            CityAndTime(publication, compact = true)
        }
    }
}
