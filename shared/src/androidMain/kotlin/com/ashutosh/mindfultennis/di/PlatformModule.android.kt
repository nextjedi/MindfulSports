package com.ashutosh.mindfultennis.di

import com.ashutosh.mindfultennis.data.local.db.getDatabaseBuilder
import com.ashutosh.mindfultennis.data.local.datastore.createDataStore
import com.ashutosh.mindfultennis.data.notification.AndroidNotificationScheduler
import com.ashutosh.mindfultennis.data.notification.NotificationScheduler
import com.ashutosh.mindfultennis.data.sync.AndroidSyncScheduler
import com.ashutosh.mindfultennis.data.sync.BackgroundSyncScheduler
import org.koin.dsl.module

actual val platformModule = module {
    single { getDatabaseBuilder(get()) }
    single { createDataStore(get()) }
    single<BackgroundSyncScheduler> { AndroidSyncScheduler(get()) }
    single<NotificationScheduler> { AndroidNotificationScheduler(get()) }
}
