package com.ashutosh.mindfultennis.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase DTO for the `sessions` table.
 */
@Serializable
data class SessionDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("sport_id") val sportId: String = "tennis",
    @SerialName("focus_note") val focusNote: String,
    @SerialName("started_at") val startedAt: Long,
    @SerialName("ended_at") val endedAt: Long? = null,
    @SerialName("time_zone_id") val timeZoneId: String,
    @SerialName("notes") val notes: String? = null,
    @SerialName("match_type") val matchType: String = "SINGLES",
    @SerialName("opponent1_id") val opponent1Id: String? = null,
    @SerialName("opponent2_id") val opponent2Id: String? = null,
    @SerialName("partner_id") val partnerId: String? = null,
    @SerialName("is_active") val isActive: Boolean = false,
    @SerialName("overall_score") val overallScore: Int? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("schema_version") val schemaVersion: Int = 1,
)
