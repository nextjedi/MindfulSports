package com.ashutosh.mindfultennis.di

import com.ashutosh.mindfultennis.data.local.db.getDatabaseBuilder
import com.ashutosh.mindfultennis.data.local.datastore.createDataStore
import com.ashutosh.mindfultennis.data.notification.IosNotificationScheduler
import com.ashutosh.mindfultennis.data.notification.NotificationScheduler
import com.ashutosh.mindfultennis.data.sync.BackgroundSyncScheduler
import com.ashutosh.mindfultennis.data.sync.IosSyncScheduler
import org.koin.dsl.module

actual val platformModule = module {
    single { getDatabaseBuilder() }
    single { createDataStore() }
    single<BackgroundSyncScheduler> { IosSyncScheduler() }
    single<NotificationScheduler> { IosNotificationScheduler() }
}
