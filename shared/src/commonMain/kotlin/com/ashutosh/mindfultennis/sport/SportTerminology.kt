package com.ashutosh.mindfultennis.sport

import com.ashutosh.mindfultennis.domain.model.Aspect

/**
 * Sport-specific display labels for the 8 fixed Aspect slots.
 *
 * The Aspect enum entries are fixed (no DB schema change required).
 * Only the UI label differs per sport — everything else (DB storage, sync,
 * analytics grouping) continues to use the enum name.
 *
 * Slots:
 *   FOREHAND  → [forehand]   (shared across all sports)
 *   BACKHAND  → [backhand]   (shared across all sports)
 *   SERVE     → [serve]      (shared across most sports)
 *   RETURN    → [returnGame] (shared across most sports)
 *   VOLLEY    → [skill5]     (sport-unique: Volley / Smash / Dink / Kill Shot / etc.)
 *   SLICE     → [skill6]     (sport-unique: Slice / Clear / Boast / Ceiling Ball / etc.)
 *   MOVEMENT  → [movement]   (shared — renamed Footwork or Sand Movement for some sports)
 *   MINDSET   → [mindset]    (shared across all sports)
 */
data class SportTerminology(
    val forehand: String = "Forehand",
    val backhand: String = "Backhand",
    val serve: String = "Serve",
    val returnGame: String = "Return",
    val skill5: String,
    val skill6: String,
    val movement: String = "Movement",
    val mindset: String = "Mindset",
) {
    fun labelFor(aspect: Aspect): String = when (aspect) {
        Aspect.FOREHAND -> forehand
        Aspect.BACKHAND -> backhand
        Aspect.SERVE    -> serve
        Aspect.RETURN   -> returnGame
        Aspect.VOLLEY   -> skill5
        Aspect.SLICE    -> skill6
        Aspect.MOVEMENT -> movement
        Aspect.MINDSET  -> mindset
    }
}
