package com.ashutosh.mindfultennis.ui.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ashutosh.mindfultennis.domain.model.SubscriptionStatus
import com.ashutosh.mindfultennis.ui.theme.Spacing
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onLoggedOut: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.isSignedOut, uiState.isAccountDeleted) {
        if (uiState.isSignedOut || uiState.isAccountDeleted) {
            onLoggedOut()
        }
    }

    LaunchedEffect(uiState.syncResult) {
        uiState.syncResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(SettingsUiEvent.SyncResultDismissed)
        }
    }

    LaunchedEffect(uiState.deleteAccountError) {
        uiState.deleteAccountError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(SettingsUiEvent.DeleteAccountErrorDismissed)
        }
    }

    SettingsScreenContent(
        state = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onNavigateToSubscription = onNavigateToSubscription,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenContent(
    state: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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

            // User info section
            Text(
                text = "Account",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(Spacing.sm))

            if (state.displayName != null) {
                Text(
                    text = state.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (state.email != null) {
                Text(
                    text = state.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(Spacing.lg))
            HorizontalDivider()
            Spacer(Modifier.height(Spacing.lg))

            // Subscription section
            Text(
                text = "Subscription",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(Spacing.sm))

            OutlinedButton(
                onClick = onNavigateToSubscription,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(subscriptionStatusLabel(state.subscriptionStatus))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(Spacing.lg))
            HorizontalDivider()
            Spacer(Modifier.height(Spacing.lg))

            // Sync section
            Text(
                text = "Data Sync",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(Spacing.sm))

            if (state.lastSyncTime != null) {
                Text(
                    text = "Last synced: ${formatSyncTime(state.lastSyncTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.sm))
            }

            OutlinedButton(
                onClick = { onEvent(SettingsUiEvent.SyncClicked) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSyncing,
            ) {
                if (state.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Syncing…")
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Sync Now")
                }
            }

            Spacer(Modifier.height(Spacing.lg))

            // Sign out button
            Button(
                onClick = { onEvent(SettingsUiEvent.SignOutClicked) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSigningOut,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                if (state.isSigningOut) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onError,
                        strokeWidth = Spacing.xs / 2,
                    )
                } else {
                    Text("Sign Out")
                }
            }

            Spacer(Modifier.height(Spacing.md))

            // Delete account button
            OutlinedButton(
                onClick = { onEvent(SettingsUiEvent.DeleteAccountClicked) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isDeletingAccount,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                if (state.isDeletingAccount) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.error,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Deleting Account…")
                } else {
                    Text("Delete Account")
                }
            }
        }
    }

    if (state.showDeleteAccountDialog) {
        DeleteAccountDialog(
            onConfirm = { onEvent(SettingsUiEvent.DeleteAccountConfirmed) },
            onDismiss = { onEvent(SettingsUiEvent.DeleteAccountDialogDismissed) },
        )
    }
}

@Composable
private fun DeleteAccountDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmText by rememberSaveable { mutableStateOf("") }
    val isDeleteConfirmed = confirmText.trim().equals("delete", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Account") },
        text = {
            Column {
                Text(
                    text = "This will permanently delete your account and all your data. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = "Type \"delete\" to confirm:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.sm))
                OutlinedTextField(
                    value = confirmText,
                    onValueChange = { confirmText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("delete") },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = isDeleteConfirmed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text("Delete Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private fun subscriptionStatusLabel(status: SubscriptionStatus): String = when (status) {
    is SubscriptionStatus.Active -> {
        val planLabel = when (status.plan) {
            "weekly" -> "Weekly Plan"
            "monthly" -> "Monthly Plan"
            "quarterly" -> "Quarterly Plan"
            "annual" -> "Annual Plan"
            "lifetime" -> "Lifetime Plan"
            else -> "Premium"
        }
        val renewLabel = if (status.plan == "lifetime" || status.renewsAt == null) {
            ""
        } else {
            " — renews ${formatShortDate(status.renewsAt)}"
        }
        "$planLabel$renewLabel"
    }
    is SubscriptionStatus.Trial -> {
        val daysLeft = daysUntil(status.endsAt)
        when {
            daysLeft <= 0 -> "Free Trial — expires today"
            daysLeft == 1L -> "Free Trial — 1 day left"
            else -> "Free Trial — $daysLeft days left"
        }
    }
    is SubscriptionStatus.Cancelled ->
        "Cancelled — access until ${formatShortDate(status.accessUntil)}"
    is SubscriptionStatus.GracePeriod ->
        "Payment Issue — update payment method"
    is SubscriptionStatus.Expired ->
        "Expired — Renew to Continue"
    is SubscriptionStatus.None,
    is SubscriptionStatus.Loading ->
        "Free — Start 3-Day Trial"
}

private fun daysUntil(instant: Instant): Long {
    val nowMs = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    val diffMs = instant.toEpochMilliseconds() - nowMs
    return diffMs / (24 * 60 * 60 * 1000L)
}

private fun formatShortDate(instant: Instant): String {
    return try {
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val month = local.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        "$month ${local.dayOfMonth}"
    } catch (_: Exception) {
        "soon"
    }
}

private fun formatSyncTime(epochMs: Long): String {
    return try {
        val instant = Instant.fromEpochMilliseconds(epochMs)
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${local.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${local.dayOfMonth}, ${local.year} at ${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
    } catch (_: Exception) {
        "Unknown"
    }
}
