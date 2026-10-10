import Foundation
import CoreBluetooth

/// Shared Bluetooth Low Energy Protocol Constants for VEXLORA NearPulse.
/// 100% Protocol-compatible with the Android Kotlin implementation.
public enum BleConstants {
    // 128-bit custom GATT Service UUID
    public static let serviceUUID = CBUUID(string: "7b14d2e0-252a-4df4-8d4e-e17f5412e601")

    // Characteristic for sending and receiving direct chat messages
    public static let charMessageUUID = CBUUID(string: "7b14d2e1-252a-4df4-8d4e-e17f5412e601")

    // Characteristic for message acknowledgments (Delivery Receipts)
    public static let charAckUUID = CBUUID(string: "7b14d2e2-252a-4df4-8d4e-e17f5412e601")

    // Characteristic for SOS emergency signaling & mutual rescue alarms
    public static let charSosUUID = CBUUID(string: "7b14d2e3-252a-4df4-8d4e-e17f5412e601")

    // Characteristic for device pseudonym and safety circle handshake
    public static let charInfoUUID = CBUUID(string: "7b14d2e4-252a-4df4-8d4e-e17f5412e601")

    // Standard Client Characteristic Configuration Descriptor (Notifications/Indications)
    public static let cccdUUID = CBUUID(string: "00002902-0000-1000-8000-00805f9b34fb")

    // Protocol Version & Packet Type Headers
    public static let protocolVersion: UInt8 = 0x01
    public static let packetTypeMsg: UInt8 = 0x10
    public static let packetTypeAck: UInt8 = 0x20
    public static let packetTypeSos: UInt8 = 0x30
    public static let packetTypeSosAck: UInt8 = 0x31
    public static let packetTypeSosCancel: UInt8 = 0x32

    public static let maxMessageLength: Int = 280
    public static let defaultRequestedMTU: Int = 512
}
