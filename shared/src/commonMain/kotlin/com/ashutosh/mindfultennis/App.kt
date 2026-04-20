package com.ashutosh.mindfultennis

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.navigation.compose.rememberNavController
import com.ashutosh.mindfultennis.data.repository.AuthRepository
import com.ashutosh.mindfultennis.data.repository.AuthState
import com.ashutosh.mindfultennis.data.repository.SubscriptionRepository
import com.ashutosh.mindfultennis.domain.model.SubscriptionStatus
import com.ashutosh.mindfultennis.domain.model.hasPremiumAccess
import com.ashutosh.mindfultennis.navigation.NavGraph
import com.ashutosh.mindfultennis.ui.theme.MindfulTennisTheme
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.timeout
import kotlin.time.Duration.Companion.seconds
import org.koin.compose.koinInject

@Composable
fun App() {
    MindfulTennisTheme {
        val navController = rememberNavController()
        val authRepository = koinInject<AuthRepository>()
        val subscriptionRepository = koinInject<SubscriptionRepository>()
        val snackbarHostState = remember { SnackbarHostState() }

        val authState by authRepository.authState
            .timeout(10.seconds)
            .catch { emit(AuthState.Unauthenticated) }
            .collectAsState(initial = AuthState.Loading)

        val subscriptionStatus by subscriptionRepository.subscriptionStatus
            .collectAsState()

        LaunchedEffect(authState) {
            if (authState is AuthState.SessionExpired) {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    message = "Your session expired. Please sign in again.",
                    duration = SnackbarDuration.Long,
                )
            }
        }

        val isAuthenticated = authState is AuthState.Authenticated
        val hasPremiumAccess = subscriptionStatus.hasPremiumAccess
        val isSubscriptionLoading = subscriptionStatus is SubscriptionStatus.Loading

        Box(modifier = Modifier.fillMaxSize()) {
            Surface(modifier = Modifier.fillMaxSize()) {
                NavGraph(
                    navController = navController,
                    isAuthenticated = isAuthenticated,
                    hasPremiumAccess = hasPremiumAccess,
                    isSubscriptionLoading = isSubscriptionLoading,
                    pendingCancelSessionId = null,
                )
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
