package com.ashutosh.mindfultennis

import android.app.Application
import com.ashutosh.mindfultennis.di.AppConfig
import com.ashutosh.mindfultennis.di.commonModule
import com.ashutosh.mindfultennis.di.platformModule
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class MindfulTennisApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Configure RevenueCat before Koin so SubscriptionRepositoryImpl can safely
        // access Purchases.sharedInstance when it is first injected.
        if (BuildConfig.REVENUECAT_KEY.isNotEmpty()) {
            Purchases.configure(
                PurchasesConfiguration.Builder(apiKey = BuildConfig.REVENUECAT_KEY).build()
            )
        }

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
                            deepLinkScheme = "com.ashutosh.mindfultennis",
                        )
                    }
                }
            )
        }
    }
}
