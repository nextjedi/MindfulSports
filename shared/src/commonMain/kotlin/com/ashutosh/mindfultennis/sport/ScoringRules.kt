package com.ashutosh.mindfultennis.sport

/**
 * Scoring rules for a sport.
 * Drives the set-score input UI and determines how win/loss results are computed.
 */
data class ScoringRules(
    /** Points needed to win a single game/rally (e.g. 6 for tennis, 21 for badminton, 11 for pickleball). */
    val pointsPerUnit: Int,
    /** Win requires leading by ≥ 2 points when scores are tied at [pointsPerUnit]-1. */
    val winByTwo: Boolean = false,
    /** Tennis-style deuce/ad scoring applies (advantage point after deuce). */
    val hasDeuce: Boolean = false,
    /** A tiebreak game is played when both sides reach [pointsPerUnit] games in a set. */
    val hasTiebreak: Boolean = false,
    /** Number of games/rallies to win one set. Ignored when [hasSets] is false. */
    val unitsPerSet: Int = 1,
    /** Sets required to win the match (first to reach this wins). */
    val setsToWin: Int = 1,
    /** Maximum number of sets that can be played (best-of-N). */
    val maxSets: Int = 1,
    /** Whether the sport is scored in sets (true) or a single game (false). */
    val hasSets: Boolean = false,
    /** UI label for a single scoring unit (e.g. "Game", "Point"). */
    val unitLabel: String = "Game",
    /** UI label for a collection of units (e.g. "Set", "Game"). */
    val setLabel: String = "Set",
    /** UI label for the overall contest (always "Match"). */
    val matchLabel: String = "Match",
)
