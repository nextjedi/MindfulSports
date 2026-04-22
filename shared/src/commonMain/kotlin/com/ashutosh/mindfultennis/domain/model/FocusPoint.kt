package com.ashutosh.mindfultennis.domain.model

/**
 * Domain model for a focus point that the user wants to work on.
 *
 * @property averageScore The average overall performance score (0-100) across
 *   all completed sessions that used this focus point. Null if no rated sessions exist.
 */
data class FocusPoint(
    val id: String,
    val userId: String,
    val sportId: String = "tennis",
    val text: String,
    val category: String? = null,
    val createdAt: Long,
    val averageScore: Int? = null,
)
