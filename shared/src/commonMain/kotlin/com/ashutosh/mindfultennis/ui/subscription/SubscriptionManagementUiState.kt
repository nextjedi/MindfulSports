package com.ashutosh.mindfultennis.ui.subscription

import com.ashutosh.mindfultennis.domain.model.SubscriptionStatus

data class SubscriptionManagementUiState(
    val subscriptionStatus: SubscriptionStatus = SubscriptionStatus.None,
    val isRestoring: Boolean = false,
    val restoreResult: String? = null,
    val error: String? = null,
)

sealed interface SubscriptionManagementUiEvent {
    data object RestoreClicked : SubscriptionManagementUiEvent
    data object RestoreResultDismissed : SubscriptionManagementUiEvent
    data object ErrorDismissed : SubscriptionManagementUiEvent
}
