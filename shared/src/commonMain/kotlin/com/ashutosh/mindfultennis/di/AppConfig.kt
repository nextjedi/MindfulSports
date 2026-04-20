package com.ashutosh.mindfultennis.di

data class AppConfig(
    val supabaseUrl: String,
    val supabaseAnonKey: String,
    val deepLinkScheme: String = "com.ashutosh.mindfultennis",
    val sentryDsn: String = "",
    val postHogApiKey: String = "",
)
