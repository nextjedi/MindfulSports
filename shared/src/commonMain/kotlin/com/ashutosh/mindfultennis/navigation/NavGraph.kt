package com.ashutosh.mindfultennis.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ashutosh.mindfultennis.ui.endsession.EndSessionScreen
import com.ashutosh.mindfultennis.ui.endsession.EndSessionViewModel
import com.ashutosh.mindfultennis.ui.home.HomeScreen
import com.ashutosh.mindfultennis.ui.home.HomeUiEvent
import com.ashutosh.mindfultennis.ui.home.HomeViewModel
import com.ashutosh.mindfultennis.ui.login.LoginScreen
import com.ashutosh.mindfultennis.ui.login.LoginViewModel
import com.ashutosh.mindfultennis.ui.paywall.PaywallScreen
import com.ashutosh.mindfultennis.ui.paywall.PaywallViewModel
import com.ashutosh.mindfultennis.ui.settings.SettingsScreen
import com.ashutosh.mindfultennis.ui.settings.SettingsViewModel
import com.ashutosh.mindfultennis.ui.subscription.SubscriptionManagementScreen
import com.ashutosh.mindfultennis.ui.subscription.SubscriptionManagementViewModel
import com.ashutosh.mindfultennis.ui.sessions.SessionDetailScreen
import com.ashutosh.mindfultennis.ui.sessions.SessionDetailViewModel
import com.ashutosh.mindfultennis.ui.sessions.SessionsListScreen
import com.ashutosh.mindfultennis.ui.sessions.SessionsListViewModel
import com.ashutosh.mindfultennis.ui.startsession.StartSessionScreen
import com.ashutosh.mindfultennis.ui.startsession.StartSessionViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun NavGraph(
    navController: NavHostController,
    isAuthenticated: Boolean,
    pendingCancelSessionId: String? = null,
    modifier: Modifier = Modifier,
) {
    val startDestination = if (isAuthenticated) Route.Home.route else Route.Login.route

    // Auth guard: redirect to login if unauthenticated, or to home if authenticated
    LaunchedEffect(isAuthenticated) {
        val currentRoute = navController.currentDestination?.route
        if (!isAuthenticated && currentRoute != Route.Login.route) {
            navController.navigate(Route.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        } else if (isAuthenticated && currentRoute == Route.Login.route) {
            navController.navigate(Route.Home.route) {
                popUpTo(Route.Login.route) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(Route.Login.route) {
            val viewModel: LoginViewModel = koinViewModel()
            LoginScreen(
                viewModel = viewModel,
                onSignedIn = {
                    navController.navigate(Route.Home.route) {
                        popUpTo(Route.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Route.Home.route) {
            val viewModel: HomeViewModel = koinViewModel()

            // Handle cancel session from notification action
            if (pendingCancelSessionId != null) {
                LaunchedEffect(pendingCancelSessionId) {
                    viewModel.onEvent(HomeUiEvent.CancelSessionClicked)
                }
            }

            HomeScreen(
                viewModel = viewModel,
                onStartSessionClicked = {
                    navController.navigate(Route.StartSession.route)
                },
                onEndSessionClicked = { sessionId ->
                    navController.navigate(Route.EndSession(sessionId).route)
                },
                onShowSessionsClicked = {
                    navController.navigate(Route.SessionsList.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Route.Settings.route)
                },
            )
        }

        composable(Route.Settings.route) {
            val viewModel: SettingsViewModel = koinViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLoggedOut = {
                    navController.navigate(Route.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToSubscription = {
                    navController.navigate(Route.SubscriptionManagement.route)
                },
            )
        }

        composable(Route.StartSession.route) {
            val viewModel: StartSessionViewModel = koinViewModel()
            StartSessionScreen(
                viewModel = viewModel,
                onSessionStarted = {
                    navController.popBackStack(Route.Home.route, inclusive = false)
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        composable(
            route = Route.EndSession.ROUTE_PATTERN,
            arguments = listOf(
                navArgument(Route.EndSession.ARG_SESSION_ID) {
                    type = NavType.StringType
                }
            ),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString(Route.EndSession.ARG_SESSION_ID) ?: return@composable
            val viewModel: EndSessionViewModel = koinViewModel { parametersOf(sessionId) }
            EndSessionScreen(
                viewModel = viewModel,
                onSessionSubmitted = {
                    navController.popBackStack(Route.Home.route, inclusive = false)
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        composable(Route.SessionsList.route) {
            val viewModel: SessionsListViewModel = koinViewModel()
            SessionsListScreen(
                viewModel = viewModel,
                onSessionClicked = { sessionId ->
                    navController.navigate(Route.SessionDetail(sessionId).route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        composable(Route.Paywall.route) {
            val viewModel: PaywallViewModel = koinViewModel()
            PaywallScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Route.SubscriptionManagement.route) {
            val viewModel: SubscriptionManagementViewModel = koinViewModel()
            SubscriptionManagementScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPaywall = {
                    navController.navigate(Route.Paywall.route)
                },
            )
        }

        composable(
            route = Route.SessionDetail.ROUTE_PATTERN,
            arguments = listOf(
                navArgument(Route.SessionDetail.ARG_SESSION_ID) {
                    type = NavType.StringType
                }
            ),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString(Route.SessionDetail.ARG_SESSION_ID) ?: return@composable
            val viewModel: SessionDetailViewModel = koinViewModel { parametersOf(sessionId) }
            SessionDetailScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }
    }
}
