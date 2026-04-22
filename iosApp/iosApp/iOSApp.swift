import SwiftUI
import shared
import BackgroundTasks

@main
struct iOSApp: App {
    init() {
        let supabaseUrl   = Bundle.main.infoDictionary?["SUPABASE_URL"]   as? String ?? ""
        let supabaseKey   = Bundle.main.infoDictionary?["SUPABASE_ANON_KEY"] as? String ?? ""
        let sportId       = Bundle.main.infoDictionary?["SPORT_ID"]       as? String ?? "tennis"
        let bgTaskId      = Bundle.main.infoDictionary?["BGTaskSchedulerPermittedIdentifiers"] as? [String]
        let syncTaskId    = bgTaskId?.first ?? "com.mindful.tennis.sync"

        // Boot Koin with the sport-specific config.
        // SportRegistry.fromId(sportId) selects the correct SportConfig.
        KoinHelperKt.doInitKoin(
            supabaseUrl: supabaseUrl,
            supabaseAnonKey: supabaseKey,
            sportId: sportId
        )

        // Register the nightly sync background task handler.
        // Registration must happen before the first runloop cycle completes.
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: syncTaskId,
            using: nil
        ) { task in
            guard let refreshTask = task as? BGAppRefreshTask else {
                task.setTaskCompleted(success: false)
                return
            }

            // Reschedule next run before doing any work so the chain continues
            // even if this run is cut short.
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
                // Hand OAuth callback URLs back to Supabase
                // so it can exchange tokens and create a session.
                .onOpenURL { url in
                    DeepLinkHandler.shared.handle(url: url.absoluteString)
                }
        }
    }
}
