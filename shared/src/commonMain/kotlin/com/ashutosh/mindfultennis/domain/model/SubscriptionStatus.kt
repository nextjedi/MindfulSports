package com.ashutosh.mindfultennis.domain.model

import kotlinx.datetime.Instant

sealed interface SubscriptionStatus {
    /** No active subscription or entitlement. */
    data object None : SubscriptionStatus

    /** Active free trial. */
    data class Trial(val endsAt: Instant) : SubscriptionStatus

    /** Paid subscription is active. */
    data object Active : SubscriptionStatus

    /** Subscription was cancelled but may still have access until period end. */
    data object Cancelled : SubscriptionStatus

    /** Subscription has fully expired — no access. */
    data object Expired : SubscriptionStatus
}

/** True if the user currently has premium access (active or on trial). */
val SubscriptionStatus.hasPremiumAccess: Boolean
    get() = this is SubscriptionStatus.Active ||
        this is SubscriptionStatus.Trial ||
        this is SubscriptionStatus.Cancelled
