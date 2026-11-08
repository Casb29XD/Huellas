package com.desarrolloMovielexample.huella.core.utils

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/** Texto para el usuario que un ViewModel emite sin Context; la pantalla lo resuelve con sus recursos. */
sealed interface UiText {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    /** Texto ya redactado que no está en recursos (p. ej. el mensaje de error del servidor). */
    data class Raw(val value: String) : UiText

    fun resolve(resources: Resources): String = when (this) {
        is Res -> resources.getString(id, *args.toTypedArray())
        is Raw -> value
    }
}

fun uiText(@StringRes id: Int, vararg args: Any): UiText = UiText.Res(id, args.toList())

/** Mensaje del error si existe; si no, el recurso [fallback]. */
fun Throwable.toUiText(@StringRes fallback: Int): UiText = message?.let { UiText.Raw(it) } ?: UiText.Res(fallback)

@Composable
fun UiText.asString(): String {
    LocalConfiguration.current // recompone al cambiar el idioma
    return resolve(LocalContext.current.resources)
}
