import Foundation

public enum MessageDeliveryStatus: String, Codable {
    case sending = "Sending..."
    case delivered = "Delivered ✓"
    case failed = "Failed ✕"
}

public struct ChatMessage: Identifiable, Equatable, Codable {
    public let id: String
    public let senderPseudonym: String
    public let recipientId: String
    public let text: String
    public let timestamp: Date
    public let isOutgoing: Bool
    public var status: MessageDeliveryStatus

    public init(
        id: String = UUID().uuidString.prefix(8).uppercased(),
        senderPseudonym: String,
        recipientId: String,
        text: String,
        timestamp: Date = Date(),
        isOutgoing: Bool,
        status: MessageDeliveryStatus = .delivered
    ) {
        self.id = id
        self.senderPseudonym = senderPseudonym
        self.recipientId = recipientId
        self.text = text
        self.timestamp = timestamp
        self.isOutgoing = isOutgoing
        self.status = status
    }
}

public struct SosAlert: Identifiable, Equatable {
    public let id: String
    public let senderAddress: String
    public let pseudonym: String
    public let timestamp: Date
    public var rssi: Int
    public var isResolved: Bool

    public init(
        id: String = UUID().uuidString,
        senderAddress: String,
        pseudonym: String,
        timestamp: Date = Date(),
        rssi: Int = -50,
        isResolved: Bool = false
    ) {
        self.id = id
        self.senderAddress = senderAddress
        self.pseudonym = pseudonym
        self.timestamp = timestamp
        self.rssi = rssi
        self.isResolved = isResolved
    }
}
