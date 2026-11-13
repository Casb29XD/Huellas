package com.desarrolloMovielexample.huella.navigation

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.desarrolloMovielexample.huella.core.components.BottomBarItem
import com.desarrolloMovielexample.huella.core.components.HuellaBottomBar
import com.desarrolloMovielexample.huella.core.components.HuellaSnackbarHost
import com.desarrolloMovielexample.huella.core.utils.UiText
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.data.remote.SupabaseRepository
import com.desarrolloMovielexample.huella.data.remote.SupabaseSession
import com.desarrolloMovielexample.huella.features.auth.ForgotPasswordScreen
import com.desarrolloMovielexample.huella.features.auth.LoginScreen
import com.desarrolloMovielexample.huella.features.auth.OnboardingScreen
import com.desarrolloMovielexample.huella.features.auth.RegisterScreen
import com.desarrolloMovielexample.huella.features.auth.SetPasswordScreen
import com.desarrolloMovielexample.huella.features.auth.SplashScreen
import com.desarrolloMovielexample.huella.features.createpost.CreatePostScreen
import com.desarrolloMovielexample.huella.features.createpost.PostSentScreen
import com.desarrolloMovielexample.huella.features.feed.HomeScreen
import com.desarrolloMovielexample.huella.features.feed.MapScreen
import com.desarrolloMovielexample.huella.features.feed.PublicationDetailScreen
import com.desarrolloMovielexample.huella.features.legal.TermsScreen
import com.desarrolloMovielexample.huella.features.moderation.ModerationDetailScreen
import com.desarrolloMovielexample.huella.features.moderation.ModerationScreen
import com.desarrolloMovielexample.huella.features.messages.MessagesScreen
import com.desarrolloMovielexample.huella.features.profile.EditProfileScreen
import com.desarrolloMovielexample.huella.features.profile.LevelsScreen
import com.desarrolloMovielexample.huella.features.profile.NotificationsScreen
import com.desarrolloMovielexample.huella.features.profile.ProfileScreen
import com.desarrolloMovielexample.huella.features.profile.PublicProfileScreen
import com.desarrolloMovielexample.huella.features.profile.SettingsScreen
import com.desarrolloMovielexample.huella.features.requests.ChatScreen
import com.desarrolloMovielexample.huella.features.requests.RequestsScreen
import kotlinx.coroutines.launch

/** Bottom-bar tab for each top-level route (bar hidden elsewhere). */
private fun tabForDestination(destination: androidx.navigation.NavDestination?): BottomBarItem? {
    if (destination == null) return null
    return when {
        destination.hierarchy.any { it.hasRoute<Routes.Home>() } -> BottomBarItem.INICIO
        destination.hierarchy.any { it.hasRoute<Routes.Map>() } -> BottomBarItem.BUSCAR
        destination.hierarchy.any { it.hasRoute<Routes.Requests>() || it.hasRoute<Routes.Messages>() } -> BottomBarItem.SOLICITUDES
        destination.hierarchy.any { it.hasRoute<Routes.Profile>() } -> BottomBarItem.PERFIL
        else -> null
    }
}

/** Switch between top-level tabs keeping one copy of each in the back stack. */
private fun NavHostController.navigateToTab(route: Any) {
    navigate(route) {
        popUpTo<Routes.Home> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Clear the whole back stack and go to [route] (login -> home, logout -> login). */
private fun NavHostController.navigateClearingStack(route: Any) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

private const val APP_PREFS = "huella_prefs"
private const val ONBOARDING_SEEN = "onboarding_seen"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    deepLinkUri: Uri? = null,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = tabForDestination(backStackEntry?.destination)
    val scope = rememberCoroutineScope()
    val prefs = LocalContext.current.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
    // Confirmaciones que sobreviven a la navegación: la pantalla sale al instante y el aviso se ve en el destino (ítem 56).
    val appSnackbar = remember { SnackbarHostState() }
    val resources = LocalContext.current.resources
    val flash: (UiText) -> Unit = { msg -> scope.launch { appSnackbar.showSnackbar(msg.resolve(resources)) } }

    // Ítem 26: si el refresh token caduca, volver a Login en vez de quedar en "Inicia sesión para continuar".
    LaunchedEffect(Unit) {
        SupabaseRepository.sessionExpired.collect { navController.navigateClearingStack(Routes.Login) }
    }

    // Manejar deep links de Supabase Auth al iniciar la app.
    LaunchedEffect(deepLinkUri) {
        if (deepLinkUri == null) return@LaunchedEffect
        val fragmentType = deepLinkUri.fragment?.split("&")
            ?.firstOrNull { it.startsWith("type=") }
            ?.substringAfter("=")
        val type = deepLinkUri.getQueryParameter("type") ?: fragmentType

        // El callback dedicado evita depender de type=recovery o del estado en memoria del proceso.
        val isPasswordRecovery = deepLinkUri.path.equals("/recovery", ignoreCase = true) ||
            type.equals("recovery", ignoreCase = true) ||
            SupabaseSession.passwordRecoveryPending

        // Recuperación: canjear primero el code PKCE y abrir el formulario solo con una sesión válida.
        val authResult = SupabaseRepository.handleAuthCallback(deepLinkUri)
        val target = when {
            authResult.isFailure -> Routes.Login
            isPasswordRecovery -> Routes.SetPassword
            else -> Routes.Home
        }
        navController.navigateClearingStack(target)
    }

    Scaffold(
        containerColor = HuellaColors.Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { HuellaSnackbarHost(appSnackbar) },
        bottomBar = {
            if (currentTab != null) {
                HuellaBottomBar(
                    selected = currentTab,
                    onItemClick = { item ->
                        when (item) {
                            BottomBarItem.INICIO -> navController.navigateToTab(Routes.Home)
                            BottomBarItem.BUSCAR -> navController.navigateToTab(Routes.Map())
                            BottomBarItem.PUBLICAR -> navController.navigate(Routes.CreatePost())
                            BottomBarItem.SOLICITUDES -> navController.navigateToTab(Routes.Requests(0))
                            BottomBarItem.PERFIL -> navController.navigateToTab(Routes.Profile)
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Splash,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            // ---------- 01 Acceso ----------
            composable<Routes.Splash> {
                SplashScreen(
                    onFinished = {
                        // Con deep link de Auth, el LaunchedEffect de arriba decide el destino.
                        if (deepLinkUri == null) scope.launch {
                            // Ítem 25: sesión válida -> Home; si no, onboarding solo la primera vez y luego Login.
                            val target = when {
                                SupabaseRepository.restoreSession() -> Routes.Home
                                prefs.getBoolean(ONBOARDING_SEEN, false) -> Routes.Login
                                else -> Routes.Onboarding
                            }
                            // Pila limpia: si el refresh caducó, sessionExpired pudo haber navegado ya a Login.
                            navController.navigateClearingStack(target)
                        }
                    },
                )
            }
            composable<Routes.Onboarding> {
                OnboardingScreen(
                    onFinish = {
                        prefs.edit().putBoolean(ONBOARDING_SEEN, true).apply()
                        navController.navigate(Routes.Login) {
                            popUpTo<Routes.Onboarding> { inclusive = true }
                        }
                    },
                )
            }
            composable<Routes.Login> {
                LoginScreen(
                    onLoginSuccess = { navController.navigateClearingStack(Routes.Home) },
                    onRegister = { navController.navigate(Routes.Register) },
                    onForgotPassword = { navController.navigate(Routes.ForgotPassword) },
                )
            }
            composable<Routes.Register> {
                RegisterScreen(
                    onBack = { navController.popBackStack() },
                    onRegistered = { navController.navigateClearingStack(Routes.Home) },
                    onOpenTerms = { navController.navigate(Routes.Terms) },
                )
            }
            composable<Routes.ForgotPassword> {
                ForgotPasswordScreen(onBack = { navController.popBackStack() })
            }
            composable<Routes.SetPassword> {
                // Desde el enlace de recuperación la pila está vacía: al terminar se entra a Home.
                val leave = { if (!navController.popBackStack()) navController.navigateClearingStack(Routes.Home) }
                SetPasswordScreen(onBack = leave, onDone = { msg -> leave(); flash(msg) })
            }

            // ---------- 02 Inicio y detalle ----------
            composable<Routes.Home> {
                HomeScreen(
                    onOpenPublication = { id -> navController.navigate(Routes.Detail(id)) },
                    onOpenMap = { navController.navigateToTab(Routes.Map()) },
                    onOpenNotifications = { navController.navigate(Routes.Notifications) },
                    onOpenProfile = { navController.navigateToTab(Routes.Profile) },
                    onCreatePost = { navController.navigate(Routes.CreatePost()) },
                )
            }
            composable<Routes.Map> { entry ->
                val route = entry.toRoute<Routes.Map>()
                MapScreen(
                    publicationId = route.publicationId,
                    onBack = { navController.popBackStack() },
                    onOpenPublication = { id -> navController.navigate(Routes.Detail(id)) },
                )
            }
            composable<Routes.Detail> { entry ->
                val detailRoute = entry.toRoute<Routes.Detail>()
                PublicationDetailScreen(
                    publicationId = detailRoute.id,
                    onBack = { navController.popBackStack() },
                    onOpenAuthor = { userId -> navController.navigate(Routes.PublicProfile(userId)) },
                    onOpenMap = { id -> navController.navigate(Routes.Map(publicationId = id)) },
                    onEditPublication = { id -> navController.navigate(Routes.CreatePost(id)) }
                )
            }

            // ---------- 03 Publicar / Editar ----------
            composable<Routes.CreatePost> { entry ->
                val route = entry.toRoute<Routes.CreatePost>()
                CreatePostScreen(
                    editPublicationId = route.editPublicationId,
                    onBack = { navController.popBackStack() },
                    onPublished = {
                        navController.navigate(Routes.PostSent) {
                            popUpTo<Routes.CreatePost> { inclusive = true }
                        }
                    },
                )
            }
            composable<Routes.PostSent> {
                PostSentScreen(
                    onViewMyPublications = {
                        navController.popBackStack()
                        navController.navigateToTab(Routes.Profile)
                    },
                    onGoHome = { navController.popBackStack<Routes.Home>(inclusive = false) },
                )
            }

            // ---------- 04 Solicitudes ----------
            composable<Routes.Requests> { entry ->
                val route = entry.toRoute<Routes.Requests>()
                RequestsScreen(
                    initialTab = route.tab,
                    onOpenPublication = { id -> navController.navigate(Routes.Detail(id)) },
                    onOpenChat = { id -> navController.navigate(Routes.Chat(id)) },
                    onExplore = { navController.navigateToTab(Routes.Home) },
                    onOpenMessages = { navController.navigate(Routes.Messages) },
                )
            }
            composable<Routes.Messages> {
                MessagesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenChat = { id -> navController.navigate(Routes.Chat(id)) },
                )
            }
            composable<Routes.Chat> { entry ->
                val route = entry.toRoute<Routes.Chat>()
                ChatScreen(
                    conversationId = route.id,
                    onBack = { navController.popBackStack() },
                )
            }

            // ---------- 05 Perfil ----------
            composable<Routes.Profile> {
                ProfileScreen(
                    onEditProfile = { navController.navigate(Routes.EditProfile) },
                    onSettings = { navController.navigate(Routes.Settings) },
                    onOpenLevels = { navController.navigate(Routes.Levels) },
                    onOpenPublication = { id -> navController.navigate(Routes.Detail(id)) },
                    onOpenModeration = { navController.navigate(Routes.Moderation) },
                )
            }
            composable<Routes.Levels> {
                LevelsScreen(onBack = { navController.popBackStack() })
            }
            composable<Routes.PublicProfile> { entry ->
                val route = entry.toRoute<Routes.PublicProfile>()
                PublicProfileScreen(
                    userId = route.id,
                    onBack = { navController.popBackStack() },
                    onOpenPublication = { id -> navController.navigate(Routes.Detail(id)) },
                    onContact = { conversationId -> navController.navigate(Routes.Chat(conversationId)) },
                )
            }
            composable<Routes.Notifications> {
                NotificationsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenRequests = { tab -> navController.navigateToTab(Routes.Requests(tab)) },
                    onOpenChat = { id -> navController.navigate(Routes.Chat(id)) },
                    onOpenPublication = { id -> navController.navigate(Routes.Detail(id)) },
                    onOpenLevels = { navController.navigate(Routes.Levels) },
                )
            }
            composable<Routes.EditProfile> {
                EditProfileScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable<Routes.Settings> {
                val user by SupabaseRepository.currentUser.collectAsStateWithLifecycle()
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onEditProfile = { navController.navigate(Routes.EditProfile) },
                    onChangePassword = {
                        val signedIn = SupabaseRepository.currentUser.value != null
                        navController.navigate(if (signedIn) Routes.SetPassword else Routes.ForgotPassword)
                    },
                    onLoggedOut = { navController.navigateClearingStack(Routes.Login) },
                    user = user,
                    onOpenTerms = { navController.navigate(Routes.Terms) },
                )
            }

            // ---------- 06 Moderación ----------
            composable<Routes.Moderation> {
                ModerationScreen(
                    onBack = { navController.popBackStack() },
                    onOpenItem = { id -> navController.navigate(Routes.ModerationDetail(id)) },
                )
            }
            composable<Routes.ModerationDetail> { entry ->
                val route = entry.toRoute<Routes.ModerationDetail>()
                ModerationDetailScreen(
                    itemId = route.id,
                    onBack = { navController.popBackStack() },
                    onDone = { msg ->
                        navController.popBackStack()
                        flash(msg)
                    },
                )
            }

            composable<Routes.Terms> {
                TermsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
