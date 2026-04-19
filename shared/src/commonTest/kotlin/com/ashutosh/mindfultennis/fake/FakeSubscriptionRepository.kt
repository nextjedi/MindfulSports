package com.ashutosh.mindfultennis.fake

import com.ashutosh.mindfultennis.data.repository.SubscriptionRepository
import com.ashutosh.mindfultennis.domain.model.SubscriptionStatus
import com.revenuecat.purchases.kmp.models.Offering
import com.revenuecat.purchases.kmp.models.Package
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Test double for [SubscriptionRepository].
 *
 * Configure results before creating the ViewModel under test:
 * ```
 * fakeRepo.offeringResult = Result.failure(Exception("Network error"))
 * ```
 *
 * Set [shouldDelayOffering] to `true` to keep [isLoading] stuck at `true`
 * so you can assert the loading state before it resolves.
 */
class FakeSubscriptionRepository : SubscriptionRepository {

    private val _subscriptionStatus = MutableStateFlow<SubscriptionStatus>(SubscriptionStatus.None)
    override val subscriptionStatus: StateFlow<SubscriptionStatus> = _subscriptionStatus.asStateFlow()

    /** Pre-configure what [getCurrentOffering] returns. */
    var offeringResult: Result<Offering?> = Result.success(null)

    /** Pre-configure what [purchasePackage] returns. */
    var purchaseResult: Result<Unit> = Result.success(Unit)

    /** Pre-configure what [restorePurchases] returns. */
    var restoreResult: Result<Unit> = Result.success(Unit)

    /**
     * When `true`, [getCurrentOffering] suspends indefinitely so the ViewModel stays
     * in the loading state. Useful for asserting `isLoading = true`.
     */
    var shouldDelayOffering = false

    fun setStatus(status: SubscriptionStatus) {
        _subscriptionStatus.value = status
    }

    override suspend fun getCurrentOffering(): Result<Offering?> {
        if (shouldDelayOffering) awaitCancellation()
        return offeringResult
    }

    override suspend fun purchasePackage(rcPackage: Package): Result<Unit> = purchaseResult

    override suspend fun restorePurchases(): Result<Unit> = restoreResult

    override suspend fun identifyUser(userId: String) = Unit

    override suspend fun resetUser() = Unit
}
