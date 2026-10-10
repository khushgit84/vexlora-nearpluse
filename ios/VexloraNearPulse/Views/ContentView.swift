import SwiftUI

public struct ContentView: View {
    @ObservedObject var bleManager = BleManager.shared
    @State private var selectedTab: Int = 0

    public init() {}

    public var body: some View {
        ZStack(alignment: .bottom) {
            // Main Content Views
            TabView(selection: $selectedTab) {
                RadarView()
                    .tag(0)

                ChatView()
                    .tag(1)

                SosView()
                    .tag(2)

                SettingsView()
                    .tag(3)
            }
            .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never))

            // Cyberpunk Bottom Navigation Bar
            HStack(spacing: 0) {
                tabButton(title: "Radar", icon: "dot.radiowaves.left.and.right", index: 0)
                tabButton(title: "Chat", icon: "bubble.left.and.bubble.right.fill", index: 1)
                tabButton(title: "SOS", icon: "exclamationmark.shield.fill", index: 2, isSos: true)
                tabButton(title: "Settings", icon: "slider.horizontal.3", index: 3)
            }
            .padding(.horizontal, 16)
            .padding(.top, 10)
            .padding(.bottom, 24)
            .background(
                Color(red: 0.03, green: 0.05, blue: 0.1)
                    .overlay(
                        Rectangle()
                            .frame(height: 1)
                            .foregroundColor(Color(red: 0.0, green: 0.9, blue: 1.0).opacity(0.2)),
                        alignment: .top
                    )
            )
        }
        .ignoresSafeArea(.keyboard, edges: .bottom)
        .onAppear {
            EmergencyAlertManager.shared.requestPermissions()
        }
    }

    @ViewBuilder
    private func tabButton(title: String, icon: String, index: Int, isSos: Bool = false) -> some View {
        Button(action: {
            selectedTab = index
        }) {
            VStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.system(size: 20, weight: selectedTab == index ? .bold : .regular))
                    .foregroundColor(
                        isSos
                            ? Color(red: 1.0, green: 0.1, blue: 0.25)
                            : (selectedTab == index ? Color(red: 0.0, green: 0.9, blue: 1.0) : Color.gray)
                    )

                Text(title)
                    .font(.system(size: 10, weight: selectedTab == index ? .bold : .medium, design: .monospaced))
                    .foregroundColor(
                        isSos
                            ? Color(red: 1.0, green: 0.1, blue: 0.25)
                            : (selectedTab == index ? Color(red: 0.0, green: 0.9, blue: 1.0) : Color.gray)
                    )
            }
            .frame(maxWidth: .infinity)
        }
    }
}
