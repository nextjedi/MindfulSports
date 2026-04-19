package com.ashutosh.mindfultennis

import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration

/**
 * Called from iOSApp.swift before Koin is initialized.
 * Configures the RevenueCat SDK with the iOS API key.
 */
fun initPurchases(apiKey: String) {
    Purchases.configure(
        PurchasesConfiguration.Builder(apiKey = apiKey).build()
    )
}
