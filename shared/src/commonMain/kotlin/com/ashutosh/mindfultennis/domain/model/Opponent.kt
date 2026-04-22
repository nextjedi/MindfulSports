package com.ashutosh.mindfultennis.domain.model

/**
 * Domain model for an opponent the user plays against.
 */
data class Opponent(
    val id: String,
    val userId: String,
    val sportId: String = "tennis",
    val name: String,
    val createdAt: Long,
)
