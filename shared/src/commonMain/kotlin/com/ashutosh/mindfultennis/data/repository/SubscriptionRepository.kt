package com.ashutosh.mindfultennis.data.repository

import com.ashutosh.mindfultennis.domain.model.SubscriptionStatus
import com.revenuecat.purchases.kmp.models.Offering
import com.revenuecat.purchases.kmp.models.Package
import kotlinx.coroutines.flow.StateFlow

interface SubscriptionRepository {

    /** Current subscription status as a hot state flow. Always has a value. */
    val subscriptionStatus: StateFlow<SubscriptionStatus>

    /** Fetches the current RC offering (the packages the user can purchase). */
    suspend fun getCurrentOffering(): Result<Offering?>

    /** Initiates a purchase for the given RC package. */
    suspend fun purchasePackage(rcPackage: Package): Result<Unit>

    /** Restores purchases from the store — updates subscription status on success. */
    suspend fun restorePurchases(): Result<Unit>

    /**
     * Identifies the RC user with the given Supabase user ID.
     * Call this after a successful login so webhook events carry the correct user_id.
     */
    suspend fun identifyUser(userId: String)

    /** Resets RC to an anonymous user. Call this on sign-out. */
    suspend fun resetUser()
}
