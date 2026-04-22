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
import com.ashutosh.mindfultennis.ui.settings.SettingsScreen
import com.ashutosh.mindfultennis.ui.settings.SettingsViewModel
import com.ashutosh.mindfultennis.ui.sessions.SessionDetailScreen
import com.ashutosh.mindfultennis.ui.sessions.SessionDetailViewModel
import com.ashutosh.mindfultennis.ui.sessions.SessionsListScreen
import com.ashutosh.mindfultennis.ui.sessions.SessionsListViewModel
import com.ashutosh.mindfultennis.ui.sportselection.SportSelectionScreen
import com.ashutosh.mindfultennis.ui.sportselection.SportSelectionViewModel
import com.ashutosh.mindfultennis.ui.startsession.StartSessionScreen
import com.ashutosh.mindfultennis.ui.startsession.StartSessionViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun NavGraph(
    navController: NavHostController,
    isAuthenticated: Boolean,
    hasSportSelected: Boolean,
    pendingCancelSessionId: String? = null,
    modifier: Modifier = Modifier,
) {
    val startDestination = when {
        !isAuthenticated  -> Route.Login.route
        !hasSportSelected -> Route.SportSelection.route
        else              -> Route.Home.route
    }

    // Navigation guard — reacts to auth and sport-selection state changes
    LaunchedEffect(isAuthenticated, hasSportSelected) {
        val currentRoute = navController.currentDestination?.route
        when {
            !isAuthenticated -> {
                if (currentRoute != Route.Login.route) {
                    navController.navigate(Route.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            !hasSportSelected -> {
                if (currentRoute != Route.SportSelection.route) {
                    navController.navigate(Route.SportSelection.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            hasSportSelected && currentRoute == Route.SportSelection.route -> {
                navController.navigate(Route.Home.route) {
                    popUpTo(Route.SportSelection.route) { inclusive = true }
                }
            }
            hasSportSelected && currentRoute == Route.Login.route -> {
                navController.navigate(Route.Home.route) {
                    popUpTo(Route.Login.route) { inclusive = true }
                }
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
                    // NavGraph guard above will navigate to SportSelection or Home
                    // based on hasSportSelected — no manual navigate needed here.
                },
            )
        }

        composable(Route.SportSelection.route) {
            val viewModel: SportSelectionViewModel = koinViewModel()
            SportSelectionScreen(
                viewModel = viewModel,
                onSportSelected = {
                    // UserPreferences.selectedSportId write triggers hasSportSelected = true
                    // in App.kt, which causes the LaunchedEffect guard above to navigate to Home.
                },
            )
        }

        composable(Route.Home.route) {
            val viewModel: HomeViewModel = koinViewModel()

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
                onChangeSport = {
                    navController.navigate(Route.SportSelection.route)
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
            val sessionId = backStackEntry.arguments?.getString(Route.EndSession.ARG_SESSION_ID)
                ?: return@composable
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

        composable(
            route = Route.SessionDetail.ROUTE_PATTERN,
            arguments = listOf(
                navArgument(Route.SessionDetail.ARG_SESSION_ID) {
                    type = NavType.StringType
                }
            ),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString(Route.SessionDetail.ARG_SESSION_ID)
                ?: return@composable
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
