package com.ashutosh.mindfultennis.ui.paywall

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ashutosh.mindfultennis.ui.theme.Spacing
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.PackageType
import kotlinx.coroutines.delay

@Composable
fun PaywallScreen(
    viewModel: PaywallViewModel,
    /** Null when the paywall is a mandatory gate (no back button shown). */
    onNavigateBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show welcome screen for 2.5 s, then navigate.
    // For the mandatory gate onNavigateBack is null — NavGraph routes to Home
    // once hasPremiumAccess resolves. The delay also gives RC time to push the
    // status update before the user lands on any downstream screen.
    LaunchedEffect(uiState.purchaseCompleted) {
        if (uiState.purchaseCompleted) {
            delay(2_500)
            onNavigateBack?.invoke()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(PaywallUiEvent.ErrorDismissed)
        }
    }

    PaywallScreenContent(
        state = uiState,
        onNavigateBack = onNavigateBack,
        onPurchasePackage = { pkg -> viewModel.onEvent(PaywallUiEvent.PurchasePackage(pkg)) },
        onRestorePurchases = { viewModel.onEvent(PaywallUiEvent.RestorePurchases) },
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaywallScreenContent(
    state: PaywallUiState,
    onNavigateBack: (() -> Unit)?,
    onPurchasePackage: (Package) -> Unit,
    onRestorePurchases: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Upgrade to Premium") },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack, enabled = !state.isPurchasing) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Normal paywall content (hidden during welcome)
            AnimatedVisibility(visible = !state.purchaseCompleted, exit = fadeOut()) {
                when {
                    state.isLoading -> {
                        Box(Modifier.fillMaxSize()) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    }
                    state.offering == null -> {
                        ErrorState(modifier = Modifier.align(Alignment.Center))
                    }
                    else -> {
                        var selectedPackageId by rememberSaveable {
                            mutableStateOf(state.offering.availablePackages.firstOrNull()?.identifier)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = Spacing.md, vertical = Spacing.lg),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(Spacing.sm))
                            Text(
                                text = "MindfulTennis Premium",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(Spacing.xs))
                            Text(
                                text = "Unlock full access to all features",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(Spacing.lg))

                            state.offering.availablePackages.forEach { pkg ->
                                val isSelected = pkg.identifier == selectedPackageId
                                PackageCard(
                                    rcPackage = pkg,
                                    isSelected = isSelected,
                                    onClick = { selectedPackageId = pkg.identifier },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = Spacing.xs),
                                )
                            }

                            Spacer(Modifier.height(Spacing.lg))

                            val selectedPkg = state.offering.availablePackages
                                .firstOrNull { it.identifier == selectedPackageId }

                            Button(
                                onClick = { selectedPkg?.let { onPurchasePackage(it) } },
                                enabled = selectedPkg != null && !state.isPurchasing,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                if (state.isPurchasing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                } else {
                                    Text("Continue")
                                }
                            }

                            Spacer(Modifier.height(Spacing.sm))

                            TextButton(
                                onClick = onRestorePurchases,
                                enabled = !state.isPurchasing,
                            ) {
                                Text("Restore Purchases")
                            }
                        }
                    }
                }
            }

            // Premium welcome overlay — fades in after purchase
            AnimatedVisibility(
                visible = state.purchaseCompleted,
                enter = fadeIn(),
                modifier = Modifier.fillMaxSize(),
            ) {
                PremiumWelcomeContent()
            }
        }
    }
}

@Composable
private fun PremiumWelcomeContent(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(88.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(Spacing.lg))
            Text(
                text = "You're Premium!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "Enjoy full access to all MindfulTennis features. Track every session, review your progress, and keep improving your game.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.xl))
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun PackageCard(
    rcPackage: Package,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        onClick = onClick,
        modifier = modifier.border(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor,
            shape = RoundedCornerShape(12.dp),
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = rcPackage.displayName(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
                Text(
                    text = rcPackage.storeProduct.price.formatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun ErrorState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Subscription plans unavailable",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = "Check your internet connection and try again.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun Package.displayName(): String = when (packageType) {
    PackageType.ANNUAL -> "Annual"
    PackageType.SIX_MONTH -> "6 Months"
    PackageType.THREE_MONTH -> "3 Months"
    PackageType.MONTHLY -> "Monthly"
    PackageType.TWO_MONTH -> "2 Months"
    PackageType.WEEKLY -> "Weekly"
    PackageType.LIFETIME -> "Lifetime"
    else -> storeProduct.title.ifBlank { identifier }
}
