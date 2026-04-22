package com.ashutosh.mindfultennis.sport

/**
 * Canonical [SportConfig] definitions for all 10 supported racket sports.
 *
 * Usage:
 *   val config = SportRegistry.fromId(BuildConfig.SPORT_ID)  // Android
 *   val config = SportRegistry.fromId(sportIdFromInfoPlist)   // iOS
 *
 * [fromId] falls back to [tennis] for unknown sport IDs so the app never
 * crashes on a mis-configured build.
 */
object SportRegistry {

    // ── 1. Tennis ────────────────────────────────────────────────────────────

    val tennis = SportConfig(
        sportId = "tennis",
        displayName = "Tennis",
        deepLinkScheme = "com.mindful.tennis",
        terminology = SportTerminology(
            skill5 = "Volley",
            skill6 = "Slice",
        ),
        scoringRules = ScoringRules(
            pointsPerUnit = 6,
            winByTwo = true,
            hasDeuce = true,
            hasTiebreak = true,
            unitsPerSet = 6,
            setsToWin = 2,
            maxSets = 3,
            hasSets = true,
            unitLabel = "Game",
            setLabel = "Set",
        ),
    )

    // ── 2. Badminton ─────────────────────────────────────────────────────────

    val badminton = SportConfig(
        sportId = "badminton",
        displayName = "Badminton",
        deepLinkScheme = "com.mindful.badminton",
        terminology = SportTerminology(
            skill5 = "Smash",
            skill6 = "Clear",
            movement = "Footwork",
        ),
        scoringRules = ScoringRules(
            pointsPerUnit = 21,
            winByTwo = true,
            setsToWin = 2,
            maxSets = 3,
            hasSets = true,
            unitLabel = "Point",
            setLabel = "Game",
        ),
    )

    // ── 3. Pickleball ────────────────────────────────────────────────────────

    val pickleball = SportConfig(
        sportId = "pickleball",
        displayName = "Pickleball",
        deepLinkScheme = "com.mindful.pickleball",
        terminology = SportTerminology(
            skill5 = "Dink",
            skill6 = "Third-Shot Drop",
        ),
        scoringRules = ScoringRules(
            pointsPerUnit = 11,
            winByTwo = true,
            setsToWin = 1,
            maxSets = 1,
            hasSets = false,
            unitLabel = "Point",
            setLabel = "Game",
        ),
    )

    // ── 4. Squash ────────────────────────────────────────────────────────────

    val squash = SportConfig(
        sportId = "squash",
        displayName = "Squash",
        deepLinkScheme = "com.mindful.squash",
        terminology = SportTerminology(
            skill5 = "Drop Shot",
            skill6 = "Boast",
        ),
        scoringRules = ScoringRules(
            pointsPerUnit = 11,
            winByTwo = true,
            setsToWin = 3,
            maxSets = 5,
            hasSets = true,
            unitLabel = "Point",
            setLabel = "Game",
        ),
    )

    // ── 5. Table Tennis ──────────────────────────────────────────────────────

    val tableTennis = SportConfig(
        sportId = "tabletennis",
        displayName = "Table Tennis",
        deepLinkScheme = "com.mindful.tabletennis",
        terminology = SportTerminology(
            skill5 = "Loop / Topspin",
            skill6 = "Block",
            movement = "Footwork",
        ),
        scoringRules = ScoringRules(
            pointsPerUnit = 11,
            winByTwo = true,
            setsToWin = 4,
            maxSets = 7,
            hasSets = true,
            unitLabel = "Point",
            setLabel = "Game",
        ),
    )

    // ── 6. Padel ─────────────────────────────────────────────────────────────

    val padel = SportConfig(
        sportId = "padel",
        displayName = "Padel",
        deepLinkScheme = "com.mindful.padel",
        terminology = SportTerminology(
            skill5 = "Bandeja",
            skill6 = "Víbora",
        ),
        scoringRules = ScoringRules(
            pointsPerUnit = 6,
            winByTwo = true,
            hasDeuce = true,
            hasTiebreak = true,
            unitsPerSet = 6,
            setsToWin = 2,
            maxSets = 3,
            hasSets = true,
            unitLabel = "Game",
            setLabel = "Set",
        ),
    )

    // ── 7. Racquetball ───────────────────────────────────────────────────────

    val racquetball = SportConfig(
        sportId = "racquetball",
        displayName = "Racquetball",
        deepLinkScheme = "com.mindful.racquetball",
        terminology = SportTerminology(
            skill5 = "Kill Shot",
            skill6 = "Ceiling Ball",
        ),
        scoringRules = ScoringRules(
            pointsPerUnit = 15,
            winByTwo = false,
            setsToWin = 2,
            maxSets = 3,
            hasSets = true,
            unitLabel = "Point",
            setLabel = "Game",
        ),
    )

    // ── 8. Platform Tennis ───────────────────────────────────────────────────

    val platformTennis = SportConfig(
        sportId = "platformtennis",
        displayName = "Platform Tennis",
        deepLinkScheme = "com.mindful.platformtennis",
        terminology = SportTerminology(
            skill5 = "Screen Rally",
            skill6 = "Lob",
        ),
        scoringRules = ScoringRules(
            pointsPerUnit = 6,
            winByTwo = true,
            hasDeuce = true,
            hasTiebreak = true,
            unitsPerSet = 6,
            setsToWin = 2,
            maxSets = 3,
            hasSets = true,
            unitLabel = "Game",
            setLabel = "Set",
        ),
    )

    // ── 9. Pop Tennis ────────────────────────────────────────────────────────

    val popTennis = SportConfig(
        sportId = "poptennis",
        displayName = "Pop Tennis",
        deepLinkScheme = "com.mindful.poptennis",
        terminology = SportTerminology(
            skill5 = "Overhead",
            skill6 = "Touch Volley",
        ),
        scoringRules = ScoringRules(
            pointsPerUnit = 6,
            winByTwo = true,
            hasDeuce = false, // no-ad scoring is standard in Pop Tennis
            hasTiebreak = true,
            unitsPerSet = 6,
            setsToWin = 2,
            maxSets = 3,
            hasSets = true,
            unitLabel = "Game",
            setLabel = "Set",
        ),
    )

    // ── 10. Beach Tennis ─────────────────────────────────────────────────────

    val beachTennis = SportConfig(
        sportId = "beachtennis",
        displayName = "Beach Tennis",
        deepLinkScheme = "com.mindful.beachtennis",
        terminology = SportTerminology(
            skill5 = "Smash",
            skill6 = "Lob",
            movement = "Sand Movement",
        ),
        scoringRules = ScoringRules(
            pointsPerUnit = 6,
            winByTwo = true,
            hasDeuce = true,
            hasTiebreak = true,
            unitsPerSet = 6,
            setsToWin = 2,
            maxSets = 3,
            hasSets = true,
            unitLabel = "Game",
            setLabel = "Set",
        ),
    )

    // ── Registry ─────────────────────────────────────────────────────────────

    val all: List<SportConfig> = listOf(
        tennis, badminton, pickleball, squash, tableTennis,
        padel, racquetball, platformTennis, popTennis, beachTennis,
    )

    /**
     * Look up a sport by its stable [sportId] string.
     * Falls back to [tennis] for unknown IDs to prevent crashes on mis-configured builds.
     */
    fun fromId(sportId: String): SportConfig = when (sportId) {
        "tennis"         -> tennis
        "badminton"      -> badminton
        "pickleball"     -> pickleball
        "squash"         -> squash
        "tabletennis"    -> tableTennis
        "padel"          -> padel
        "racquetball"    -> racquetball
        "platformtennis" -> platformTennis
        "poptennis"      -> popTennis
        "beachtennis"    -> beachTennis
        else             -> tennis
    }
}
