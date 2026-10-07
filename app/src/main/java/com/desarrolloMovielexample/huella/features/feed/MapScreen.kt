package com.desarrolloMovielexample.huella.features.feed

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.components.HuellaIcon
import com.desarrolloMovielexample.huella.core.components.PublicationRowCard
import com.desarrolloMovielexample.huella.core.components.categoryIcon
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.core.theme.color
import com.desarrolloMovielexample.huella.domain.model.Category
import com.desarrolloMovielexample.huella.domain.model.Publication
import com.desarrolloMovielexample.huella.domain.repository.FakeRepository
import com.desarrolloMovielexample.huella.domain.repository.HuellaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Lost & found publications for the "Buscar" tab. */
class MapViewModel(repo: HuellaRepository = FakeRepository) : ViewModel() {
    val publications: StateFlow<List<Publication>> = repo.publications
        .map { list -> list.filter { it.category.isLostFound } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.publications.value.filter { it.category.isLostFound })
}

/** Pin positions from the design (390x844 frame), pointing at feed ids. */
private data class MapMarker(val x: Float, val y: Float, val category: Category, val publicationId: String)

private val markers = listOf(
    MapMarker(168f, 300f, Category.PERDIDOS, "8"),
    MapMarker(240f, 380f, Category.ENCONTRADOS, "3"),
    MapMarker(96f, 420f, Category.PERDIDOS, "2"),
    MapMarker(290f, 250f, Category.ENCONTRADOS, "3"),
    MapMarker(130f, 210f, Category.PERDIDOS, "8"),
)

private val MapGround = Color(0xFFE8EFE9)

@Composable
fun MapScreen(
    onBack: () -> Unit,
    onOpenPublication: (String) -> Unit,
    viewModel: MapViewModel = viewModel(),
) {
    val all by viewModel.publications.collectAsStateWithLifecycle()
    var showMap by rememberSaveable { mutableStateOf(true) }
    var showLost by rememberSaveable { mutableStateOf(true) }
    var showFound by rememberSaveable { mutableStateOf(true) }
    val visible = { c: Category -> (c == Category.PERDIDOS && showLost) || (c == Category.ENCONTRADOS && showFound) }
    val posts = all.filter { visible(it.category) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MapGround),
    ) {
        if (showMap) {
            // ponytail: static map placeholder; real tiles/markers arrive with the maps phase.
            BoxWithConstraints(Modifier.fillMaxSize()) {
                MapBackground(Modifier.fillMaxSize())
                markers.filter { visible(it.category) }.forEach { m ->
                    MapPin(
                        category = m.category,
                        modifier = Modifier
                            .offset(x = maxWidth * (m.x / 390f) - 22.dp, y = maxHeight * (m.y / 844f) - 44.dp)
                            .clickable { onOpenPublication(m.publicationId) },
                    )
                }
            }
        }

        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier
                        .shadow(3.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(onClick = onBack)
                        .padding(10.dp),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Atrás", tint = HuellaColors.TextPrimary)
                }
                Row(
                    Modifier
                        .weight(1f)
                        .shadow(3.dp, RoundedCornerShape(22.dp))
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White)
                        .padding(3.dp),
                ) {
                    ToggleSegment("Lista", R.drawable.icon_accion_lista, selected = !showMap, Modifier.weight(1f)) { showMap = false }
                    ToggleSegment("Mapa", R.drawable.icon_accion_mapa, selected = showMap, Modifier.weight(1f)) { showMap = true }
                }
            }
            Row(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LegendChip("Perdidos", HuellaColors.CatPerdidos, showLost) { showLost = !showLost }
                LegendChip("Encontrados", HuellaColors.CatEncontrados, showFound) { showFound = !showFound }
            }

            if (showMap) {
                Box(Modifier.weight(1f))
                Column(
                    Modifier.padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .size(width = 32.dp, height = 4.dp)
                            .shadow(1.dp, RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(2.dp)),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(posts, key = { it.id }) { p ->
                            PublicationRowCard(p, onClick = { onOpenPublication(p.id) }, modifier = Modifier.width(330.dp))
                        }
                    }
                }
            } else {
                LazyColumn(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(HuellaColors.Background),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(posts, key = { it.id }) { p ->
                        PublicationRowCard(p, onClick = { onOpenPublication(p.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleSegment(text: String, @DrawableRes icon: Int, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val fg = if (selected) HuellaColors.TextPrimary else HuellaColors.TextSecondary
    Row(
        modifier
            .height(38.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(if (selected) HuellaColors.PrimaryContainer else Color.Transparent)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HuellaIcon(icon, tint = fg, size = 23.dp)
        Text(text, color = fg, style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp), modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun LegendChip(text: String, dot: Color, active: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .alpha(if (active) 1f else 0.5f)
            .shadow(2.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dot),
        )
        Text(text, style = MaterialTheme.typography.labelMedium, color = HuellaColors.TextPrimary)
    }
}

/** Category pin (filled category icon with a white halo), 44dp, anchored at its bottom center by the caller. */
@Composable
internal fun MapPin(category: Category, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 44.dp) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        HuellaIcon(categoryIcon(category, filled = true), Modifier.requiredSize(size + 6.dp), tint = Color.White, size = size + 6.dp)
        HuellaIcon(categoryIcon(category, filled = true), tint = category.color, size = size)
    }
}

/** Static "city map" drawn with Compose: green-grey ground, parks, a river and a rotated street grid. */
@Composable
internal fun MapBackground(modifier: Modifier = Modifier) {
    Canvas(modifier.background(MapGround)) {
        val park = Color(0xFFCFE3D3)
        val water = Color(0xFFCADDEB)
        val street = Color.White
        drawRoundRect(park, Offset(size.width * 0.08f, size.height * 0.55f), Size(size.width * 0.28f, size.height * 0.12f), CornerRadius(24f))
        drawRoundRect(park, Offset(size.width * 0.62f, size.height * 0.14f), Size(size.width * 0.3f, size.height * 0.09f), CornerRadius(24f))
        drawLine(water, Offset(-20f, size.height * 0.78f), Offset(size.width + 20f, size.height * 0.62f), strokeWidth = 18.dp.toPx())
        rotate(-14f) {
            val minor = 46.dp.toPx()
            val major = 3.dp.toPx()
            var x = -size.width
            var i = 0
            while (x < size.width * 2) {
                drawLine(street, Offset(x, -size.height), Offset(x, size.height * 2), strokeWidth = if (i % 4 == 0) major * 2.5f else major)
                x += minor; i++
            }
            var y = -size.height
            i = 0
            while (y < size.height * 2) {
                drawLine(street, Offset(-size.width, y), Offset(size.width * 2, y), strokeWidth = if (i % 5 == 0) major * 2.5f else major)
                y += minor; i++
            }
        }
    }
}
