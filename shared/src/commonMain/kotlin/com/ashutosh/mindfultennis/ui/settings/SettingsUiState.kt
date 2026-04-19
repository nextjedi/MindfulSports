package com.ashutosh.mindfultennis.ui.settings

import com.ashutosh.mindfultennis.domain.model.SubscriptionStatus

data class SettingsUiState(
    val subscriptionStatus: SubscriptionStatus = SubscriptionStatus.None,
    val email: String? = null,
    val displayName: String? = null,
    val isSigningOut: Boolean = false,
    val isSignedOut: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncTime: Long? = null,
    val syncResult: String? = null,
    val showDeleteAccountDialog: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val isAccountDeleted: Boolean = false,
    val deleteAccountError: String? = null,
)

sealed interface SettingsUiEvent {
    data object SignOutClicked : SettingsUiEvent
    data object SyncClicked : SettingsUiEvent
    data object SyncResultDismissed : SettingsUiEvent
    data object DeleteAccountClicked : SettingsUiEvent
    data object DeleteAccountConfirmed : SettingsUiEvent
    data object DeleteAccountDialogDismissed : SettingsUiEvent
    data object DeleteAccountErrorDismissed : SettingsUiEvent
}
