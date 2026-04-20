package com.ashutosh.mindfultennis.data.repository

import com.ashutosh.mindfultennis.data.local.datastore.UserPreferences
import com.ashutosh.mindfultennis.data.notification.NotificationScheduler
import com.ashutosh.mindfultennis.data.remote.SupabaseSubscriptionDataSource
import com.ashutosh.mindfultennis.data.remote.model.SubscriptionRowDto
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.coroutines.resume

private const val TRIAL_DURATION_MS = 3L * 24 * 60 * 60 * 1000   // 3 days
private const val OFFLINE_GRACE_MS  = 72L * 60 * 60 * 1000        // 72 hours

class SubscriptionRepositoryImpl(
    private val authRepository: AuthRepository,
    private val supabaseSubscriptionDataSource: SupabaseSubscriptionDataSource,
    private val userPreferences: UserPreferences,
    private val notificationScheduler: NotificationScheduler,
) : SubscriptionRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _subscriptionStatus = MutableStateFlow<SubscriptionStatus>(SubscriptionStatus.Loading)
    override val subscriptionStatus: StateFlow<SubscriptionStatus> = _subscriptionStatus.asStateFlow()

    // In-memory cache of server-fetched data (lives for the session)
    private var trialStartedAtMs: Long? = null
    private var activeSubscriptionRow: SubscriptionRowDto? = null
    private var currentUserId: String? = null

    init {
        // Restore offline-cached status before anything else so the UI doesn't flash paywall.
        // This resolves Loading → real cached value; RC/Supabase will refine it shortly after.
        scope.launch {
            val cached = userPreferences.cachedSubscriptionStatus.first()
            val verifiedAt = userPreferences.subscriptionStatusVerifiedAt.first()
            val trialStart = userPreferences.trialStartedAtMs.first()
            trialStartedAtMs = trialStart
            _subscriptionStatus.value = if (cached != null)
                resolveOfflineStatus(cached, verifiedAt)
            else
                SubscriptionStatus.None  // First launch — no cache
        }

        // Register RC delegate so we stay in sync when RC refreshes CustomerInfo
        try {
            Purchases.sharedInstance.delegate = object : PurchasesDelegate {
                override fun onCustomerInfoUpdated(customerInfo: CustomerInfo) {
                    scope.launch {
                        refreshSupabaseSubscription()
                        applyRcInfo(customerInfo)
                    }
                }
                override fun onPurchasePromoProduct(
                    product: StoreProduct,
                    startPurchase: (
                        onError: (com.revenuecat.purchases.kmp.models.PurchasesError, Boolean) -> Unit,
                        onSuccess: (com.revenuecat.purchases.kmp.models.StoreTransaction, CustomerInfo) -> Unit,
                    ) -> Unit,
                ) { /* no-op */ }
            }
            // Seed with current RC state
            Purchases.sharedInstance.getCustomerInfo(
                onError = { /* keep offline cache */ },
                onSuccess = { info -> scope.launch { applyRcInfo(info) } },
            )
        } catch (_: Exception) {
            // RC not configured yet — rely on cached state
        }

        // Auto-identify / reset when auth state changes
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

    // ── SubscriptionRepository API ──────────────────────────────────────

    override suspend fun getCurrentOffering(): Result<Offering?> =
        suspendCancellableCoroutine { cont ->
            Purchases.sharedInstance.getOfferings(
                onError = { e -> if (cont.isActive) cont.resume(Result.failure(Exception(e.message))) },
                onSuccess = { o -> if (cont.isActive) cont.resume(Result.success(o.current)) },
            )
        }

    override suspend fun purchasePackage(rcPackage: Package): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            Purchases.sharedInstance.purchase(
                packageToPurchase = rcPackage,
                onError = { error, userCancelled ->
                    if (cont.isActive) cont.resume(
                        if (userCancelled) Result.failure(PurchaseCancelledByUserException())
                        else Result.failure(Exception(error.message))
                    )
                },
                onSuccess = { _, info ->
                    scope.launch {
                        // Refresh Supabase so we have the plan name from the RC webhook
                        refreshSupabaseSubscription()
                        applyRcInfo(info)
                    }
                    if (cont.isActive) cont.resume(Result.success(Unit))
                },
            )
        }

    override suspend fun restorePurchases(): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            Purchases.sharedInstance.restorePurchases(
                onError = { e -> if (cont.isActive) cont.resume(Result.failure(Exception(e.message))) },
                onSuccess = { info ->
                    scope.launch {
                        refreshSupabaseSubscription()
                        applyRcInfo(info)
                    }
                    if (cont.isActive) cont.resume(Result.success(Unit))
                },
            )
        }

    override suspend fun identifyUser(userId: String) {
        currentUserId = userId

        // 1. Log into RC with the Supabase user ID
        val rcInfo = suspendCancellableCoroutine<CustomerInfo?> { cont ->
            Purchases.sharedInstance.logIn(
                newAppUserID = userId,
                onError = { _ -> if (cont.isActive) cont.resume(null) },
                onSuccess = { info, _ -> if (cont.isActive) cont.resume(info) },
            )
        }

        // 2. Fetch trial start + active subscription from Supabase
        try {
            val trialStart = supabaseSubscriptionDataSource.getTrialStartedAt(userId)
            val row = supabaseSubscriptionDataSource.getActiveSubscription(userId)
            trialStartedAtMs = trialStart
            activeSubscriptionRow = row
            trialStart?.let { userPreferences.setTrialStartedAt(it) }
        } catch (_: Exception) {
            // Offline: fall back to DataStore-cached trial start
            if (trialStartedAtMs == null) {
                trialStartedAtMs = userPreferences.trialStartedAtMs.first()
            }
        }

        // 3. Compute and apply combined status
        if (rcInfo != null) {
            applyRcInfo(rcInfo)
        } else {
            // RC offline: compute from Supabase + trial data only
            val status = computeStatus(rcInfo = null)
            emit(status)
        }

        // 4. Request notification permission on first login
        scope.launch { notificationScheduler.requestPermission() }
    }

    override suspend fun resetUser() {
        suspendCancellableCoroutine { cont ->
            Purchases.sharedInstance.logOut(
                onError = { _ -> if (cont.isActive) cont.resume(Unit) },
                onSuccess = { info ->
                    _subscriptionStatus.value = SubscriptionStatus.None
                    if (cont.isActive) cont.resume(Unit)
                },
            )
        }
        trialStartedAtMs = null
        activeSubscriptionRow = null
        currentUserId = null
        _subscriptionStatus.value = SubscriptionStatus.None
        notificationScheduler.cancelAll()
    }

    // ── Internal helpers ────────────────────────────────────────────────

    private suspend fun applyRcInfo(info: CustomerInfo) {
        val status = computeStatus(info)
        emit(status)
    }

    /**
     * Combines RC [CustomerInfo] with app-side trial and Supabase subscription data
     * to produce the authoritative [SubscriptionStatus].
     *
     * Priority:
     *  1. RC active paid entitlement → [Active] or [Trial] (RC-managed trial)
     *  2. Supabase grace_period row  → [GracePeriod]
     *  3. Supabase cancelled row still in period → [Cancelled]
     *  4. App-side 3-day trial still valid → [Trial]
     *  5. Otherwise → [None]
     */
    private fun computeStatus(rcInfo: CustomerInfo?): SubscriptionStatus {
        if (rcInfo != null) {
            val premiumEntitlements = listOf(
                "premium_lifetime", "premium_annual", "premium_quarterly",
                "premium_monthly", "premium_weekly",
            )
            val active = premiumEntitlements.firstNotNullOfOrNull { rcInfo.entitlements.active[it] }
            if (active != null) {
                val expiresAt = active.expirationDateMillis?.let { Instant.fromEpochMilliseconds(it) }
                return if (active.periodType == PeriodType.TRIAL) {
                    SubscriptionStatus.Trial(endsAt = expiresAt ?: Instant.fromEpochSeconds(32503680000L))
                } else {
                    SubscriptionStatus.Active(
                        plan = activeSubscriptionRow?.plan,
                        renewsAt = expiresAt,
                    )
                }
            }
        }

        // Grace period / cancelled from Supabase
        val row = activeSubscriptionRow
        if (row != null) {
            val nowMs = Clock.System.now().toEpochMilliseconds()
            when (row.status) {
                "grace_period" -> return SubscriptionStatus.GracePeriod
                "cancelled" -> {
                    val until = row.currentPeriodEndMs
                    if (until != null && until > nowMs) {
                        return SubscriptionStatus.Cancelled(
                            accessUntil = Instant.fromEpochMilliseconds(until)
                        )
                    }
                }
            }
        }

        // App-side 3-day trial
        val trialStart = trialStartedAtMs
        if (trialStart != null) {
            val trialEndMs = trialStart + TRIAL_DURATION_MS
            val nowMs = Clock.System.now().toEpochMilliseconds()
            if (nowMs < trialEndMs) {
                return SubscriptionStatus.Trial(endsAt = Instant.fromEpochMilliseconds(trialEndMs))
            }
        }

        return SubscriptionStatus.None
    }

    private suspend fun emit(status: SubscriptionStatus) {
        val previous = _subscriptionStatus.value
        _subscriptionStatus.value = status

        // Persist for offline use
        val nowMs = Clock.System.now().toEpochMilliseconds()
        userPreferences.setSubscriptionStatus(serializeStatus(status), nowMs)

        // Schedule notifications on status transitions
        if (status != previous) {
            scheduleNotificationsForStatus(status)
        }
    }

    private fun scheduleNotificationsForStatus(status: SubscriptionStatus) {
        when (status) {
            is SubscriptionStatus.Trial -> {
                notificationScheduler.scheduleTrial(status.endsAt.toEpochMilliseconds())
            }
            is SubscriptionStatus.Active -> {
                notificationScheduler.cancelTrialNotifications()
                status.renewsAt?.let {
                    notificationScheduler.scheduleRenewalReminder(it.toEpochMilliseconds())
                }
            }
            is SubscriptionStatus.Cancelled -> {
                notificationScheduler.showSubscriptionCancelledNotification()
                notificationScheduler.scheduleAccessEndingReminder(status.accessUntil.toEpochMilliseconds())
            }
            is SubscriptionStatus.GracePeriod -> notificationScheduler.showBillingIssueNotification()
            is SubscriptionStatus.Expired -> notificationScheduler.showSubscriptionExpiredNotification()
            is SubscriptionStatus.None,
            is SubscriptionStatus.Loading -> Unit
        }
    }

    private suspend fun refreshSupabaseSubscription() {
        val uid = currentUserId ?: return
        try {
            activeSubscriptionRow = supabaseSubscriptionDataSource.getActiveSubscription(uid)
        } catch (_: Exception) { /* keep stale cache */ }
    }

    // ── Offline status serialization ────────────────────────────────────

    private fun serializeStatus(status: SubscriptionStatus): String = when (status) {
        is SubscriptionStatus.Active ->
            "active:${status.plan.orEmpty()}:${status.renewsAt?.toEpochMilliseconds() ?: 0}"
        is SubscriptionStatus.Trial ->
            "trial:${status.endsAt.toEpochMilliseconds()}"
        is SubscriptionStatus.Cancelled ->
            "cancelled:${status.accessUntil.toEpochMilliseconds()}"
        is SubscriptionStatus.GracePeriod -> "grace_period"
        is SubscriptionStatus.Expired -> "expired"
        is SubscriptionStatus.None,
        is SubscriptionStatus.Loading -> "none"
    }

    private fun resolveOfflineStatus(serialized: String, verifiedAtMs: Long): SubscriptionStatus {
        val nowMs = Clock.System.now().toEpochMilliseconds()
        if (verifiedAtMs == 0L || (nowMs - verifiedAtMs) > OFFLINE_GRACE_MS) {
            return SubscriptionStatus.None  // Stale — require re-verification
        }
        return deserializeStatus(serialized, nowMs)
    }

    private fun deserializeStatus(serialized: String, nowMs: Long): SubscriptionStatus {
        val parts = serialized.split(":")
        return when (parts[0]) {
            "active" -> SubscriptionStatus.Active(
                plan = parts.getOrNull(1)?.takeIf { it.isNotEmpty() },
                renewsAt = parts.getOrNull(2)?.toLongOrNull()
                    ?.takeIf { it > 0 }
                    ?.let { Instant.fromEpochMilliseconds(it) },
            )
            "trial" -> {
                val endMs = parts.getOrNull(1)?.toLongOrNull() ?: return SubscriptionStatus.None
                if (endMs < nowMs) SubscriptionStatus.None  // Expired even offline
                else SubscriptionStatus.Trial(Instant.fromEpochMilliseconds(endMs))
            }
            "cancelled" -> {
                val until = parts.getOrNull(1)?.toLongOrNull() ?: return SubscriptionStatus.None
                if (until < nowMs) SubscriptionStatus.None  // Access period ended offline
                else SubscriptionStatus.Cancelled(Instant.fromEpochMilliseconds(until))
            }
            "grace_period" -> SubscriptionStatus.GracePeriod
            "expired" -> SubscriptionStatus.Expired
            else -> SubscriptionStatus.None
        }
    }
}
