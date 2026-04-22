package com.ashutosh.mindfultennis.di

import com.ashutosh.mindfultennis.sport.SportConfig
import com.ashutosh.mindfultennis.sport.SportRegistry

data class AppConfig(
    val supabaseUrl: String,
    val supabaseAnonKey: String,
    /** Sport-specific runtime config: aspects, scoring rules, display name. */
    val sportConfig: SportConfig = SportRegistry.tennis,
    /** OAuth callback scheme — defaults to the sport's deep-link scheme. */
    val deepLinkScheme: String = sportConfig.deepLinkScheme,
)
