package com.desarrolloMovielexample.huella.navigation

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.desarrolloMovielexample.huella.core.components.BottomBarItem
import com.desarrolloMovielexample.huella.core.components.HuellaBottomBar
import com.desarrolloMovielexample.huella.core.theme.HuellaColors
import com.desarrolloMovielexample.huella.features.auth.ForgotPasswordScreen
import com.desarrolloMovielexample.huella.features.auth.LoginScreen
import com.desarrolloMovielexample.huella.features.auth.OnboardingScreen
import com.desarrolloMovielexample.huella.features.auth.RegisterScreen
import com.desarrolloMovielexample.huella.features.auth.SplashScreen
import com.desarrolloMovielexample.huella.features.createpost.CreatePostScreen
import com.desarrolloMovielexample.huella.features.createpost.PostSentScreen
import com.desarrolloMovielexample.huella.features.feed.HomeScreen
import com.desarrolloMovielexample.huella.features.feed.MapScreen
import com.desarrolloMovielexample.huella.features.feed.PublicationDetailScreen
import com.desarrolloMovielexample.huella.features.moderation.ModerationDetailScreen
import com.desarrolloMovielexample.huella.features.moderation.ModerationScreen
import com.desarrolloMovielexample.huella.features.profile.EditProfileScreen
import com.desarrolloMovielexample.huella.features.profile.LevelsScreen
import com.desarrolloMovielexample.huella.features.profile.NotificationsScreen
import com.desarrolloMovielexample.huella.features.profile.ProfileScreen
import com.desarrolloMovielexample.huella.features.profile.PublicProfileScreen
import com.desarrolloMovielexample.huella.features.profile.SettingsScreen
import com.desarrolloMovielexample.huella.features.requests.ChatScreen
import com.desarrolloMovielexample.huella.features.requests.RequestsScreen

/** Bottom-bar tab for each top-level route pattern (bar hidden elsewhere). */
private fun tabFor(route: String?): BottomBarItem? = when (route) {
    Routes.Home.route -> BottomBarItem.INICIO
    Routes.Map.route -> BottomBarItem.BUSCAR
    Routes.Requests.route -> BottomBarItem.SOLICITUDES
    Routes.Profile.route -> BottomBarItem.PERFIL
    else -> null
}

/** Switch between top-level tabs keeping one copy of each in the back stack. */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(Routes.Home.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Clear the whole back stack and go to [route] (login -> home, logout -> login). */
private fun NavHostController.navigateClearingStack(route: String) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = tabFor(backStackEntry?.destination?.route)

    Scaffold(
        containerColor = HuellaColors.Background,
        // Screens handle their own status-bar insets through their own Scaffold.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentTab != null) {
                HuellaBottomBar(
                    selected = currentTab,
                    onItemClick = { item ->
                        when (item) {
                            BottomBarItem.INICIO -> navController.navigateToTab(Routes.Home.route)
                            BottomBarItem.BUSCAR -> navController.navigateToTab(Routes.Map.route)
                            BottomBarItem.PUBLICAR -> navController.navigate(Routes.CreatePost.route)
                            BottomBarItem.SOLICITUDES -> navController.navigateToTab(Routes.Requests.createRoute(0))
                            BottomBarItem.PERFIL -> navController.navigateToTab(Routes.Profile.route)
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Splash.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            // ---------- 01 Acceso ----------
            composable(Routes.Splash.route) {
                SplashScreen(
                    onFinished = {
                        navController.navigate(Routes.Onboarding.route) {
                            popUpTo(Routes.Splash.route) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.Onboarding.route) {
                OnboardingScreen(
                    onFinish = {
                        navController.navigate(Routes.Login.route) {
                            popUpTo(Routes.Onboarding.route) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.Login.route) {
                LoginScreen(
                    onLoginSuccess = { navController.navigateClearingStack(Routes.Home.route) },
                    onRegister = { navController.navigate(Routes.Register.route) },
                    onForgotPassword = { navController.navigate(Routes.ForgotPassword.route) },
                )
            }
            composable(Routes.Register.route) {
                RegisterScreen(
                    onBack = { navController.popBackStack() },
                    onRegistered = { navController.navigateClearingStack(Routes.Home.route) },
                )
            }
            composable(Routes.ForgotPassword.route) {
                ForgotPasswordScreen(onBack = { navController.popBackStack() })
            }

            // ---------- 02 Inicio y detalle ----------
            composable(Routes.Home.route) {
                HomeScreen(
                    onOpenPublication = { id -> navController.navigate(Routes.Detail.createRoute(id)) },
                    onOpenMap = { navController.navigateToTab(Routes.Map.route) },
                    onOpenNotifications = { navController.navigate(Routes.Notifications.route) },
                    onOpenProfile = { navController.navigateToTab(Routes.Profile.route) },
                    onCreatePost = { navController.navigate(Routes.CreatePost.route) },
                )
            }
            composable(Routes.Map.route) {
                MapScreen(
                    onBack = { navController.navigateToTab(Routes.Home.route) },
                    onOpenPublication = { id -> navController.navigate(Routes.Detail.createRoute(id)) },
                )
            }
            composable(
                Routes.Detail.route,
                arguments = listOf(navArgument(Routes.Detail.ARG) { type = NavType.StringType }),
            ) { entry ->
                PublicationDetailScreen(
                    publicationId = entry.arguments?.getString(Routes.Detail.ARG).orEmpty(),
                    onBack = { navController.popBackStack() },
                    onOpenAuthor = { userId -> navController.navigate(Routes.PublicProfile.createRoute(userId)) },
                    onOpenMap = { navController.navigateToTab(Routes.Map.route) },
                )
            }

            // ---------- 03 Publicar ----------
            composable(Routes.CreatePost.route) {
                CreatePostScreen(
                    onBack = { navController.popBackStack() },
                    onPublished = {
                        navController.navigate(Routes.PostSent.route) {
                            popUpTo(Routes.CreatePost.route) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.PostSent.route) {
                PostSentScreen(
                    // Pop PostSent first so navigateToTab doesn't save it into Home's tab state
                    // (restoreState would bring it back on the next "Inicio").
                    onViewMyPublications = {
                        navController.popBackStack()
                        navController.navigateToTab(Routes.Profile.route)
                    },
                    onGoHome = { navController.popBackStack(Routes.Home.route, inclusive = false) },
                )
            }

            // ---------- 04 Solicitudes ----------
            composable(
                Routes.Requests.route,
                arguments = listOf(navArgument(Routes.Requests.ARG) { type = NavType.IntType; defaultValue = 0 }),
            ) { entry ->
                RequestsScreen(
                    initialTab = entry.arguments?.getInt(Routes.Requests.ARG) ?: 0,
                    onOpenPublication = { id -> navController.navigate(Routes.Detail.createRoute(id)) },
                    onOpenChat = { id -> navController.navigate(Routes.Chat.createRoute(id)) },
                    onExplore = { navController.navigateToTab(Routes.Home.route) },
                )
            }
            composable(
                Routes.Chat.route,
                arguments = listOf(navArgument(Routes.Chat.ARG) { type = NavType.StringType }),
            ) { entry ->
                ChatScreen(
                    conversationId = entry.arguments?.getString(Routes.Chat.ARG).orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }

            // ---------- 05 Perfil ----------
            composable(Routes.Profile.route) {
                ProfileScreen(
                    onEditProfile = { navController.navigate(Routes.EditProfile.route) },
                    onSettings = { navController.navigate(Routes.Settings.route) },
                    onOpenLevels = { navController.navigate(Routes.Levels.route) },
                    onOpenPublication = { id -> navController.navigate(Routes.Detail.createRoute(id)) },
                    onOpenModeration = { navController.navigate(Routes.Moderation.route) },
                )
            }
            composable(Routes.Levels.route) {
                LevelsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                Routes.PublicProfile.route,
                arguments = listOf(navArgument(Routes.PublicProfile.ARG) { type = NavType.StringType }),
            ) { entry ->
                PublicProfileScreen(
                    userId = entry.arguments?.getString(Routes.PublicProfile.ARG).orEmpty(),
                    onBack = { navController.popBackStack() },
                    onOpenPublication = { id -> navController.navigate(Routes.Detail.createRoute(id)) },
                    onContact = { conversationId -> navController.navigate(Routes.Chat.createRoute(conversationId)) },
                )
            }
            composable(Routes.Notifications.route) {
                NotificationsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenRequests = { tab -> navController.navigateToTab(Routes.Requests.createRoute(tab)) },
                    onOpenChat = { id -> navController.navigate(Routes.Chat.createRoute(id)) },
                    onOpenPublication = { id -> navController.navigate(Routes.Detail.createRoute(id)) },
                    onOpenLevels = { navController.navigate(Routes.Levels.route) },
                )
            }
            composable(Routes.EditProfile.route) {
                EditProfileScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable(Routes.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onEditProfile = { navController.navigate(Routes.EditProfile.route) },
                    onChangePassword = { navController.navigate(Routes.ForgotPassword.route) },
                    onLoggedOut = { navController.navigateClearingStack(Routes.Login.route) },
                )
            }

            // ---------- 06 Moderación ----------
            composable(Routes.Moderation.route) {
                ModerationScreen(
                    onBack = { navController.popBackStack() },
                    onOpenItem = { id -> navController.navigate(Routes.ModerationDetail.createRoute(id)) },
                )
            }
            composable(
                Routes.ModerationDetail.route,
                arguments = listOf(navArgument(Routes.ModerationDetail.ARG) { type = NavType.StringType }),
            ) { entry ->
                ModerationDetailScreen(
                    itemId = entry.arguments?.getString(Routes.ModerationDetail.ARG).orEmpty(),
                    onBack = { navController.popBackStack() },
                    onDone = { navController.popBackStack() },
                )
            }
        }
    }
}
