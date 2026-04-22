package com.ashutosh.mindfultennis

import com.ashutosh.mindfultennis.di.AppConfig
import com.ashutosh.mindfultennis.di.commonModule
import com.ashutosh.mindfultennis.di.platformModule
import org.koin.core.context.startKoin
import org.koin.dsl.module

fun initKoin(supabaseUrl: String, supabaseAnonKey: String) {
    startKoin {
        modules(
            commonModule,
            platformModule,
            module {
                single {
                    AppConfig(
                        supabaseUrl = supabaseUrl,
                        supabaseAnonKey = supabaseAnonKey,
                    )
                }
            }
        )
    }
}
