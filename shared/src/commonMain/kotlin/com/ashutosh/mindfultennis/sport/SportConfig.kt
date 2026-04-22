package com.ashutosh.mindfultennis.sport

/**
 * Identifies a sport and carries all sport-specific runtime configuration.
 *
 * A single [SportConfig] instance is injected via Koin at startup so that
 * every screen, use case, and data source operates in the correct sport context
 * without needing to know about the other 9 sports.
 *
 * One codebase — 10 apps. The only thing that differs at compile time is
 * the [sportId] (from BuildConfig.SPORT_ID / xcconfig SPORT_ID).
 */
data class SportConfig(
    /**
     * Stable lowercase identifier used in the DB, Supabase tables,
     * RevenueCat project keys, and deep-link schemes.
     * Values: "tennis" | "badminton" | "pickleball" | "squash" | "tabletennis" |
     *         "padel" | "racquetball" | "platformtennis" | "poptennis" | "beachtennis"
     */
    val sportId: String,

    /** Human-readable name shown in the UI (e.g. "Table Tennis", "Beach Tennis"). */
    val displayName: String,

    /**
     * Android/iOS deep-link scheme for OAuth callback routing.
     * Must match the scheme registered in the store listing and in Supabase Auth.
     * E.g. "com.mindful.tennis".
     */
    val deepLinkScheme: String,

    /** Sport-specific display labels for the 8 fixed Aspect rating slots. */
    val terminology: SportTerminology,

    /** Scoring rules that drive the set-score input UI and result computation. */
    val scoringRules: ScoringRules,
)
