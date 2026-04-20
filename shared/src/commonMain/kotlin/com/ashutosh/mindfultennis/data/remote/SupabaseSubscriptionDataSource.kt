package com.ashutosh.mindfultennis.data.remote

import com.ashutosh.mindfultennis.data.remote.model.SubscriptionRowDto
import com.ashutosh.mindfultennis.data.remote.model.UserDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

/**
 * Reads subscription and trial data from Supabase for combining with RevenueCat state.
 */
class SupabaseSubscriptionDataSource(
    private val supabaseClient: SupabaseClient,
) {
    /**
     * Returns the epoch-ms timestamp when the user's trial started,
     * or null if the user row doesn't exist yet.
     */
    suspend fun getTrialStartedAt(userId: String): Long? {
        return supabaseClient.postgrest["users"]
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<UserDto>()
            ?.trialStartedAt
    }

    /**
     * Returns the most recent non-expired subscription row for the user,
     * or null if none exists. Filters to active, grace_period, and cancelled
     * rows since those are the ones that affect access decisions.
     */
    suspend fun getActiveSubscription(userId: String): SubscriptionRowDto? {
        return supabaseClient.postgrest["subscriptions"]
            .select {
                filter {
                    eq("user_id", userId)
                    isIn("status", listOf("active", "trial", "grace_period", "cancelled"))
                }
                order("updated_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                limit(1)
            }
            .decodeSingleOrNull<SubscriptionRowDto>()
    }
}
