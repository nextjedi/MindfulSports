package com.ashutosh.mindfultennis.ui.subscription

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ashutosh.mindfultennis.domain.model.SubscriptionStatus
import com.ashutosh.mindfultennis.domain.model.hasPremiumAccess
import com.ashutosh.mindfultennis.ui.theme.Spacing
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun SubscriptionManagementScreen(
    viewModel: SubscriptionManagementViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.restoreResult) {
        uiState.restoreResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(SubscriptionManagementUiEvent.RestoreResultDismissed)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(SubscriptionManagementUiEvent.ErrorDismissed)
        }
    }

    SubscriptionManagementContent(
        state = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onNavigateToPaywall = onNavigateToPaywall,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionManagementContent(
    state: SubscriptionManagementUiState,
    onEvent: (SubscriptionManagementUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Subscription") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.md),
        ) {
            Spacer(Modifier.height(Spacing.md))

            StatusSection(status = state.subscriptionStatus)

            Spacer(Modifier.height(Spacing.lg))

            if (!state.subscriptionStatus.hasPremiumAccess) {
                Button(
                    onClick = onNavigateToPaywall,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Upgrade to Premium")
                }

                Spacer(Modifier.height(Spacing.sm))
            }

            OutlinedButton(
                onClick = { onEvent(SubscriptionManagementUiEvent.RestoreClicked) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isRestoring,
            ) {
                if (state.isRestoring) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Restoring…")
                } else {
                    Text("Restore Purchases")
                }
            }
        }
    }
}

@Composable
private fun StatusSection(
    status: SubscriptionStatus,
    modifier: Modifier = Modifier,
) {
    val (icon, label, detail) = when (status) {
        is SubscriptionStatus.Active -> Triple(
            Icons.Default.CheckCircle,
            "Premium Active",
            null,
        )
        is SubscriptionStatus.Trial -> Triple(
            Icons.Default.CheckCircle,
            "Free Trial",
            "Trial ends ${formatTrialEnd(status)}",
        )
        is SubscriptionStatus.Cancelled -> Triple(
            Icons.Default.CheckCircle,
            "Premium (Cancelled)",
            "Access continues until the end of the billing period.",
        )
        is SubscriptionStatus.Expired,
        is SubscriptionStatus.None -> Triple(
            Icons.Default.Lock,
            "No Active Subscription",
            "Upgrade to unlock all premium features.",
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (status.hasPremiumAccess) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(Spacing.sm))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
            )
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatTrialEnd(status: SubscriptionStatus.Trial): String {
    return try {
        val local = status.endsAt.toLocalDateTime(TimeZone.currentSystemDefault())
        "${local.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${local.dayOfMonth}, ${local.year}"
    } catch (_: Exception) {
        "soon"
    }
}
