package com.ashutosh.mindfultennis.ui.paywall

import com.revenuecat.purchases.kmp.models.Offering
import com.revenuecat.purchases.kmp.models.Package

data class PaywallUiState(
    val isLoading: Boolean = true,
    val offering: Offering? = null,
    val isPurchasing: Boolean = false,
    val purchaseCompleted: Boolean = false,
    val error: String? = null,
)

sealed interface PaywallUiEvent {
    data class PurchasePackage(val rcPackage: Package) : PaywallUiEvent
    data object RestorePurchases : PaywallUiEvent
    data object PurchaseCompleted : PaywallUiEvent
    data class PurchaseError(val message: String) : PaywallUiEvent
    data object ErrorDismissed : PaywallUiEvent
}
