package com.ashutosh.mindfultennis

import android.app.Application
import com.ashutosh.mindfultennis.di.AppConfig
import com.ashutosh.mindfultennis.di.commonModule
import com.ashutosh.mindfultennis.di.platformModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class MindfulTennisApp : Application() {
    override fun onCreate() {
        super.onCreate()
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
                        )
                    }
                }
            )
        }
    }
}
