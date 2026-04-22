import SwiftUI
import shared
import BackgroundTasks

@main
struct iOSApp: App {
    init() {
        let supabaseUrl   = Bundle.main.infoDictionary?["SUPABASE_URL"]   as? String ?? ""
        let supabaseKey   = Bundle.main.infoDictionary?["SUPABASE_ANON_KEY"] as? String ?? ""
        let syncTaskId    = "com.mindful.sports.sync"

        // Boot Koin — sport selection is handled at runtime via UserPreferences.
        KoinHelperKt.doInitKoin(
            supabaseUrl: supabaseUrl,
            supabaseAnonKey: supabaseKey
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
