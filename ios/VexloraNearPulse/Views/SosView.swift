import SwiftUI

struct SosView: View {
    @ObservedObject var bleManager = BleManager.shared
    @State private var isHolding: Bool = false
    @State private var holdProgress: Double = 0.0
    @State private var holdTimer: Timer? = nil

    var body: some View {
        ZStack {
            Color(red: 0.02, green: 0.03, blue: 0.07)
                .ignoresSafeArea()

            VStack(spacing: 24) {
                // Header
                VStack(spacing: 4) {
                    Text("TACTICAL DISTRESS BEACON")
                        .font(.system(size: 11, weight: .bold, design: .monospaced))
                        .foregroundColor(Color(red: 1.0, green: 0.1, blue: 0.25))

                    Text("Emergency SOS")
                        .font(.system(size: 26, weight: .black))
                        .foregroundColor(.white)

                    Text("Fires maximum gain 2.4 GHz RF beacon to all phones within range.")
                        .font(.system(size: 12))
                        .foregroundColor(.gray)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 30)
                }
                .padding(.top, 20)

                Spacer()

                if bleManager.isSosBroadcasting || bleManager.activeSosAlert != nil {
                    // Active SOS Emergency Screen with Stop Siren Action
                    VStack(spacing: 20) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .font(.system(size: 64))
                            .foregroundColor(Color(red: 1.0, green: 0.1, blue: 0.25))
                            .shadow(color: Color.red, radius: 20)

                        Text("DISTRESS BEACON ACTIVE")
                            .font(.system(size: 20, weight: .black, design: .monospaced))
                            .foregroundColor(.white)

                        Text("Broadcasting emergency beacon over BLE mesh. Siren alarm is sounding.")
                            .font(.system(size: 13))
                            .foregroundColor(.gray)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 30)

                        // Problem Solved / Stop Sound Button
                        Button(action: {
                            bleManager.resolveSosProblem()
                        }) {
                            HStack(spacing: 10) {
                                Image(systemName: "speaker.slash.fill")
                                Text("PROBLEM SOLVED (STOP SIREN)")
                                    .fontWeight(.heavy)
                            }
                            .font(.system(size: 15, design: .monospaced))
                            .foregroundColor(Color(red: 0.02, green: 0.03, blue: 0.07))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(Color(red: 0.0, green: 0.9, blue: 1.0))
                            .cornerRadius(14)
                            .shadow(color: Color(red: 0.0, green: 0.9, blue: 1.0).opacity(0.4), radius: 15)
                        }
                        .padding(.horizontal, 30)
                    }
                    .padding(24)
                    .background(Color(red: 1.0, green: 0.1, blue: 0.25).opacity(0.12))
                    .overlay(
                        RoundedRectangle(cornerRadius: 20)
                            .stroke(Color(red: 1.0, green: 0.1, blue: 0.25).opacity(0.4), lineWidth: 1.5)
                    )
                    .cornerRadius(20)
                    .padding(.horizontal, 20)
                } else {
                    // Hold 3 Seconds to Activate SOS
                    VStack(spacing: 24) {
                        ZStack {
                            // Progress Outer Ring
                            Circle()
                                .stroke(Color.white.opacity(0.1), lineWidth: 8)
                                .frame(width: 200, height: 200)

                            Circle()
                                .trim(from: 0.0, to: CGFloat(holdProgress))
                                .stroke(Color(red: 1.0, green: 0.1, blue: 0.25), style: StrokeStyle(lineWidth: 8, lineCap: .round))
                                .rotationEffect(.degrees(-90))
                                .frame(width: 200, height: 200)

                            // Main SOS Push Button
                            Circle()
                                .fill(
                                    LinearGradient(
                                        colors: [Color(red: 0.85, green: 0.05, blue: 0.18), Color(red: 0.5, green: 0.0, blue: 0.1)],
                                        startPoint: .topLeading,
                                        endPoint: .bottomTrailing
                                    )
                                )
                                .frame(width: 170, height: 170)
                                .shadow(color: Color(red: 1.0, green: 0.1, blue: 0.25).opacity(0.5), radius: 20)

                            VStack(spacing: 4) {
                                Text("HOLD 3S")
                                    .font(.system(size: 11, weight: .bold, design: .monospaced))
                                    .foregroundColor(Color.white.opacity(0.8))
                                Text("SOS")
                                    .font(.system(size: 38, weight: .black))
                                    .foregroundColor(.white)
                            }
                        }
                        .gesture(
                            DragGesture(minimumDistance: 0)
                                .onChanged { _ in
                                    if !isHolding {
                                        startHoldCountdown()
                                    }
                                }
                                .onEnded { _ in
                                    cancelHoldCountdown()
                                }
                        )

                        Text("Press and hold for 3 seconds to trigger distress alarm")
                            .font(.system(size: 12, design: .monospaced))
                            .foregroundColor(.gray)
                    }
                }

                Spacer()

                // Security Policy Info
                HStack(spacing: 8) {
                    Image(systemName: "lock.shield.fill")
                        .foregroundColor(Color(red: 0.0, green: 0.9, blue: 1.0))
                    Text("100% AIR-GAPPED • ZERO CELLULAR REQUIRED")
                        .font(.system(size: 10, weight: .bold, design: .monospaced))
                        .foregroundColor(.gray)
                }
                .padding(.bottom, 20)
            }
        }
    }

    private func startHoldCountdown() {
        isHolding = true
        holdProgress = 0.0
        holdTimer?.invalidate()

        holdTimer = Timer.scheduledTimer(withTimeInterval: 0.05, repeats: true) { _ in
            holdProgress += 0.05 / 3.0 // 3 seconds total
            if holdProgress >= 1.0 {
                cancelHoldCountdown()
                bleManager.triggerSosBeacon()
            }
        }
    }

    private func cancelHoldCountdown() {
        isHolding = false
        holdProgress = 0.0
        holdTimer?.invalidate()
        holdTimer = nil
    }
}
