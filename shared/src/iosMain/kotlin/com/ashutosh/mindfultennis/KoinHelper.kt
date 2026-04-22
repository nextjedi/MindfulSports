package com.ashutosh.mindfultennis

import com.ashutosh.mindfultennis.di.AppConfig
import com.ashutosh.mindfultennis.di.commonModule
import com.ashutosh.mindfultennis.di.platformModule
import com.ashutosh.mindfultennis.sport.SportRegistry
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun initKoin(supabaseUrl: String, supabaseAnonKey: String, sportId: String = "tennis") {
    val sportConfig = SportRegistry.fromId(sportId)
    startKoin {
        modules(
            commonModule,
            platformModule,
            module {
                single {
                    AppConfig(
                        supabaseUrl = supabaseUrl,
                        supabaseAnonKey = supabaseAnonKey,
                        sportConfig = sportConfig,
                    )
                }
            }
        )
    }
}
