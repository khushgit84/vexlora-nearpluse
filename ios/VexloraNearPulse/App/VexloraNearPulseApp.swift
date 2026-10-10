import SwiftUI

@main
struct VexloraNearPulseApp: App {
    @StateObject private var bleManager = BleManager.shared

    var body: some Scene {
        WindowGroup {
            ContentView()
                .preferredColorScheme(.dark)
                .environmentObject(bleManager)
        }
    }
}
