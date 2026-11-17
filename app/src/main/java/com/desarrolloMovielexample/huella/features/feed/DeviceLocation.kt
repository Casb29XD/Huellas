package com.desarrolloMovielexample.huella.features.feed

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat

internal val LocationPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

internal fun hasLocationPermission(context: Context): Boolean =
    LocationPermissions.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

/** Última ubicación conocida del celular (la más reciente entre GPS, red y pasiva), o null. */
// ponytail: no pide una lectura nueva; si el celular no tiene ninguna reciente, el usuario abre el mapa o activa el GPS.
@SuppressLint("MissingPermission")
internal fun lastKnownLocation(context: Context): Pair<Double, Double>? {
    if (!hasLocationPermission(context)) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return manager.getProviders(true)
        .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time }
        ?.let { it.latitude to it.longitude }
}
