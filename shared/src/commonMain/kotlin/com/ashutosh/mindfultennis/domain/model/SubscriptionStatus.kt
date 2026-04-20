package com.ashutosh.mindfultennis.domain.model

import kotlinx.datetime.Instant

sealed interface SubscriptionStatus {
    /** Initial state — cache not yet read. Navigation is suppressed until this resolves. */
    data object Loading : SubscriptionStatus

    /** No active subscription — trial not started or has expired. */
    data object None : SubscriptionStatus

    /** Free 3-day trial — no payment method required. */
    data class Trial(val endsAt: Instant) : SubscriptionStatus

    /** Active paid subscription. */
    data class Active(
        val plan: String? = null,       // "weekly" | "monthly" | "quarterly" | "annual" | "lifetime"
        val renewsAt: Instant? = null,  // null for lifetime plans
    ) : SubscriptionStatus

    /** Subscription cancelled but still within the paid billing period — access allowed. */
    data class Cancelled(val accessUntil: Instant) : SubscriptionStatus

    /** Billing issue — RC in grace period; access allowed while payment is retried. */
    data object GracePeriod : SubscriptionStatus

    /** Subscription fully expired — no access. */
    data object Expired : SubscriptionStatus
}

/** True when the user currently has premium access. */
val SubscriptionStatus.hasPremiumAccess: Boolean
    get() = when (this) {
        is SubscriptionStatus.Active,
        is SubscriptionStatus.Trial,
        is SubscriptionStatus.Cancelled,
        is SubscriptionStatus.GracePeriod -> true
        is SubscriptionStatus.Loading,
        is SubscriptionStatus.None,
        is SubscriptionStatus.Expired -> false
    }
