package com.ashutosh.mindfultennis.domain.model

/**
 * Domain model for a doubles partner.
 */
data class Partner(
    val id: String,
    val userId: String,
    val sportId: String = "tennis",
    val name: String,
    val createdAt: Long,
)
