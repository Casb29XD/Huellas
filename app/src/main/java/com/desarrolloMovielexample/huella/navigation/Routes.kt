package com.desarrolloMovielexample.huella.navigation

/** Every screen of the design. String routes; use createRoute(...) for the ones with arguments. */
sealed class Routes(val route: String) {
    // 01 Acceso
    data object Splash : Routes("splash")
    data object Onboarding : Routes("onboarding")
    data object Login : Routes("login")
    data object Register : Routes("register")
    data object ForgotPassword : Routes("forgot_password")

    // 02 Inicio y detalle
    data object Home : Routes("home")
    /** "Buscar" tab: Lista/Mapa of perdidos y encontrados (design shows Buscar selected on these frames). */
    data object Map : Routes("map")
    data object Detail : Routes("detail/{id}") {
        const val ARG = "id"
        fun createRoute(id: String) = "detail/$id"
    }

    // 03 Publicar
    data object CreatePost : Routes("create_post")
    data object PostSent : Routes("post_sent")

    // 04 Solicitudes
    data object Requests : Routes("requests?tab={tab}") {
        const val ARG = "tab"
        /** tab 0 = Enviadas, 1 = Recibidas */
        fun createRoute(tab: Int = 0) = "requests?tab=$tab"
    }
    data object Chat : Routes("chat/{id}") {
        const val ARG = "id"
        fun createRoute(conversationId: String) = "chat/$conversationId"
    }

    // 05 Perfil
    data object Profile : Routes("profile")
    data object Levels : Routes("levels")
    data object PublicProfile : Routes("public_profile/{id}") {
        const val ARG = "id"
        fun createRoute(userId: String) = "public_profile/$userId"
    }
    data object Notifications : Routes("notifications")
    data object EditProfile : Routes("edit_profile")
    data object Settings : Routes("settings")

    // 06 Moderación
    data object Moderation : Routes("moderation")
    data object ModerationDetail : Routes("moderation_detail/{id}") {
        const val ARG = "id"
        fun createRoute(itemId: String) = "moderation_detail/$itemId"
    }
}
