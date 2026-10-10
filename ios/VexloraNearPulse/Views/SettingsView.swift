import SwiftUI

struct SettingsView: View {
    @ObservedObject var bleManager = BleManager.shared
    @State private var pseudonymText: String = BleManager.shared.localPseudonym
    @State private var rssiThreshold: Double = -85.0
    @State private var isTestAlarmPlaying: Bool = false

    var body: some View {
        ZStack {
            Color(red: 0.02, green: 0.03, blue: 0.07)
                .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 20) {
                    // Header
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("NODE CONFIGURATION")
                                .font(.system(size: 11, weight: .bold, design: .monospaced))
                                .foregroundColor(Color(red: 0.0, green: 0.9, blue: 1.0))
                            Text("Mesh Settings")
                                .font(.system(size: 24, weight: .black))
                                .foregroundColor(.white)
                        }
                        Spacer()
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 20)

                    // Node Identity Card
                    VStack(alignment: .leading, spacing: 12) {
                        Text("LOCAL CALLSIGN / PSEUDONYM")
                            .font(.system(size: 11, weight: .bold, design: .monospaced))
                            .foregroundColor(Color(red: 0.0, green: 0.9, blue: 1.0))

                        HStack {
                            TextField("Enter Callsign", text: $pseudonymText)
                                .font(.system(size: 16, weight: .bold, design: .monospaced))
                                .foregroundColor(.white)
                                .padding(12)
                                .background(Color.white.opacity(0.06))
                                .cornerRadius(10)

                            Button(action: {
                                bleManager.localPseudonym = pseudonymText
                                bleManager.stopAdvertising()
                                bleManager.startAdvertising()
                            }) {
                                Text("Update")
                                    .font(.system(size: 13, weight: .bold))
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 12)
                                    .background(Color(red: 0.0, green: 0.9, blue: 1.0))
                                    .foregroundColor(Color(red: 0.02, green: 0.03, blue: 0.07))
                                    .cornerRadius(10)
                            }
                        }

                        Text("Broadcast over Bluetooth Low Energy advertisement packets.")
                            .font(.system(size: 11))
                            .foregroundColor(.gray)
                    }
                    .padding(20)
                    .background(Color.white.opacity(0.04))
                    .cornerRadius(16)
                    .padding(.horizontal, 20)

                    // Proximity Sensitivity Slider
                    VStack(alignment: .leading, spacing: 14) {
                        HStack {
                            Text("RADAR PROXIMITY CUTOFF")
                                .font(.system(size: 11, weight: .bold, design: .monospaced))
                                .foregroundColor(Color(red: 0.0, green: 0.9, blue: 1.0))
                            Spacer()
                            Text("\(Int(rssiThreshold)) dBm")
                                .font(.system(size: 13, weight: .bold, design: .monospaced))
                                .foregroundColor(.white)
                        }

                        Slider(value: $rssiThreshold, in: -95...(-45), step: 1.0)
                            .accentColor(Color(red: 0.0, green: 0.9, blue: 1.0))

                        Text("Filters out faint signals beyond physical radio proximity.")
                            .font(.system(size: 11))
                            .foregroundColor(.gray)
                    }
                    .padding(20)
                    .background(Color.white.opacity(0.04))
                    .cornerRadius(16)
                    .padding(.horizontal, 20)

                    // Emergency Audio Test
                    VStack(alignment: .leading, spacing: 14) {
                        Text("EMERGENCY SIREN AUDIO TEST")
                            .font(.system(size: 11, weight: .bold, design: .monospaced))
                            .foregroundColor(Color(red: 1.0, green: 0.1, blue: 0.25))

                        Button(action: {
                            if isTestAlarmPlaying {
                                EmergencyAlertManager.shared.stopEmergencySiren()
                                isTestAlarmPlaying = false
                            } else {
                                EmergencyAlertManager.shared.startEmergencySiren()
                                isTestAlarmPlaying = true
                            }
                        }) {
                            HStack {
                                Image(systemName: isTestAlarmPlaying ? "speaker.slash.fill" : "speaker.wave.3.fill")
                                Text(isTestAlarmPlaying ? "Stop Test Siren" : "Test High-Gain Siren")
                                    .fontWeight(.bold)
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(isTestAlarmPlaying ? Color.red : Color.white.opacity(0.08))
                            .foregroundColor(.white)
                            .cornerRadius(10)
                        }
                    }
                    .padding(20)
                    .background(Color.white.opacity(0.04))
                    .cornerRadius(16)
                    .padding(.horizontal, 20)

                    // Cross-Platform Verification Details
                    VStack(alignment: .leading, spacing: 10) {
                        Text("CROSS-PLATFORM PROTOCOL COMPATIBILITY")
                            .font(.system(size: 11, weight: .bold, design: .monospaced))
                            .foregroundColor(Color(red: 0.0, green: 0.9, blue: 1.0))

                        HStack {
                            Text("Service UUID:")
                                .font(.system(size: 11, design: .monospaced))
                                .foregroundColor(.gray)
                            Spacer()
                            Text("7B14D2E0-252A-4DF4...")
                                .font(.system(size: 11, weight: .bold, design: .monospaced))
                                .foregroundColor(.white)
                        }

                        HStack {
                            Text("Interoperability:")
                                .font(.system(size: 11, design: .monospaced))
                                .foregroundColor(.gray)
                            Spacer()
                            Text("Android 14/15/16 + iOS 17/18")
                                .font(.system(size: 11, weight: .bold, design: .monospaced))
                                .foregroundColor(.green)
                        }

                        HStack {
                            Text("Encryption & Security:")
                                .font(.system(size: 11, design: .monospaced))
                                .foregroundColor(.gray)
                            Spacer()
                            Text("Air-Gapped (Zero Cloud)")
                                .font(.system(size: 11, weight: .bold, design: .monospaced))
                                .foregroundColor(Color(red: 0.0, green: 0.9, blue: 1.0))
                        }
                    }
                    .padding(20)
                    .background(Color.white.opacity(0.04))
                    .cornerRadius(16)
                    .padding(.horizontal, 20)

                    Spacer(minLength: 40)
                }
            }
        }
    }
}
