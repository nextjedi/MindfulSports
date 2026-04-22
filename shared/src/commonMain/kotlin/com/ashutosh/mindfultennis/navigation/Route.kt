package com.ashutosh.mindfultennis.navigation

/**
 * Sealed class defining all navigation routes in the app.
 */
sealed class Route(val route: String) {
    data object Login : Route("login")
    data object SportSelection : Route("sport_selection")
    data object Home : Route("home")
    data object StartSession : Route("start_session")

    data class EndSession(val sessionId: String) : Route("end_session/$sessionId") {
        companion object {
            const val ROUTE_PATTERN = "end_session/{sessionId}"
            const val ARG_SESSION_ID = "sessionId"
        }
    }

    data object Settings : Route("settings")
    data object SessionsList : Route("sessions_list")

    data class SessionDetail(val sessionId: String) : Route("session_detail/$sessionId") {
        companion object {
            const val ROUTE_PATTERN = "session_detail/{sessionId}"
            const val ARG_SESSION_ID = "sessionId"
        }
    }
}
