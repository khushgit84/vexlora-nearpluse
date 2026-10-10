import Foundation
import UserNotifications
import AVFoundation
import UIKit

/// Manages high-gain Emergency SOS audio sirens, Morse vibration haptics,
/// and heads-up chat notifications on iOS.
public final class EmergencyAlertManager: NSObject, UNUserNotificationCenterDelegate {
    public static let shared = EmergencyAlertManager()

    private var audioPlayer: AVAudioPlayer?
    private var isPlayingSiren: Bool = false
    private let hapticGenerator = UINotificationFeedbackGenerator()
    private var hapticTimer: Timer?

    private override init() {
        super.init()
        setupNotificationCategories()
        UNUserNotificationCenter.current().delegate = self
    }

    /// Requests user authorization for critical sound, alert banners, and badges
    public func requestPermissions() {
        let options: UNAuthorizationOptions = [.alert, .sound, .badge]
        UNUserNotificationCenter.current().requestAuthorization(options: options) { granted, error in
            if let error = error {
                print("[EmergencyAlertManager] Notification permission error: \(error.localizedDescription)")
            } else {
                print("[EmergencyAlertManager] Notification granted: \(granted)")
            }
        }
    }

    private func setupNotificationCategories() {
        // Action to instantly silence alarm from the lock screen notification banner
        let stopAction = UNNotificationAction(
            identifier: "ACTION_STOP_SOS_ALARM",
            title: "Problem Solved (Stop Siren)",
            options: [.destructive, .foreground]
        )

        let sosCategory = UNNotificationCategory(
            identifier: "NEAR_PULSE_SOS_CATEGORY",
            actions: [stopAction],
            intentIdentifiers: [],
            options: [.customDismissAction]
        )

        UNUserNotificationCenter.current().setNotificationCategories([sosCategory])
    }

    /// Triggers the full emergency distress sequence (Siren + Morse Haptic + Heads-up Alert)
    public func triggerSosAlert(senderPseudonym: String, message: String) {
        // 1. Post iOS System Notification Banner
        let content = UNMutableNotificationContent()
        content.title = "🚨 EMERGENCY SOS DISTRESS BEACON"
        content.subtitle = "Node: \(senderPseudonym) is nearby!"
        content.body = message.isEmpty ? "Distress alert received over 2.4 GHz radio. Tap to open tactical map." : message
        content.sound = UNNotificationSound.defaultCritical
        content.categoryIdentifier = "NEAR_PULSE_SOS_CATEGORY"

        let request = UNNotificationRequest(
            identifier: "SOS_ACTIVE_\(UUID().uuidString)",
            content: content,
            trigger: nil // Immediate delivery
        )

        UNUserNotificationCenter.current().add(request) { error in
            if let error = error {
                print("[EmergencyAlertManager] Failed to post SOS notification: \(error)")
            }
        }

        // 2. Start Emergency Siren Audio Loop
        startEmergencySiren()

        // 3. Start Repeating Morse Code Haptics (... --- ...)
        startMorseHaptics()
    }

    /// Posts a clean heads-up notification outside the app when a direct peer chat message is received
    public func showChatMessageNotification(senderPseudonym: String, text: String) {
        let content = UNMutableNotificationContent()
        content.title = "💬 NearPulse: \(senderPseudonym)"
        content.body = text
        content.sound = UNNotificationSound.default

        let request = UNNotificationRequest(
            identifier: "MSG_\(UUID().uuidString)",
            content: content,
            trigger: nil
        )

        UNUserNotificationCenter.current().add(request)
        hapticGenerator.notificationOccurred(.success)
    }

    /// Synthesizes and loops an emergency warble alarm using AVAudioPlayer
    public func startEmergencySiren() {
        stopEmergencySiren()
        isPlayingSiren = true

        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default, options: [.duckOthers])
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            print("[EmergencyAlertManager] AVAudioSession configuration error: \(error)")
        }

        // Generate high-intensity two-tone emergency siren in memory (880 Hz / 440 Hz warble)
        if let audioData = generateEmergencySirenWavData() {
            do {
                audioPlayer = try AVAudioPlayer(data: audioData)
                audioPlayer?.numberOfLoops = -1 // Infinite loop until problem is solved
                audioPlayer?.volume = 1.0
                audioPlayer?.play()
            } catch {
                print("[EmergencyAlertManager] AVAudioPlayer error: \(error)")
            }
        }
    }

    /// IMMEDIATELY stops and silences the emergency siren audio and haptics.
    /// Called when user taps "Problem Solved / I am Safe" or dismisses the alarm.
    public func stopEmergencySiren() {
        isPlayingSiren = false

        if let player = audioPlayer, player.isPlaying {
            player.stop()
        }
        audioPlayer = nil

        hapticTimer?.invalidate()
        hapticTimer = nil

        // Clean up audio session
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        print("[EmergencyAlertManager] Emergency siren & haptics STOPPED.")
    }

    private func startMorseHaptics() {
        hapticTimer?.invalidate()
        hapticGenerator.prepare()

        // Loop every 4 seconds: S-O-S pattern
        hapticTimer = Timer.scheduledTimer(withTimeInterval: 4.0, repeats: true) { [weak self] _ in
            guard let self = self, self.isPlayingSiren else { return }
            self.pulseMorsePattern()
        }
        pulseMorsePattern()
    }

    private func pulseMorsePattern() {
        // ... (3 short pulses)
        for i in 0..<3 {
            DispatchQueue.main.asyncAfter(deadline: .now() + Double(i) * 0.25) { [weak self] in
                self?.hapticGenerator.notificationOccurred(.warning)
            }
        }
        // --- (3 long pulses)
        for i in 0..<3 {
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.0 + Double(i) * 0.4) { [weak self] in
                self?.hapticGenerator.notificationOccurred(.error)
            }
        }
        // ... (3 short pulses)
        for i in 0..<3 {
            DispatchQueue.main.asyncAfter(deadline: .now() + 2.5 + Double(i) * 0.25) { [weak self] in
                self?.hapticGenerator.notificationOccurred(.warning)
            }
        }
    }

    /// Generates pure in-memory PCM WAV audio for an air-raid / ambulance alternating emergency siren
    private func generateEmergencySirenWavData() -> Data? {
        let sampleRate: Double = 44100.0
        let duration: Double = 1.0 // 1 second looped
        let numSamples = Int(sampleRate * duration)

        var pcmData = [Int16]()
        pcmData.reserveCapacity(numSamples)

        for i in 0..<numSamples {
            let t = Double(i) / sampleRate
            // Alternating two tones: 880Hz for 0.5s, 660Hz for 0.5s
            let frequency: Double = (t < 0.5) ? 880.0 : 660.0
            let sample = sin(2.0 * .pi * frequency * t)
            pcmData.append(Int16(sample * 30000.0))
        }

        var wavData = Data()
        // RIFF header
        wavData.append("RIFF".data(using: .ascii)!)
        var chunkSize = UInt32(36 + pcmData.count * 2)
        wavData.append(Data(bytes: &chunkSize, count: 4))
        wavData.append("WAVE".data(using: .ascii)!)

        // fmt chunk
        wavData.append("fmt ".data(using: .ascii)!)
        var subchunk1Size: UInt32 = 16
        wavData.append(Data(bytes: &subchunk1Size, count: 4))
        var audioFormat: UInt16 = 1 // PCM
        wavData.append(Data(bytes: &audioFormat, count: 2))
        var numChannels: UInt16 = 1 // Mono
        wavData.append(Data(bytes: &numChannels, count: 2))
        var sRate = UInt32(sampleRate)
        wavData.append(Data(bytes: &sRate, count: 4))
        var byteRate = UInt32(sampleRate * 2.0)
        wavData.append(Data(bytes: &byteRate, count: 4))
        var blockAlign: UInt16 = 2
        wavData.append(Data(bytes: &blockAlign, count: 2))
        var bitsPerSample: UInt16 = 16
        wavData.append(Data(bytes: &bitsPerSample, count: 2))

        // data chunk
        wavData.append("data".data(using: .ascii)!)
        var subchunk2Size = UInt32(pcmData.count * 2)
        wavData.append(Data(bytes: &subchunk2Size, count: 4))

        pcmData.withUnsafeBufferPointer { buffer in
            wavData.append(Data(buffer: buffer))
        }

        return wavData
    }

    // MARK: - UNUserNotificationCenterDelegate
    public func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        // Show banner and play sound even when the app is in the foreground!
        completionHandler([.banner, .sound, .badge])
    }

    public func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        if response.actionIdentifier == "ACTION_STOP_SOS_ALARM" {
            stopEmergencySiren()
        }
        completionHandler()
    }
}
