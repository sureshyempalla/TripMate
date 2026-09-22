import SwiftUI
import shared

@main
struct TripMateApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    init() {
        KoinBootstrapKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            TripListView()
        }
    }
}
