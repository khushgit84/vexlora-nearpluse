import SwiftUI

struct ChatView: View {
    @ObservedObject var bleManager = BleManager.shared
    @State private var inputMessage: String = ""

    var body: some View {
        ZStack {
            Color(red: 0.02, green: 0.03, blue: 0.07)
                .ignoresSafeArea()

            VStack(spacing: 0) {
                // Header
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("PEER CHAT")
                            .font(.system(size: 11, weight: .bold, design: .monospaced))
                            .foregroundColor(Color(red: 0.0, green: 0.9, blue: 1.0))

                        if let peer = bleManager.activePeer {
                            HStack {
                                Text(peer.name)
                                    .font(.system(size: 18, weight: .heavy))
                                    .foregroundColor(.white)
                                Text("• \(peer.rssi) dBm")
                                    .font(.system(size: 12, design: .monospaced))
                                    .foregroundColor(Color(red: 0.0, green: 0.9, blue: 1.0))
                            }
                        } else {
                            Text("No Peer Selected")
                                .font(.system(size: 18, weight: .heavy))
                                .foregroundColor(.gray)
                        }
                    }

                    Spacer()

                    if bleManager.activePeer != nil {
                        Button(action: {
                            bleManager.activePeer = nil
                        }) {
                            Text("Switch")
                                .font(.system(size: 12, weight: .semibold, design: .monospaced))
                                .foregroundColor(Color(red: 0.0, green: 0.9, blue: 1.0))
                                .padding(.horizontal, 10)
                                .padding(.vertical, 4)
                                .background(Color.white.opacity(0.06))
                                .cornerRadius(6)
                        }
                    }
                }
                .padding()
                .background(Color.white.opacity(0.03))

                Divider().background(Color.white.opacity(0.08))

                // Messages ScrollView
                if bleManager.activePeer == nil {
                    VStack(spacing: 12) {
                        Spacer()
                        Image(systemName: "antenna.radiowaves.left.and.right")
                            .font(.system(size: 48))
                            .foregroundColor(Color(red: 0.0, green: 0.9, blue: 1.0).opacity(0.6))
                        Text("Select a nearby phone from the Radar tab to begin direct offline peer chat.")
                            .font(.system(size: 14))
                            .foregroundColor(.gray)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 40)
                        Spacer()
                    }
                } else {
                    ScrollViewReader { proxy in
                        ScrollView {
                            LazyVStack(spacing: 12) {
                                ForEach(bleManager.chatMessages) { msg in
                                    HStack {
                                        if msg.isOutgoing { Spacer() }

                                        VStack(alignment: msg.isOutgoing ? .trailing : .leading, spacing: 4) {
                                            Text(msg.senderPseudonym)
                                                .font(.system(size: 10, weight: .bold, design: .monospaced))
                                                .foregroundColor(msg.isOutgoing ? Color(red: 0.0, green: 0.9, blue: 1.0) : Color(red: 0.55, green: 0.36, blue: 0.96))

                                            Text(msg.text)
                                                .font(.system(size: 15))
                                                .foregroundColor(.white)
                                                .padding(.horizontal, 14)
                                                .padding(.vertical, 10)
                                                .background(
                                                    msg.isOutgoing
                                                        ? Color(red: 0.0, green: 0.45, blue: 0.65).opacity(0.4)
                                                        : Color(red: 0.35, green: 0.2, blue: 0.55).opacity(0.4)
                                                )
                                                .overlay(
                                                    RoundedRectangle(cornerRadius: 12)
                                                        .stroke(
                                                            msg.isOutgoing
                                                                ? Color(red: 0.0, green: 0.9, blue: 1.0).opacity(0.3)
                                                                : Color(red: 0.55, green: 0.36, blue: 0.96).opacity(0.3),
                                                            lineWidth: 1
                                                        )
                                                )
                                                .cornerRadius(12)

                                            HStack(spacing: 4) {
                                                Text(msg.timestamp, style: .time)
                                                    .font(.system(size: 9, design: .monospaced))
                                                    .foregroundColor(.gray)

                                                if msg.isOutgoing {
                                                    Text(msg.status.rawValue)
                                                        .font(.system(size: 9, design: .monospaced))
                                                        .foregroundColor(Color(red: 0.0, green: 0.9, blue: 1.0))
                                                }
                                            }
                                        }

                                        if !msg.isOutgoing { Spacer() }
                                    }
                                    .padding(.horizontal, 16)
                                    .id(msg.id)
                                }
                            }
                            .padding(.vertical, 16)
                        }
                    }
                }

                // Input Bar
                if let peer = bleManager.activePeer {
                    HStack(spacing: 10) {
                        TextField("Type encrypted offline message...", text: $inputMessage)
                            .font(.system(size: 14))
                            .padding(12)
                            .background(Color.white.opacity(0.06))
                            .foregroundColor(.white)
                            .cornerRadius(10)

                        Button(action: {
                            guard !inputMessage.isEmpty else { return }
                            bleManager.sendMessage(text: inputMessage, to: peer)
                            inputMessage = ""
                        }) {
                            Image(systemName: "paperplane.fill")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(Color(red: 0.02, green: 0.03, blue: 0.07))
                                .frame(width: 44, height: 44)
                                .background(Color(red: 0.0, green: 0.9, blue: 1.0))
                                .cornerRadius(10)
                        }
                    }
                    .padding()
                    .background(Color.white.opacity(0.03))
                }
            }
        }
    }
}
