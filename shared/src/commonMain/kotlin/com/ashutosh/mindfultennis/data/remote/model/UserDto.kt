package com.ashutosh.mindfultennis.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase DTO for the `users` table.
 */
@Serializable
data class UserDto(
    @SerialName("id") val id: String,
    @SerialName("email") val email: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("time_zone") val timeZone: String,
    @SerialName("trial_started_at") val trialStartedAt: Long? = null,
)
