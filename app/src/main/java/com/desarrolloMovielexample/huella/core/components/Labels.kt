package com.desarrolloMovielexample.huella.core.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.domain.model.Badge
import com.desarrolloMovielexample.huella.domain.model.Category
import com.desarrolloMovielexample.huella.domain.model.Level
import com.desarrolloMovielexample.huella.domain.model.PostStatus
import com.desarrolloMovielexample.huella.domain.model.RequestStatus

// Textos visibles de los enums del dominio; el `label` en español del enum sigue siendo el valor de datos.

@get:StringRes
val Category.labelRes: Int
    get() = when (this) {
        Category.ADOPCION -> R.string.category_adoption
        Category.PERDIDOS -> R.string.category_lost
        Category.ENCONTRADOS -> R.string.category_found
        Category.TEMPORAL -> R.string.category_foster
        Category.VETERINARIA -> R.string.category_vet
    }

@get:StringRes
val Level.labelRes: Int
    get() = when (this) {
        Level.AMIGO_ANIMAL -> R.string.level_animal_friend
        Level.PROTECTOR -> R.string.level_protector
        Level.GUARDIAN -> R.string.level_guardian
        Level.HEROE -> R.string.level_hero
    }

@get:StringRes
val Level.rangeRes: Int
    get() = when (this) {
        Level.AMIGO_ANIMAL -> R.string.level_range_animal_friend
        Level.PROTECTOR -> R.string.level_range_protector
        Level.GUARDIAN -> R.string.level_range_guardian
        Level.HEROE -> R.string.level_range_hero
    }

val Level.benefitsRes: List<Int>
    get() = when (this) {
        Level.AMIGO_ANIMAL -> listOf(R.string.level_benefit_post_request, R.string.level_benefit_favorites)
        Level.PROTECTOR -> listOf(R.string.level_benefit_badge, R.string.level_benefit_5_posts)
        Level.GUARDIAN -> listOf(R.string.level_benefit_featured, R.string.level_benefit_priority)
        Level.HEROE -> listOf(R.string.level_benefit_unlimited, R.string.level_benefit_events, R.string.level_benefit_recognition)
    }

@get:StringRes
val PostStatus.labelRes: Int
    get() = when (this) {
        PostStatus.EN_REVISION -> R.string.post_status_review
        PostStatus.APROBADA -> R.string.post_status_approved
        PostStatus.RECHAZADA -> R.string.post_status_rejected
    }

@get:StringRes
val RequestStatus.labelRes: Int
    get() = when (this) {
        RequestStatus.PENDIENTE -> R.string.request_status_pending
        RequestStatus.ACEPTADA -> R.string.request_status_accepted
        RequestStatus.RECHAZADA -> R.string.request_status_rejected
    }

@get:StringRes
val Badge.labelRes: Int
    get() = when (this) {
        Badge.PRIMER_POST -> R.string.badge_first_post
        Badge.ADOPCION_EXITOSA -> R.string.badge_adoption
        Badge.HOGAR_TEMPORAL -> R.string.badge_foster
        Badge.COMUNIDAD_ACTIVA -> R.string.badge_community
        Badge.MODERADOR -> R.string.badge_moderator
    }

/** Valores fijos en español que llegan de la BD o del repositorio (especie, tamaño, sexo, motivos, grupos). Lo desconocido se muestra tal cual. */
@Composable
fun optionLabel(value: String): String {
    val res = when (value) {
        "Perro" -> R.string.option_dog
        "Gato" -> R.string.option_cat
        "Ave" -> R.string.option_bird
        "Conejo" -> R.string.option_rabbit
        "Roedor" -> R.string.option_rodent
        "Otro" -> R.string.option_other
        "Perros y gatos" -> R.string.option_dogs_and_cats
        "Pequeño" -> R.string.option_small
        "Mediano" -> R.string.option_medium
        "Grande" -> R.string.option_large
        "Todos" -> R.string.option_all
        "Macho" -> R.string.option_male
        "Hembra" -> R.string.option_female
        "Desconocido" -> R.string.option_unknown
        "Venta de animales" -> R.string.reason_animal_sale
        "Maltrato" -> R.string.reason_abuse
        "Información falsa" -> R.string.reason_false_info
        "Spam" -> R.string.reason_spam
        "Contenido falso" -> R.string.reason_false_content
        "Fotos inadecuadas" -> R.string.reason_bad_photos
        "Duplicado" -> R.string.reason_duplicate
        "Castración" -> R.string.service_neutering
        "Desparasitación" -> R.string.service_deworming
        "Vacunación" -> R.string.service_vaccination
        "Hoy" -> R.string.group_today
        "Ayer" -> R.string.group_yesterday
        "Esta semana" -> R.string.group_this_week
        "Antes" -> R.string.group_earlier
        else -> null
    }
    return if (res != null) stringResource(res) else value
}
