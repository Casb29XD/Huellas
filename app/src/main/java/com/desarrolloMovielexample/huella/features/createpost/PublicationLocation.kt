package com.desarrolloMovielexample.huella.features.createpost

import android.location.Geocoder
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.components.HuellaTextButton
import com.desarrolloMovielexample.huella.core.components.PrimaryButton
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Colombia = LatLng(4.5709, -74.2973)

/** Mapa a pantalla casi completa: el usuario toca para poner el marcador y confirma. */
@Composable
fun PublicationLocationPickerDialog(
    city: String,
    latitude: Double?,
    longitude: Double?,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double) -> Unit,
) {
    val context = LocalContext.current
    var picked by remember { mutableStateOf(if (latitude != null && longitude != null) LatLng(latitude, longitude) else null) }
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(picked ?: Colombia, if (picked != null) 15f else 5f)
    }
    // Sin marcador previo, centra el mapa en la ciudad elegida con el Geocoder del sistema.
    LaunchedEffect(city) {
        if (picked != null || city.isBlank()) return@LaunchedEffect
        val found = withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            runCatching { Geocoder(context).getFromLocationName("$city, Colombia", 1)?.firstOrNull() }.getOrNull()
        }
        if (found != null) camera.animate(CameraUpdateFactory.newLatLngZoom(LatLng(found.latitude, found.longitude), 13f))
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = HuellaColors.Surface,
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.create_map_pick_instruction),
                    style = MaterialTheme.typography.bodyMedium,
                    color = HuellaColors.TextPrimary,
                )
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    cameraPositionState = camera,
                    uiSettings = MapUiSettings(mapToolbarEnabled = false, zoomControlsEnabled = true),
                    onMapClick = { picked = it },
                ) {
                    picked?.let { Marker(state = rememberUpdatedMarkerState(position = it)) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    HuellaTextButton(stringResource(R.string.action_cancel), onDismiss)
                    PrimaryButton(
                        text = stringResource(R.string.create_map_confirm),
                        onClick = { picked?.let { onConfirm(it.latitude, it.longitude) } },
                        enabled = picked != null,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** Mapa pequeño (lite mode) con el punto guardado; tocarlo vuelve a abrir el selector. */
@Composable
fun PublicationLocationPreview(latitude: Double, longitude: Double, onEditLocation: () -> Unit) {
    val point = LatLng(latitude, longitude)
    val camera = rememberCameraPositionState(key = point.toString()) { position = CameraPosition.fromLatLngZoom(point, 15f) }
    Box(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onEditLocation),
    ) {
        GoogleMap(
            cameraPositionState = camera,
            googleMapOptionsFactory = { GoogleMapOptions().liteMode(true) },
            uiSettings = MapUiSettings(mapToolbarEnabled = false, zoomControlsEnabled = false),
            onMapClick = { onEditLocation() },
        ) {
            Marker(state = rememberUpdatedMarkerState(position = point))
        }
    }
}
