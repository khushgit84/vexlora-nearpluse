import Foundation

/// Binary Packet Structure for VEXLORA NearPulse BLE Protocol.
/// Byte-for-byte serialization mirroring `BlePacket.kt`.
public struct BlePacket: Equatable {
    public let packetType: UInt8
    public let messageId: String
    public let senderPseudonym: String
    public let payload: String

    public init(
        packetType: UInt8 = BleConstants.packetTypeMsg,
        messageId: String = UUID().uuidString.prefix(8).uppercased(),
        senderPseudonym: String,
        payload: String
    ) {
        self.packetType = packetType
        self.messageId = messageId
        self.senderPseudonym = senderPseudonym
        self.payload = payload
    }

    /// Serializes the packet into binary wire format
    public func toData() -> Data {
        let idData = Data(messageId.utf8)
        let senderData = Data(senderPseudonym.utf8)
        let payloadData = Data(payload.utf8)

        var data = Data()
        data.reserveCapacity(6 + idData.count + senderData.count + payloadData.count)

        // 1. Protocol Version (1 byte)
        data.append(BleConstants.protocolVersion)

        // 2. Packet Type (1 byte)
        data.append(packetType)

        // 3. Message ID Length (1 byte)
        data.append(UInt8(truncatingIfNeeded: idData.count))

        // 4. Sender Pseudonym Length (1 byte)
        data.append(UInt8(truncatingIfNeeded: senderData.count))

        // 5. Payload Length (2 bytes Big-Endian)
        var payloadLengthBigEndian = UInt16(truncatingIfNeeded: payloadData.count).bigEndian
        withUnsafeBytes(of: &payloadLengthBigEndian) { bytes in
            data.append(contentsOf: bytes)
        }

        // 6. Data Buffers
        data.append(idData)
        data.append(senderData)
        data.append(payloadData)

        return data
    }

    /// Deserializes binary wire data into a BlePacket instance
    public static func parse(data: Data) -> BlePacket? {
        guard data.count >= 6 else { return nil }

        let version = data[0]
        guard version == BleConstants.protocolVersion else { return nil }

        let packetType = data[1]
        let idLen = Int(data[2])
        let senderLen = Int(data[3])

        let payloadLen = Int(data[4]) << 8 | Int(data[5])

        let totalExpected = 6 + idLen + senderLen + payloadLen
        guard data.count >= totalExpected else { return nil }

        var offset = 6
        let idData = data.subdata(in: offset ..< (offset + idLen))
        offset += idLen

        let senderData = data.subdata(in: offset ..< (offset + senderLen))
        offset += senderLen

        let payloadData = data.subdata(in: offset ..< (offset + payloadLen))

        guard let messageId = String(data: idData, encoding: .utf8),
              let sender = String(data: senderData, encoding: .utf8),
              let payload = String(data: payloadData, encoding: .utf8) else {
            return nil
        }

        return BlePacket(
            packetType: packetType,
            messageId: messageId,
            senderPseudonym: sender,
            payload: payload
        )
    }
}
