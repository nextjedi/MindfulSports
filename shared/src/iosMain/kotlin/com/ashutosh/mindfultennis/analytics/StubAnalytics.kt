package com.ashutosh.mindfultennis.analytics

import co.touchlab.kermit.Logger

/**
 * No-op Analytics implementation for iOS.
 *
 * PostHog iOS SDK requires SPM (Swift Package Manager) setup in Xcode and cannot
 * be bundled through the Kotlin/Gradle dependency graph. Until that is wired:
 * - All analytics calls are logged at DEBUG level so you can verify events fire
 * - No data is sent to PostHog
 *
 * To activate PostHog on iOS:
 * 1. Add PostHog iOS SDK via SPM in Xcode
 * 2. Call PostHogSDK.shared.setup() in iOSApp.swift
 * 3. Replace this stub with a real implementation that calls PostHogSDK via @ObjCName
 */
class StubAnalytics : Analytics {

    private val log = Logger.withTag("StubAnalytics")

    override fun identify(userId: String, traits: Map<String, Any>) {
        log.d { "identify(userId=$userId, traits=$traits)" }
    }

    override fun track(event: String, properties: Map<String, Any>) {
        log.d { "track(event=$event, properties=$properties)" }
    }

    override fun screen(name: String, properties: Map<String, Any>) {
        log.d { "screen(name=$name)" }
    }

    override fun reset() {
        log.d { "reset()" }
    }
}
