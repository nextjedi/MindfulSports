package com.ashutosh.mindfultennis.data.repository

import com.ashutosh.mindfultennis.domain.model.SubscriptionStatus
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesDelegate
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.Offering
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.PeriodType
import com.revenuecat.purchases.kmp.models.StoreProduct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.Instant
import kotlin.coroutines.resume

class SubscriptionRepositoryImpl(
    private val authRepository: AuthRepository,
) : SubscriptionRepository {

    private val _subscriptionStatus = MutableStateFlow<SubscriptionStatus>(SubscriptionStatus.None)
    override val subscriptionStatus: StateFlow<SubscriptionStatus> = _subscriptionStatus.asStateFlow()

    // Lives as long as this singleton — acceptable since the repository is app-scoped.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        // Register a delegate so the Flow stays up-to-date whenever RC refreshes
        // CustomerInfo (e.g. after a purchase, renewal, or app foreground).
        try {
            Purchases.sharedInstance.delegate = object : PurchasesDelegate {
                override fun onCustomerInfoUpdated(customerInfo: CustomerInfo) {
                    _subscriptionStatus.value = customerInfo.toSubscriptionStatus()
                }
                override fun onPurchasePromoProduct(
                    product: StoreProduct,
                    startPurchase: (
                        onError: (com.revenuecat.purchases.kmp.models.PurchasesError, Boolean) -> Unit,
                        onSuccess: (com.revenuecat.purchases.kmp.models.StoreTransaction, CustomerInfo) -> Unit,
                    ) -> Unit,
                ) { /* no-op — we don't use App Store promotional purchases */ }
            }
            // Seed the initial value.
            Purchases.sharedInstance.getCustomerInfo(
                onError = { /* keep SubscriptionStatus.None */ },
                onSuccess = { info -> _subscriptionStatus.value = info.toSubscriptionStatus() },
            )
        } catch (_: Exception) {
            // Purchases not configured yet (should not happen after correct init order).
        }

        // Observe auth state: identify/reset the RC user whenever login state changes.
        // This ensures the RC app_user_id matches the Supabase user ID so webhook events
        // can be routed correctly.
        scope.launch {
            authRepository.authState.collect { state ->
                when (state) {
                    is AuthState.Authenticated -> identifyUser(state.userId)
                    is AuthState.Unauthenticated,
                    is AuthState.SessionExpired -> resetUser()
                    is AuthState.Loading -> Unit
                }
            }
        }
    }

    override suspend fun getCurrentOffering(): Result<Offering?> =
        suspendCancellableCoroutine { cont ->
            Purchases.sharedInstance.getOfferings(
                onError = { error ->
                    if (cont.isActive) cont.resume(Result.failure(Exception(error.message)))
                },
                onSuccess = { offerings ->
                    if (cont.isActive) cont.resume(Result.success(offerings.current))
                },
            )
        }

    override suspend fun purchasePackage(rcPackage: Package): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            Purchases.sharedInstance.purchase(
                packageToPurchase = rcPackage,
                onError = { error: com.revenuecat.purchases.kmp.models.PurchasesError, userCancelled: Boolean ->
                    if (cont.isActive) cont.resume(
                        if (userCancelled) Result.failure(PurchaseCancelledByUserException())
                        else Result.failure(Exception(error.message))
                    )
                },
                onSuccess = { _: com.revenuecat.purchases.kmp.models.StoreTransaction, info: CustomerInfo ->
                    _subscriptionStatus.value = info.toSubscriptionStatus()
                    if (cont.isActive) cont.resume(Result.success(Unit))
                },
            )
        }

    override suspend fun restorePurchases(): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            Purchases.sharedInstance.restorePurchases(
                onError = { error ->
                    if (cont.isActive) cont.resume(Result.failure(Exception(error.message)))
                },
                onSuccess = { info ->
                    _subscriptionStatus.value = info.toSubscriptionStatus()
                    if (cont.isActive) cont.resume(Result.success(Unit))
                },
            )
        }

    override suspend fun identifyUser(userId: String): Unit =
        suspendCancellableCoroutine { cont ->
            Purchases.sharedInstance.logIn(
                newAppUserID = userId,
                onError = { _ -> if (cont.isActive) cont.resume(Unit) },
                onSuccess = { info, _ ->
                    _subscriptionStatus.value = info.toSubscriptionStatus()
                    if (cont.isActive) cont.resume(Unit)
                },
            )
        }

    override suspend fun resetUser(): Unit =
        suspendCancellableCoroutine { cont ->
            Purchases.sharedInstance.logOut(
                onError = { _ -> if (cont.isActive) cont.resume(Unit) },
                onSuccess = { info ->
                    _subscriptionStatus.value = info.toSubscriptionStatus()
                    if (cont.isActive) cont.resume(Unit)
                },
            )
        }
}

// ---------------------------------------------------------------------------
// Mapping helpers
// ---------------------------------------------------------------------------

private fun CustomerInfo.toSubscriptionStatus(): SubscriptionStatus {
    // Check all four premium entitlement IDs in preference order.
    val premiumIds = listOf(
        "premium_lifetime",
        "premium_annual",
        "premium_quarterly",
        "premium_monthly",
    )
    val activeEntitlement = premiumIds.firstNotNullOfOrNull { entitlements.active[it] }
        ?: return SubscriptionStatus.None

    return when (activeEntitlement.periodType) {
        PeriodType.TRIAL -> {
            val millis = activeEntitlement.expirationDateMillis
            val endsAt = if (millis != null) Instant.fromEpochMilliseconds(millis)
                         else Instant.fromEpochSeconds(32503680000L) // year 3000 fallback
            SubscriptionStatus.Trial(endsAt = endsAt)
        }
        else -> SubscriptionStatus.Active
    }
}
