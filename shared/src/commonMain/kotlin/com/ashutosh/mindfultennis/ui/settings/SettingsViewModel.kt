package com.ashutosh.mindfultennis.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashutosh.mindfultennis.data.local.datastore.UserPreferences
import com.ashutosh.mindfultennis.data.local.db.MindfulDatabase
import com.ashutosh.mindfultennis.data.remote.SupabaseUserDataSource
import com.ashutosh.mindfultennis.data.repository.AuthRepository
import com.ashutosh.mindfultennis.data.repository.AuthState
import com.ashutosh.mindfultennis.data.sync.SyncManager
import com.ashutosh.mindfultennis.domain.usecase.GetSubscriptionStatusUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val userPreferences: UserPreferences,
    private val syncManager: SyncManager,
    private val supabaseUserDataSource: SupabaseUserDataSource,
    private val mindfulDatabase: MindfulDatabase,
    private val getSubscriptionStatusUseCase: GetSubscriptionStatusUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Load last sync time
            val lastSync = userPreferences.lastSyncTimestamp.first()
            if (lastSync > 0L) {
                _uiState.update { it.copy(lastSyncTime = lastSync) }
            }
        }
        viewModelScope.launch {
            getSubscriptionStatusUseCase().collect { status ->
                _uiState.update { it.copy(subscriptionStatus = status) }
            }
        }
        viewModelScope.launch {
            authRepository.authState.collect { authState ->
                when (authState) {
                    is AuthState.Authenticated -> {
                        _uiState.update {
                            it.copy(
                                email = authState.email,
                                displayName = authState.displayName,
                            )
                        }
                    }
                    is AuthState.Unauthenticated,
                    is AuthState.SessionExpired,
                    is AuthState.Loading -> { /* no-op */ }
                }
            }
        }
    }

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.SignOutClicked -> signOut()
            is SettingsUiEvent.SyncClicked -> sync()
            is SettingsUiEvent.SyncResultDismissed -> {
                _uiState.update { it.copy(syncResult = null) }
            }
            is SettingsUiEvent.DeleteAccountClicked -> {
                _uiState.update { it.copy(showDeleteAccountDialog = true) }
            }
            is SettingsUiEvent.DeleteAccountDialogDismissed -> {
                _uiState.update { it.copy(showDeleteAccountDialog = false) }
            }
            is SettingsUiEvent.DeleteAccountConfirmed -> deleteAccount()
            is SettingsUiEvent.DeleteAccountErrorDismissed -> {
                _uiState.update { it.copy(deleteAccountError = null) }
            }
        }
    }

    private fun sync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncResult = null) }
            val userId = userPreferences.cachedUserId.first()
            if (userId == null) {
                _uiState.update { it.copy(isSyncing = false, syncResult = "Not signed in") }
                return@launch
            }
            val result = syncManager.sync(userId)
            val lastSync = userPreferences.lastSyncTimestamp.first()
            _uiState.update {
                it.copy(
                    isSyncing = false,
                    lastSyncTime = if (lastSync > 0L) lastSync else null,
                    syncResult = if (result.isSuccess) "Sync completed" else "Sync failed: ${result.exceptionOrNull()?.message}",
                )
            }
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSigningOut = true) }
            // Sign out from server first; only clear local data once the server call succeeds
            // (or at minimum attempted). Clearing first risks a broken half-state if signOut throws.
            try {
                authRepository.signOut()
            } finally {
                // Always clear local data — even if server signout fails the user
                // should not be stuck in a broken auth state locally.
                userPreferences.clearAll()
            }
            _uiState.update { it.copy(isSigningOut = false, isSignedOut = true) }
        }
    }

    private fun deleteAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeletingAccount = true, showDeleteAccountDialog = false) }
            try {
                val userId = userPreferences.cachedUserId.first()
                if (userId == null) {
                    _uiState.update { it.copy(isDeletingAccount = false, deleteAccountError = "Not signed in") }
                    return@launch
                }
                // Delete all user data from Supabase (FK cascade removes all related rows)
                supabaseUserDataSource.deleteUser(userId)
                // Delete auth user — this invalidates the session
                supabaseUserDataSource.deleteAuthUser()
                // From here on, server-side ops may fail since session is dead — that's expected
                // Clear all local Room tables
                mindfulDatabase.sessionDao().deleteAllForUser(userId)
                mindfulDatabase.focusPointDao().deleteAllForUser(userId)
                mindfulDatabase.opponentDao().deleteAllForUser(userId)
                mindfulDatabase.partnerDao().deleteAllForUser(userId)
                // Clear preferences
                userPreferences.clearAll()
                // Clear local session (will fail server-side since user is gone, but clears local state)
                try { authRepository.signOut() } catch (_: Exception) {}
                _uiState.update { it.copy(isDeletingAccount = false, isAccountDeleted = true) }
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                val friendlyMessage = when {
                    msg.contains("timeout", ignoreCase = true) ->
                        "Connection timed out. Please check your internet and try again."
                    msg.contains("network", ignoreCase = true) ->
                        "Network error. Please check your connection and try again."
                    else -> "Failed to delete account: ${msg.take(100)}"
                }
                _uiState.update {
                    it.copy(
                        isDeletingAccount = false,
                        deleteAccountError = friendlyMessage,
                    )
                }
            }
        }
    }
}
