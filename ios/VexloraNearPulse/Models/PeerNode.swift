import Foundation
import CoreBluetooth

public enum ProximityTier: String, CaseIterable {
    case immediate = "Immediate (<1m)"
    case near = "Near (1-5m)"
    case midRange = "Mid-Range (5-15m)"
    case far = "Far (>15m)"

    public static func from(rssi: Int) -> ProximityTier {
        switch rssi {
        case -55...0:
            return .immediate
        case -70 ..< -55:
            return .near
        case -85 ..< -70:
            return .midRange
        default:
            return .far
        }
    }
}

public struct PeerNode: Identifiable, Equatable {
    public let id: String
    public var name: String
    public var rssi: Int
    public var lastSeen: Date
    public var isNearPulseNode: Bool
    public var isPhone: Bool
    public var isSosActive: Bool
    public var peripheral: CBPeripheral?

    public var proximityTier: ProximityTier {
        ProximityTier.from(rssi: rssi)
    }

    public static func == (lhs: PeerNode, rhs: PeerNode) -> Bool {
        lhs.id == rhs.id
    }
}
