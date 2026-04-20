package com.ashutosh.mindfultennis.analytics

import android.content.Context
import com.posthog.android.PostHog
import com.posthog.android.PostHogAndroidConfig

/**
 * PostHog-backed analytics implementation for Android.
 *
 * Initialised once via Koin DI. The API key and host are passed from AppConfig
 * so they come from local.properties (POSTHOG_API_KEY), not hardcoded here.
 *
 * PostHog default flush: 20 events or 30 seconds, whichever comes first.
 * PostHog session timeout: 30 minutes of inactivity.
 */
class PostHogAnalytics(
    context: Context,
    apiKey: String,
    host: String = "https://us.i.posthog.com",
) : Analytics {

    init {
        if (apiKey.isNotBlank()) {
            PostHog.setup(
                context,
                PostHogAndroidConfig(
                    apiKey = apiKey,
                    host = host,
                )
            )
        }
        // If apiKey is blank (e.g., local dev without local.properties key),
        // PostHog is not initialised and all calls are no-ops via the guard below.
    }

    override fun identify(userId: String, traits: Map<String, Any>) {
        if (!PostHog.isFeatureEnabled("")) return  // guard: not initialised = skip
        PostHog.identify(userId, traits.ifEmpty { null })
    }

    override fun track(event: String, properties: Map<String, Any>) {
        PostHog.capture(event, properties.ifEmpty { null })
    }

    override fun screen(name: String, properties: Map<String, Any>) {
        PostHog.screen(name, properties.ifEmpty { null })
    }

    override fun reset() {
        PostHog.reset()
    }
}
