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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
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
    val containerColor = when (status) {
        is SubscriptionStatus.Active,
        is SubscriptionStatus.Trial,
        is SubscriptionStatus.Cancelled -> MaterialTheme.colorScheme.primaryContainer
        is SubscriptionStatus.GracePeriod -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val onContainerColor = when (status) {
        is SubscriptionStatus.GracePeriod -> MaterialTheme.colorScheme.onErrorContainer
        is SubscriptionStatus.Active,
        is SubscriptionStatus.Trial,
        is SubscriptionStatus.Cancelled -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon = when (status) {
                is SubscriptionStatus.GracePeriod -> Icons.Default.Warning
                is SubscriptionStatus.Active,
                is SubscriptionStatus.Trial,
                is SubscriptionStatus.Cancelled -> Icons.Default.CheckCircle
                else -> Icons.Default.Lock
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = onContainerColor,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(Spacing.md))
            Column {
                Text(
                    text = statusTitle(status),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = onContainerColor,
                )
                val detail = statusDetail(status)
                if (detail != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = onContainerColor.copy(alpha = 0.75f),
                    )
                }
            }
        }
    }
}

private fun statusTitle(status: SubscriptionStatus): String = when (status) {
    is SubscriptionStatus.Active -> when (status.plan) {
        "weekly" -> "Weekly Premium"
        "monthly" -> "Monthly Premium"
        "quarterly" -> "Quarterly Premium"
        "annual" -> "Annual Premium"
        "lifetime" -> "Lifetime Premium"
        else -> "Premium Active"
    }
    is SubscriptionStatus.Trial -> "Free Trial Active"
    is SubscriptionStatus.Cancelled -> "Premium — Cancelled"
    is SubscriptionStatus.GracePeriod -> "Billing Issue"
    is SubscriptionStatus.Expired -> "Subscription Expired"
    is SubscriptionStatus.None,
    is SubscriptionStatus.Loading -> "No Active Subscription"
}

private fun statusDetail(status: SubscriptionStatus): String? = when (status) {
    is SubscriptionStatus.Active -> when {
        status.plan == "lifetime" -> "Lifetime access — never expires"
        status.renewsAt != null -> "Renews on ${formatDate(status.renewsAt)}"
        else -> null
    }
    is SubscriptionStatus.Trial -> "Trial ends ${formatDate(status.endsAt)}"
    is SubscriptionStatus.Cancelled -> "Access until ${formatDate(status.accessUntil)}"
    is SubscriptionStatus.GracePeriod -> "Update your payment method to keep access."
    is SubscriptionStatus.Expired -> "Subscribe again to continue tracking your game."
    is SubscriptionStatus.None,
    is SubscriptionStatus.Loading -> "Start a free 3-day trial — no card required."
}

private fun formatDate(instant: kotlinx.datetime.Instant): String {
    return try {
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${local.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${local.dayOfMonth}, ${local.year}"
    } catch (_: Exception) {
        "soon"
    }
}
