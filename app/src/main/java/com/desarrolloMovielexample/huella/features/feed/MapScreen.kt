package com.desarrolloMovielexample.huella.features.feed

import android.Manifest
import android.content.Context
import android.location.LocationManager
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
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
import com.desarrolloMovielexample.huella.core.theme.HuellaTheme
import com.desarrolloMovielexample.huella.core.theme.color
import com.desarrolloMovielexample.huella.domain.model.Category
import com.desarrolloMovielexample.huella.domain.model.Publication
import com.desarrolloMovielexample.huella.data.remote.SupabaseRepository
import com.desarrolloMovielexample.huella.domain.repository.HuellaRepository
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel para el mapa: incluye perdidos, encontrados, veterinaria/jornadas y hogar temporal. */
class MapViewModel(private val repo: HuellaRepository = SupabaseRepository) : ViewModel() {
    fun getPublication(id: String): Publication? = repo.getPublication(id)

    val publications: StateFlow<List<Publication>> = repo.publications
        .map { list ->
            list.filter {
                it.category.isLostFound || it.category == Category.VETERINARIA || it.category == Category.TEMPORAL
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            repo.publications.value.filter {
                it.category.isLostFound || it.category == Category.VETERINARIA || it.category == Category.TEMPORAL
            }
        )
}

/** Devuelve las coordenadas geográficas de la publicación o un punto aproximado de su ciudad. */
private fun Publication.toLatLng(): LatLng {
    if (lat != 0.0 && lon != 0.0) return LatLng(lat, lon)
    val hash = id.hashCode()
    val deltaLat = (hash % 100) * 0.0003
    val deltaLon = (hash % 80) * 0.0003
    return when {
        city.contains("Bogot", ignoreCase = true) -> LatLng(4.6533 + deltaLat, -74.0633 + deltaLon)
        city.contains("Cali", ignoreCase = true) -> LatLng(3.4372 + deltaLat, -76.5443 + deltaLon)
        city.contains("Barranquilla", ignoreCase = true) -> LatLng(11.0041 + deltaLat, -74.8070 + deltaLon)
        else -> LatLng(6.2442 + deltaLat, -75.5812 + deltaLon) // Medellín por defecto
    }
}

private val MapGround = Color(0xFFE8EFE9)

@Composable
fun MapScreen(
    onBack: () -> Unit,
    onOpenPublication: (String) -> Unit,
    publicationId: String? = null,
    viewModel: MapViewModel = viewModel(),
) {
    val all by viewModel.publications.collectAsStateWithLifecycle()
    var showMap by rememberSaveable { mutableStateOf(true) }
    var showLost by rememberSaveable { mutableStateOf(true) }
    var showFound by rememberSaveable { mutableStateOf(true) }
    var showVet by rememberSaveable { mutableStateOf(true) }
    val context = LocalContext.current
    var hasLocationPermission by remember { mutableStateOf(false) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        hasLocationPermission = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    val visible = { c: Category ->
        (c == Category.PERDIDOS && showLost) ||
        (c == Category.ENCONTRADOS && showFound) ||
        (c == Category.VETERINARIA && showVet) ||
        (c == Category.TEMPORAL && showFound)
    }
    val selectedPublication = publicationId?.let(viewModel::getPublication)
    val posts = buildList {
        addAll(all.filter { visible(it.category) })
        selectedPublication?.takeIf { selected -> none { it.id == selected.id } }?.let(::add)
    }

    // Al venir desde una publicación, abre el mapa directamente en su marcador.
    val firstCoords = remember(posts, publicationId) {
        posts.firstOrNull { it.id == publicationId }?.toLatLng()
            ?: posts.firstOrNull()?.toLatLng()
            ?: LatLng(6.2442, -75.5812)
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(firstCoords, 13.5f)
    }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        hasLocationPermission = fineGranted || coarseGranted
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location = runCatching {
                manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }.getOrNull()
            if (location != null && publicationId == null) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 14.5f),
                )
            }
        }
    }

    // Centrar cámara si cambian las publicaciones
    LaunchedEffect(firstCoords) {
        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(firstCoords, 13.5f))
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MapGround),
    ) {
        if (showMap) {
            // Google Map interactivo con los punteros de los eventos y reportes
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = hasLocationPermission,
                    mapToolbarEnabled = true,
                    compassEnabled = true,
                ),
                properties = MapProperties(
                    isMyLocationEnabled = hasLocationPermission,
                ),
            ) {
                posts.forEachIndexed { index, post ->
                    val coords = post.toLatLng()
                    val markerHue = when (post.category) {
                        Category.PERDIDOS -> BitmapDescriptorFactory.HUE_RED
                        Category.ENCONTRADOS -> BitmapDescriptorFactory.HUE_AZURE
                        Category.VETERINARIA -> BitmapDescriptorFactory.HUE_GREEN
                        Category.TEMPORAL -> BitmapDescriptorFactory.HUE_VIOLET
                        Category.ADOPCION -> BitmapDescriptorFactory.HUE_ORANGE
                    }

                    val snippetInfo = post.eventDate ?: post.place ?: post.neighborhood

                    Marker(
                        state = MarkerState(position = coords),
                        title = post.title,
                        snippet = snippetInfo,
                        icon = BitmapDescriptorFactory.defaultMarker(markerHue),
                        onClick = {
                            coroutineScope.launch {
                                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(coords, 14.5f))
                                listState.animateScrollToItem(index)
                            }
                            false
                        },
                        onInfoWindowClick = {
                            onOpenPublication(post.id)
                        },
                    )
                }
            }
        }

        // Interfaz superior y controles
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
                        .background(HuellaColors.Surface)
                        .clickable(onClick = onBack)
                        .padding(10.dp),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = HuellaColors.TextPrimary)
                }
                Row(
                    Modifier
                        .weight(1f)
                        .shadow(3.dp, RoundedCornerShape(22.dp))
                        .clip(RoundedCornerShape(22.dp))
                        .background(HuellaColors.Surface)
                        .padding(3.dp),
                ) {
                    ToggleSegment(stringResource(R.string.map_list), R.drawable.icon_accion_lista, selected = !showMap, Modifier.weight(1f)) { showMap = false }
                    ToggleSegment(stringResource(R.string.map_map), R.drawable.icon_accion_mapa, selected = showMap, Modifier.weight(1f)) { showMap = true }
                }
            }

            // Chips de filtrado por tipo de evento o reporte
            LazyRow(
                Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    LegendChip(stringResource(R.string.category_lost), HuellaColors.CatPerdidos, showLost) { showLost = !showLost }
                }
                item {
                    LegendChip(stringResource(R.string.category_found), HuellaColors.CatEncontrados, showFound) { showFound = !showFound }
                }
                item {
                    LegendChip(stringResource(R.string.map_events), HuellaColors.CatVeterinaria, showVet) { showVet = !showVet }
                }
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
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(posts, key = { it.id }) { p ->
                            PublicationRowCard(
                                publication = p,
                                onClick = {
                                    val target = p.toLatLng()
                                    coroutineScope.launch {
                                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(target, 15f))
                                    }
                                    onOpenPublication(p.id)
                                },
                                modifier = Modifier.width(330.dp),
                            )
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
            .background(HuellaColors.Surface)
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

/** Category pin para vistas previas estáticas y detalle de publicación. */
@Composable
internal fun MapPin(category: Category, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 44.dp) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        HuellaIcon(categoryIcon(category, filled = true), Modifier.requiredSize(size + 6.dp), tint = Color.White, size = size + 6.dp)
        HuellaIcon(categoryIcon(category, filled = true), tint = category.color, size = size)
    }
}

/** Fondo estático de mapa usado por el detalle de publicación y previews. */
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

@Preview(showBackground = true)
@Composable
fun MapScreenPreview() {
    HuellaTheme {
        MapScreen(
            onBack = {},
            onOpenPublication = {},
        )
    }
}
