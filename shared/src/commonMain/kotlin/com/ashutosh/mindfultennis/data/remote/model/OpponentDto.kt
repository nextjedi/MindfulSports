package com.ashutosh.mindfultennis.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase DTO for the `opponents` table.
 */
@Serializable
data class OpponentDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("sport_id") val sportId: String = "tennis",
    @SerialName("name") val name: String,
    @SerialName("created_at") val createdAt: Long,
)

/**
 * Supabase DTO for the `partners` table.
 */
@Serializable
data class PartnerDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("sport_id") val sportId: String = "tennis",
    @SerialName("name") val name: String,
    @SerialName("created_at") val createdAt: Long,
)
