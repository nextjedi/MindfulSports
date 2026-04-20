package com.ashutosh.mindfultennis.observability

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryLevel
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb

/**
 * Kermit LogWriter that forwards logs to Sentry as breadcrumbs.
 *
 * Usage:
 * - INFO+  → breadcrumb (trail of events leading up to a crash)
 * - ERROR+ → breadcrumb AND Sentry.captureException (appears in Issues dashboard)
 *
 * In release builds this is the only writer; debug builds use LogcatWriter only.
 */
class SentryBreadcrumbWriter(
    private val minSeverity: Severity = Severity.Info,
) : LogWriter() {

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        if (severity < minSeverity) return

        val level = severity.toSentryLevel()

        Sentry.addBreadcrumb(Breadcrumb().apply {
            this.message = message
            this.level = level
            this.category = tag
        })

        // Surface ERROR and above as handled exceptions in the Sentry Issues dashboard
        if (severity >= Severity.Error && throwable != null) {
            Sentry.captureException(throwable)
        }
    }

    private fun Severity.toSentryLevel(): SentryLevel = when (this) {
        Severity.Verbose, Severity.Debug -> SentryLevel.DEBUG
        Severity.Info -> SentryLevel.INFO
        Severity.Warn -> SentryLevel.WARNING
        Severity.Error -> SentryLevel.ERROR
        Severity.Assert -> SentryLevel.FATAL
    }
}
