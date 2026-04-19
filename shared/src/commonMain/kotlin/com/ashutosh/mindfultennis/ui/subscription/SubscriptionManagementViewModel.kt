package com.ashutosh.mindfultennis.ui.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashutosh.mindfultennis.domain.usecase.GetSubscriptionStatusUseCase
import com.ashutosh.mindfultennis.domain.usecase.RestorePurchasesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SubscriptionManagementViewModel(
    private val getSubscriptionStatusUseCase: GetSubscriptionStatusUseCase,
    private val restorePurchasesUseCase: RestorePurchasesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionManagementUiState())
    val uiState: StateFlow<SubscriptionManagementUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getSubscriptionStatusUseCase().collect { status ->
                _uiState.update { it.copy(subscriptionStatus = status) }
            }
        }
    }

    fun onEvent(event: SubscriptionManagementUiEvent) {
        when (event) {
            is SubscriptionManagementUiEvent.RestoreClicked -> restorePurchases()
            is SubscriptionManagementUiEvent.RestoreResultDismissed -> {
                _uiState.update { it.copy(restoreResult = null) }
            }
            is SubscriptionManagementUiEvent.ErrorDismissed -> {
                _uiState.update { it.copy(error = null) }
            }
        }
    }

    private fun restorePurchases() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true) }
            val result = restorePurchasesUseCase()
            _uiState.update {
                it.copy(
                    isRestoring = false,
                    restoreResult = if (result.isSuccess) "Purchases restored successfully." else null,
                    error = result.exceptionOrNull()?.message?.let { msg ->
                        "Restore failed: ${msg.take(100)}"
                    },
                )
            }
        }
    }
}
