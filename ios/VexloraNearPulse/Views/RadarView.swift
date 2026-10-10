import SwiftUI

struct RadarView: View {
    @ObservedObject var bleManager = BleManager.shared
    @State private var sweepAngle: Double = 0
    @State private var selectedPeer: PeerNode? = nil

    private let timer = Timer.publish(every: 0.03, on: .main, in: .common).autoconnect()

    var body: some View {
        ZStack {
            Color(red: 0.02, green: 0.03, blue: 0.07)
                .ignoresSafeArea()

            VStack(spacing: 16) {
                // Header Bar
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("RF SPECTRUM RADAR")
                            .font(.system(size: 11, weight: .bold, design: .monospaced))
                            .foregroundColor(Color(red: 0.0, green: 0.9, blue: 1.0))
                        Text("2.4 GHz Proximity Grid")
                            .font(.system(size: 18, weight: .heavy, design: .default))
                            .foregroundColor(.white)
                    }

                    Spacer()

                    HStack(spacing: 6) {
                        Circle()
                            .fill(bleManager.isScanning ? Color.green : Color.orange)
                            .frame(width: 8, height: 8)
                        Text(bleManager.isScanning ? "SCANNING" : "PAUSED")
                            .font(.system(size: 10, weight: .bold, design: .monospaced))
                            .foregroundColor(.white)
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(Color.white.opacity(0.06))
                    .cornerRadius(20)
                }
                .padding(.horizontal, 20)
                .padding(.top, 10)

                // Phone Filter Tag
                HStack {
                    Image(systemName: "iphone.radiowaves.left.and.right")
                        .foregroundColor(Color(red: 0.0, green: 0.9, blue: 1.0))
                    Text("SMART PHONE-ONLY FILTER ACTIVE (TVs & accessories hidden)")
                        .font(.system(size: 10, weight: .semibold, design: .monospaced))
                        .foregroundColor(Color.white.opacity(0.7))
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 4)
                .background(Color(red: 0.0, green: 0.9, blue: 1.0).opacity(0.1))
                .cornerRadius(6)

                // Interactive Radar Canvas
                GeometryReader { geo in
                    let size = min(geo.size.width, geo.size.height)
                    let center = CGPoint(x: geo.size.width / 2, y: geo.size.height / 2)
                    let radius = (size / 2) - 20

                    ZStack {
                        // Concentric Rings
                        ForEach([0.25, 0.50, 0.75, 1.0], id: \.self) { ringRatio in
                            Circle()
                                .stroke(Color(red: 0.0, green: 0.9, blue: 1.0).opacity(0.18), lineWidth: 1)
                                .frame(width: radius * 2 * ringRatio, height: radius * 2 * ringRatio)
                        }

                        // Crosshair Axes
                        Path { path in
                            path.move(to: CGPoint(x: center.x - radius, y: center.y))
                            path.addLine(to: CGPoint(x: center.x + radius, y: center.y))
                            path.move(to: CGPoint(x: center.x, y: center.y - radius))
                            path.addLine(to: CGPoint(x: center.x, y: center.y + radius))
                        }
                        .stroke(Color(red: 0.0, green: 0.9, blue: 1.0).opacity(0.15), lineWidth: 1)

                        // Sweeping Beam Line
                        Path { path in
                            path.move(to: center)
                            let rad = sweepAngle * .pi / 180
                            let target = CGPoint(
                                x: center.x + CGFloat(cos(rad)) * radius,
                                y: center.y + CGFloat(sin(rad)) * radius
                            )
                            path.addLine(to: target)
                        }
                        .stroke(
                            LinearGradient(
                                colors: [Color(red: 0.0, green: 0.9, blue: 1.0), Color.clear],
                                startPoint: .center,
                                endPoint: .topTrailing
                            ),
                            lineWidth: 2
                        )

                        // Center Node (Self)
                        Circle()
                            .fill(Color(red: 0.0, green: 0.9, blue: 1.0))
                            .frame(width: 12, height: 12)
                            .shadow(color: Color(red: 0.0, green: 0.9, blue: 1.0), radius: 8)

                        // Discovered Peer Nodes Blips
                        let peers = Array(bleManager.discoveredPeers.values)
                        ForEach(Array(peers.enumerated()), id: \.element.id) { index, peer in
                            let angle = Double(index) * (360.0 / Double(max(1, peers.count))) * .pi / 180
                            let normalizedDistance = min(1.0, max(0.2, (Double(-peer.rssi) - 30.0) / 70.0))
                            let peerX = center.x + CGFloat(cos(angle)) * (radius * normalizedDistance)
                            let peerY = center.y + CGFloat(sin(angle)) * (radius * normalizedDistance)

                            Button(action: {
                                selectedPeer = peer
                                bleManager.activePeer = peer
                            }) {
                                VStack(spacing: 2) {
                                    Circle()
                                        .fill(peer.isSosActive ? Color.red : Color(red: 0.55, green: 0.36, blue: 0.96))
                                        .frame(width: 10, height: 10)
                                        .shadow(color: peer.isSosActive ? Color.red : Color.purple, radius: 6)

                                    Text(peer.name)
                                        .font(.system(size: 9, weight: .bold, design: .monospaced))
                                        .foregroundColor(.white)
                                        .padding(.horizontal, 4)
                                        .background(Color.black.opacity(0.6))
                                        .cornerRadius(3)

                                    Text("\(peer.rssi) dBm")
                                        .font(.system(size: 8, design: .monospaced))
                                        .foregroundColor(Color(red: 0.0, green: 0.9, blue: 1.0))
                                }
                            }
                            .position(x: peerX, y: peerY)
                        }
                    }
                }
                .frame(height: 320)
                .onReceive(timer) { _ in
                    sweepAngle = (sweepAngle + 3).truncatingRemainder(dividingBy: 360)
                }

                // Discovered Peers List Drawer
                VStack(alignment: .leading, spacing: 8) {
                    Text("DETECTED SMARTPHONE NODES (\(bleManager.discoveredPeers.count))")
                        .font(.system(size: 11, weight: .bold, design: .monospaced))
                        .foregroundColor(Color(red: 0.0, green: 0.9, blue: 1.0))
                        .padding(.horizontal, 20)

                    ScrollView {
                        LazyVStack(spacing: 8) {
                            ForEach(Array(bleManager.discoveredPeers.values)) { peer in
                                HStack {
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(peer.name)
                                            .font(.system(size: 14, weight: .bold))
                                            .foregroundColor(.white)
                                        Text(peer.proximityTier.rawValue)
                                            .font(.system(size: 11, design: .monospaced))
                                            .foregroundColor(.gray)
                                    }

                                    Spacer()

                                    Text("\(peer.rssi) dBm")
                                        .font(.system(size: 12, weight: .bold, design: .monospaced))
                                        .foregroundColor(Color(red: 0.0, green: 0.9, blue: 1.0))

                                    Button(action: {
                                        bleManager.activePeer = peer
                                    }) {
                                        Text("Chat")
                                            .font(.system(size: 11, weight: .bold))
                                            .padding(.horizontal, 12)
                                            .padding(.vertical, 6)
                                            .background(Color(red: 0.0, green: 0.9, blue: 1.0))
                                            .foregroundColor(Color(red: 0.02, green: 0.03, blue: 0.07))
                                            .cornerRadius(6)
                                    }
                                }
                                .padding(12)
                                .background(Color.white.opacity(0.04))
                                .cornerRadius(10)
                                .padding(.horizontal, 20)
                            }
                        }
                    }
                }

                Spacer()
            }
        }
    }
}
