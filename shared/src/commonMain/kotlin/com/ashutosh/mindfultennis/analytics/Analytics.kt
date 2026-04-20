package com.ashutosh.mindfultennis.analytics

/**
 * Platform-neutral analytics interface.
 *
 * Android implementation uses PostHog. iOS implementation is a stub until
 * PostHog iOS SDK is wired via SPM. Registered in each platform's DI module.
 *
 * Event naming convention: Object-Action, Title Case, past tense.
 * Property naming convention: snake_case.
 *
 * Usage:
 *   analytics.track(Events.SESSION_STARTED, mapOf("sport" to "tennis"))
 */
interface Analytics {
    /** Associate all subsequent events with this user ID and optional traits. */
    fun identify(userId: String, traits: Map<String, Any> = emptyMap())

    /** Track a discrete user action or system event. */
    fun track(event: String, properties: Map<String, Any> = emptyMap())

    /** Track a screen view. */
    fun screen(name: String, properties: Map<String, Any> = emptyMap())

    /** Clear the current user identity (call on logout). */
    fun reset()
}

/** Canonical event name constants — use these everywhere to avoid typos. */
object Events {
    const val APP_OPENED            = "App Opened"
    const val SESSION_STARTED       = "Session Started"       // tennis session, not app session
    const val SESSION_ENDED         = "Session Ended"
    const val SESSION_CANCELLED     = "Session Cancelled"
    const val SCREEN_VIEWED         = "Screen Viewed"
    const val USER_SIGNED_UP        = "User Signed Up"
    const val USER_LOGGED_IN        = "User Logged In"
    const val USER_LOGGED_OUT       = "User Logged Out"
    const val SUBSCRIPTION_STARTED  = "Subscription Started"
    const val SUBSCRIPTION_CANCELLED = "Subscription Cancelled"
    const val FEATURE_ACTIVATED     = "Feature Activated"     // first meaningful use
    const val SYNC_COMPLETED        = "Sync Completed"
    const val SYNC_FAILED           = "Sync Failed"
}

/** Canonical property key constants — snake_case for consistency. */
object Props {
    const val LAUNCH_TYPE           = "launch_type"           // cold | warm | hot
    const val AUTH_METHOD           = "auth_method"           // google | apple | email
    const val PLATFORM              = "platform"              // android | ios
    const val APP_VERSION           = "app_version"
    const val SUBSCRIPTION_PLAN     = "subscription_plan"     // monthly | annual
    const val IS_TRIAL              = "is_trial"
    const val DURATION_SECONDS      = "duration_seconds"
    const val RECORDS_SYNCED        = "records_synced"
    const val DURATION_MS           = "duration_ms"
    const val ERROR_CODE            = "error_code"
    const val FEATURE_NAME          = "feature_name"
    const val SCREEN_NAME           = "screen_name"
    const val PREVIOUS_SCREEN       = "previous_screen"
}
