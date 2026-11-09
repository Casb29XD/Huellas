package com.desarrolloMovielexample.huella.core.utils

import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/** Texto para el usuario que un ViewModel emite sin Context; la pantalla lo resuelve con sus recursos. */
sealed interface UiText {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    /** Cantidad con plurales, p. ej. "hace 3 h" / "3 h ago". */
    data class Plural(@PluralsRes val id: Int, val count: Int) : UiText

    /** Texto ya redactado que no está en recursos (p. ej. el mensaje de error del servidor). */
    data class Raw(val value: String) : UiText

    fun resolve(resources: Resources): String = when (this) {
        is Res -> resources.getString(id, *args.toTypedArray())
        is Plural -> resources.getQuantityString(id, count, count)
        is Raw -> value
    }
}

/** Error propio de la app con su texto en recursos (ítem 37); se muestra en el idioma de la UI. */
class AppError(@StringRes val res: Int, vararg val args: Any) : IllegalStateException()

fun uiText(@StringRes id: Int, vararg args: Any): UiText = UiText.Res(id, args.toList())

/** AppError en el idioma de la UI; si no, el mensaje (del servidor) tal cual o el recurso [fallback]. */
fun Throwable.toUiText(@StringRes fallback: Int): UiText = when {
    this is AppError -> UiText.Res(res, args.toList())
    else -> message?.let { UiText.Raw(it) } ?: UiText.Res(fallback)
}

@Composable
fun UiText.asString(): String {
    LocalConfiguration.current // recompone al cambiar el idioma
    return resolve(LocalContext.current.resources)
}
