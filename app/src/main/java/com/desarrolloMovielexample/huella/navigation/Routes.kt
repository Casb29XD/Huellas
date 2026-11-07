package com.desarrolloMovielexample.huella.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes using Kotlinx Serialization.
 * Replaces the legacy String routes and navArgument bundle parsing.
 */
sealed interface Routes {
    // 01 Acceso
    @Serializable
    data object Splash : Routes

    @Serializable
    data object Onboarding : Routes

    @Serializable
    data object Login : Routes

    @Serializable
    data object Register : Routes

    @Serializable
    data object ForgotPassword : Routes

    // 02 Inicio y detalle
    @Serializable
    data object Home : Routes

    @Serializable
    data object Map : Routes

    @Serializable
    data class Detail(val id: String) : Routes

    // 03 Publicar / Editar
    @Serializable
    data class CreatePost(val editPublicationId: String? = null) : Routes

    @Serializable
    data object PostSent : Routes

    // 04 Solicitudes
    @Serializable
    data class Requests(val tab: Int = 0) : Routes

    @Serializable
    data class Chat(val id: String) : Routes

    // 05 Perfil
    @Serializable
    data object Profile : Routes

    @Serializable
    data object Levels : Routes

    @Serializable
    data class PublicProfile(val id: String) : Routes

    @Serializable
    data object Notifications : Routes

    @Serializable
    data object EditProfile : Routes

    @Serializable
    data object Settings : Routes

    // 06 Moderación
    @Serializable
    data object Moderation : Routes

    @Serializable
    data class ModerationDetail(val id: String) : Routes
}
