package com.ashutosh.mindfultennis.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase DTO for the `subscriptions` table.
 * Used to read status, plan, and period info for combining with RevenueCat state.
 */
@Serializable
data class SubscriptionRowDto(
    @SerialName("status") val status: String,                       // active | trial | cancelled | expired | grace_period
    @SerialName("plan") val plan: String? = null,                   // weekly | monthly | quarterly | annual | lifetime
    @SerialName("current_period_end") val currentPeriodEndMs: Long? = null,
    @SerialName("is_trial") val isTrial: Boolean = false,
    @SerialName("trial_ends_at") val trialEndsAtMs: Long? = null,
    @SerialName("store") val store: String? = null,
)
