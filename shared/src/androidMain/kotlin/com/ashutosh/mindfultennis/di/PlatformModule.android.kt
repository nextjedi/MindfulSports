package com.ashutosh.mindfultennis.di

import com.ashutosh.mindfultennis.analytics.Analytics
import com.ashutosh.mindfultennis.analytics.PostHogAnalytics
import com.ashutosh.mindfultennis.data.local.db.getDatabaseBuilder
import com.ashutosh.mindfultennis.data.local.datastore.createDataStore
import com.ashutosh.mindfultennis.data.sync.AndroidSyncScheduler
import com.ashutosh.mindfultennis.data.sync.BackgroundSyncScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single { getDatabaseBuilder(get()) }
    single { createDataStore(get()) }
    single<BackgroundSyncScheduler> { AndroidSyncScheduler(get()) }
    single<Analytics> {
        val config = get<AppConfig>()
        PostHogAnalytics(context = androidContext(), apiKey = config.postHogApiKey)
    }
}
