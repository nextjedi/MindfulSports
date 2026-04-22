package com.ashutosh.mindfultennis.domain.model

/**
 * Domain model for a tennis session.
 * Clean data class — no Room or serialization annotations.
 */
data class Session(
    val id: String,
    val userId: String,
    val sportId: String = "tennis",
    val focusNote: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val timeZoneId: String,
    val notes: String? = null,
    val matchType: MatchType = MatchType.SINGLES,
    val opponent1Id: String? = null,
    val opponent2Id: String? = null,
    val partnerId: String? = null,
    val isActive: Boolean = false,
    val overallScore: Int? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val schemaVersion: Int = 1,
)
