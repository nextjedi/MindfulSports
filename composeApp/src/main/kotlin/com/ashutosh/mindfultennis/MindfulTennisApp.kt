package com.ashutosh.mindfultennis

import android.app.Application
import co.touchlab.kermit.LogcatWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.StaticConfig
import com.ashutosh.mindfultennis.di.AppConfig
import com.ashutosh.mindfultennis.di.commonModule
import com.ashutosh.mindfultennis.di.platformModule
import com.ashutosh.mindfultennis.observability.SentryBreadcrumbWriter
import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryOptions
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class MindfulTennisApp : Application() {

    override fun onCreate() {
        super.onCreate()
        setupKermit()
        setupSentry()
        setupKoin()
    }

    // ── Logging ───────────────────────────────────────────────────────────

    private fun setupKermit() {
        val writers = if (BuildConfig.DEBUG) {
            // Debug: all log levels to Logcat only — nothing leaves the device
            listOf(LogcatWriter())
        } else {
            // Release: Info+ forwarded to Sentry as breadcrumbs;
            //          Error+ also surfaces as a handled exception in Issues.
            listOf(SentryBreadcrumbWriter(minSeverity = Severity.Info))
        }
        Logger.config = StaticConfig(
            minSeverity = if (BuildConfig.DEBUG) Severity.Debug else Severity.Info,
            logWriterList = writers,
        )
    }

    // ── Crash Reporting ───────────────────────────────────────────────────

    private fun setupSentry() {
        val dsn = BuildConfig.SENTRY_DSN
        if (dsn.isBlank()) return  // not configured locally — skip init

        Sentry.init { options: SentryOptions ->
            options.dsn = dsn
            options.environment = if (BuildConfig.DEBUG) "debug" else "production"
            // Trace 20 % of sessions in production for performance monitoring.
            // 0.0 in debug keeps the Sentry dashboard clean during development.
            options.tracesSampleRate = if (BuildConfig.DEBUG) null else 0.2
        }
        // ANR detection is configured via the Sentry Android Gradle plugin or
        // by setting options.isAnrEnabled = true once the sentry-android artifact
        // is directly available (transitive via sentry-kotlin-multiplatform on Android).
    }

    // ── Dependency Injection ──────────────────────────────────────────────

    private fun setupKoin() {
        startKoin {
            androidContext(this@MindfulTennisApp)
            modules(
                commonModule,
                platformModule,
                module {
                    single {
                        AppConfig(
                            supabaseUrl = BuildConfig.SUPABASE_URL,
                            supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY,
                            deepLinkScheme = "com.nextjedi.mindful-tennis",
                            sentryDsn = BuildConfig.SENTRY_DSN,
                            postHogApiKey = BuildConfig.POSTHOG_API_KEY,
                        )
                    }
                }
            )
        }
    }
}
