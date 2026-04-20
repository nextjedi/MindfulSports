package com.ashutosh.mindfultennis.domain.model

/**
 * Determines whether the Win/Loss card counts by matches (sessions) or individual sets.
 */
enum class WinLossMode(val label: String) {
    MATCHES("Matches"),
    SETS("Sets"),
    GAMES("Games"),
}
