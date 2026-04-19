import SwiftUI
import shared
import BackgroundTasks

@main
struct iOSApp: App {
    init() {
        let supabaseUrl = Bundle.main.infoDictionary?["SUPABASE_URL"] as? String ?? ""
        let supabaseAnonKey = Bundle.main.infoDictionary?["SUPABASE_ANON_KEY"] as? String ?? ""

        // Configure RevenueCat before Koin so SubscriptionRepositoryImpl can safely
        // access Purchases.sharedInstance when it is first injected.
        let revenueCatKey = Bundle.main.infoDictionary?["REVENUECAT_KEY"] as? String ?? ""
        if !revenueCatKey.isEmpty {
            PurchasesHelperKt.initPurchases(apiKey: revenueCatKey)
        }

        KoinHelperKt.doInitKoin(supabaseUrl: supabaseUrl, supabaseAnonKey: supabaseAnonKey)

        // Register the nightly sync background task handler.
        // Registration must happen before the first runloop cycle completes.
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: "com.nextjedi.mindful-tennis.sync",
            using: nil
        ) { task in
            guard let refreshTask = task as? BGAppRefreshTask else {
                task.setTaskCompleted(success: false)
                return
            }

            // Reschedule next run at 1:30 AM before doing any work,
            // so the chain continues even if this run is cut short.
            IosSyncScheduler().schedulePeriodic()

            BackgroundSyncRunner.shared.runSync { success in
                refreshTask.setTaskCompleted(success: success.boolValue)
            }

            refreshTask.expirationHandler = {
                refreshTask.setTaskCompleted(success: false)
            }
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                // Hand OAuth callback URLs (Google, Apple) back to Supabase
                // so it can exchange tokens and create a session.
                .onOpenURL { url in
                    DeepLinkHandler.shared.handle(url: url.absoluteString)
                }
        }
    }
}
