package com.ashutosh.mindfultennis.ui.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ashutosh.mindfultennis.data.repository.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PaywallViewModel(
    private val subscriptionRepository: SubscriptionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    init {
        loadOffering()
    }

    fun onEvent(event: PaywallUiEvent) {
        when (event) {
            is PaywallUiEvent.PurchasePackage -> purchasePackage(event)
            is PaywallUiEvent.RestorePurchases -> restorePurchases()
            is PaywallUiEvent.PurchaseCompleted -> {
                _uiState.update { it.copy(purchaseCompleted = true) }
            }
            is PaywallUiEvent.PurchaseError -> {
                _uiState.update { it.copy(isPurchasing = false, error = event.message) }
            }
            is PaywallUiEvent.ErrorDismissed -> {
                _uiState.update { it.copy(error = null) }
            }
        }
    }

    private fun loadOffering() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = subscriptionRepository.getCurrentOffering()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    offering = result.getOrNull(),
                    error = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    private fun purchasePackage(event: PaywallUiEvent.PurchasePackage) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPurchasing = true, error = null) }
            val result = subscriptionRepository.purchasePackage(event.rcPackage)
            result.fold(
                onSuccess = { _uiState.update { it.copy(isPurchasing = false, purchaseCompleted = true) } },
                onFailure = { e -> _uiState.update { it.copy(isPurchasing = false, error = e.message) } },
            )
        }
    }

    private fun restorePurchases() {
        viewModelScope.launch {
            _uiState.update { it.copy(isPurchasing = true, error = null) }
            val result = subscriptionRepository.restorePurchases()
            result.fold(
                onSuccess = { _uiState.update { it.copy(isPurchasing = false, purchaseCompleted = true) } },
                onFailure = { e -> _uiState.update { it.copy(isPurchasing = false, error = e.message) } },
            )
        }
    }
}
