import Foundation
import CoreBluetooth
import Combine

/// Dual-Role Bluetooth Low Energy Engine for iOS (VEXLORA NearPulse).
/// Implements CBCentralManager (client) and CBPeripheralManager (server) concurrently.
public final class BleManager: NSObject, ObservableObject {
    public static let shared = BleManager()

    // MARK: - Published Reactive State
    @Published public var isScanning: Bool = false
    @Published public var isAdvertising: Bool = false
    @Published public var bluetoothState: CBManagerState = .unknown
    @Published public var discoveredPeers: [String: PeerNode] = [:]
    @Published public var activePeer: PeerNode? = nil
    @Published public var chatMessages: [ChatMessage] = []
    @Published public var activeSosAlert: SosAlert? = nil
    @Published public var isSosBroadcasting: Bool = false

    @Published public var localPseudonym: String = {
        let code = String(UUID().uuidString.prefix(4)).uppercased()
        return "NP-IOS-\(code)"
    }()

    // MARK: - CoreBluetooth Managers
    private var centralManager: CBCentralManager!
    private var peripheralManager: CBPeripheralManager!

    // Peripheral connections & characteristics cache
    private var connectedPeripherals: [UUID: CBPeripheral] = [:]
    private var charMessageMap: [UUID: CBCharacteristic] = [:]
    private var charAckMap: [UUID: CBCharacteristic] = [:]
    private var charSosMap: [UUID: CBCharacteristic] = [:]

    // Local GATT Service & Characteristics
    private var localMessageChar: CBMutableCharacteristic?
    private var localAckChar: CBMutableCharacteristic?
    private var localSosChar: CBMutableCharacteristic?
    private var localInfoChar: CBMutableCharacteristic?

    // Resolved SOS addresses to prevent looping
    private var resolvedSosAddresses = Set<String>()

    private override init() {
        super.init()
        centralManager = CBCentralManager(delegate: self, queue: nil, options: [
            CBCentralManagerOptionShowPowerAlertKey: true
        ])
        peripheralManager = CBPeripheralManager(delegate: self, queue: nil, options: [
            CBPeripheralManagerOptionShowPowerAlertKey: true
        ])
    }

    // MARK: - Public Controls
    public func startScanning() {
        guard centralManager.state == .poweredOn else { return }
        centralManager.scanForPeripherals(
            withServices: [BleConstants.serviceUUID],
            options: [CBCentralManagerScanOptionAllowDuplicatesKey: true]
        )
        isScanning = true
        print("[BleManager] Scanning started for service: \(BleConstants.serviceUUID)")
    }

    public func stopScanning() {
        centralManager.stopScan()
        isScanning = false
        print("[BleManager] Scanning stopped.")
    }

    public func startAdvertising() {
        guard peripheralManager.state == .poweredOn else { return }
        let advertisementData: [String: Any] = [
            CBAdvertisementDataServiceUUIDsKey: [BleConstants.serviceUUID],
            CBAdvertisementDataLocalNameKey: localPseudonym
        ]
        peripheralManager.startAdvertising(advertisementData)
        isAdvertising = true
        print("[BleManager] Advertising started as: \(localPseudonym)")
    }

    public func stopAdvertising() {
        peripheralManager.stopAdvertising()
        isAdvertising = false
        print("[BleManager] Advertising stopped.")
    }

    // MARK: - Direct Chat Sending
    public func sendMessage(text: String, to peer: PeerNode) {
        guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        let packet = BlePacket(
            packetType: BleConstants.packetTypeMsg,
            senderPseudonym: localPseudonym,
            payload: text
        )

        let outgoingMsg = ChatMessage(
            id: packet.messageId,
            senderPseudonym: localPseudonym,
            recipientId: peer.id,
            text: text,
            isOutgoing: true,
            status: .sending
        )
        chatMessages.append(outgoingMsg)

        let data = packet.toData()

        if let peripheral = peer.peripheral,
           let char = charMessageMap[peripheral.identifier] {
            peripheral.writeValue(data, for: char, type: .withResponse)
        } else if let peripheral = peer.peripheral {
            // Attempt connect and write
            centralManager.connect(peripheral, options: nil)
        }
    }

    // MARK: - Emergency SOS Trigger & Stop
    public func triggerSosBeacon(distressText: String = "EMERGENCY: Urgent assistance needed!") {
        isSosBroadcasting = true
        EmergencyAlertManager.shared.triggerSosAlert(senderPseudonym: localPseudonym, message: distressText)

        let sosPacket = BlePacket(
            packetType: BleConstants.packetTypeSos,
            senderPseudonym: localPseudonym,
            payload: distressText
        )
        let data = sosPacket.toData()

        // Broadcast to all connected peripherals
        for (id, peripheral) in connectedPeripherals {
            if let sosChar = charSosMap[id] {
                peripheral.writeValue(data, for: sosChar, type: .withResponse)
            }
        }

        // Notify subscribers of local SOS characteristic
        if let localSos = localSosChar {
            peripheralManager.updateValue(data, for: localSos, onSubscribedCentrals: nil)
        }
    }

    /// Stops the SOS emergency, cuts off siren sound, and notifies network that problem is solved!
    public func resolveSosProblem() {
        isSosBroadcasting = false
        activeSosAlert = nil
        EmergencyAlertManager.shared.stopEmergencySiren()

        let cancelPacket = BlePacket(
            packetType: BleConstants.packetTypeSosCancel,
            senderPseudonym: localPseudonym,
            payload: "PROBLEM_SOLVED"
        )
        let data = cancelPacket.toData()

        for (id, peripheral) in connectedPeripherals {
            if let sosChar = charSosMap[id] {
                peripheral.writeValue(data, for: sosChar, type: .withResponse)
            }
        }

        if let localSos = localSosChar {
            peripheralManager.updateValue(data, for: localSos, onSubscribedCentrals: nil)
        }
        print("[BleManager] SOS Alarm CANCELLED and sound STOPPED.")
    }

    // MARK: - Smart Phone Filter Heuristic
    private func isSmartPhone(peripheral: CBPeripheral, advertisementData: [String: Any]) -> Bool {
        // 1. Service UUID match
        if let uuids = advertisementData[CBAdvertisementDataServiceUUIDsKey] as? [CBUUID],
           uuids.contains(BleConstants.serviceUUID) {
            return true
        }

        // 2. Local Name check
        let name = (advertisementData[CBAdvertisementDataLocalNameKey] as? String ?? peripheral.name ?? "").lowercased()
        if name.hasPrefix("np-") || name.hasPrefix("sos-") {
            return true
        }

        // 3. Known Smartphone Brand Keywords
        let phoneKeywords = [
            "iphone", "redmi", "xiaomi", "oneplus", "nord", "galaxy", "pixel",
            "realme", "oppo", "vivo", "motorola", "moto", "nothing", "poco",
            "iqoo", "honor", "huawei", "mobile", "phone"
        ]

        for kw in phoneKeywords {
            if name.contains(kw) {
                return true
            }
        }

        return false
    }
}

// MARK: - CBCentralManagerDelegate
extension BleManager: CBCentralManagerDelegate {
    public func centralManagerDidUpdateState(_ central: CBCentralManager) {
        DispatchQueue.main.async {
            self.bluetoothState = central.state
            if central.state == .poweredOn {
                self.startScanning()
            }
        }
    }

    public func centralManager(
        _ central: CBCentralManager,
        didDiscover peripheral: CBPeripheral,
        advertisementData: [String: Any],
        rssi RSSI: NSNumber
    ) {
        // Apply Smart Phone Filter
        let isPhone = isSmartPhone(peripheral: peripheral, advertisementData: advertisementData)
        guard isPhone else { return }

        let id = peripheral.identifier.uuidString
        let advName = advertisementData[CBAdvertisementDataLocalNameKey] as? String
        let name = advName ?? peripheral.name ?? "Node-\(String(id.prefix(4)))"
        let isSos = name.hasPrefix("SOS-")

        let peer = PeerNode(
            id: id,
            name: name,
            rssi: RSSI.intValue,
            lastSeen: Date(),
            isNearPulseNode: true,
            isPhone: true,
            isSosActive: isSos,
            peripheral: peripheral
        )

        DispatchQueue.main.async {
            self.discoveredPeers[id] = peer
        }
    }

    public func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        peripheral.delegate = self
        connectedPeripherals[peripheral.identifier] = peripheral
        peripheral.discoverServices([BleConstants.serviceUUID])
    }
}

// MARK: - CBPeripheralDelegate
extension BleManager: CBPeripheralDelegate {
    public func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        guard let services = peripheral.services else { return }
        for service in services where service.uuid == BleConstants.serviceUUID {
            peripheral.discoverCharacteristics([
                BleConstants.charMessageUUID,
                BleConstants.charAckUUID,
                BleConstants.charSosUUID,
                BleConstants.charInfoUUID
            ], for: service)
        }
    }

    public func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
        guard let chars = service.characteristics else { return }
        for char in chars {
            if char.uuid == BleConstants.charMessageUUID {
                charMessageMap[peripheral.identifier] = char
                peripheral.setNotifyValue(true, for: char)
            } else if char.uuid == BleConstants.charAckUUID {
                charAckMap[peripheral.identifier] = char
                peripheral.setNotifyValue(true, for: char)
            } else if char.uuid == BleConstants.charSosUUID {
                charSosMap[peripheral.identifier] = char
                peripheral.setNotifyValue(true, for: char)
            }
        }
    }

    public func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        guard let data = characteristic.value, let packet = BlePacket.parse(data: data) else { return }

        DispatchQueue.main.async {
            switch packet.packetType {
            case BleConstants.packetTypeMsg:
                let msg = ChatMessage(
                    id: packet.messageId,
                    senderPseudonym: packet.senderPseudonym,
                    recipientId: "LOCAL",
                    text: packet.payload,
                    isOutgoing: false,
                    status: .delivered
                )
                self.chatMessages.append(msg)
                EmergencyAlertManager.shared.showChatMessageNotification(
                    senderPseudonym: packet.senderPseudonym,
                    text: packet.payload
                )

                // Send ACK delivery receipt
                let ackPacket = BlePacket(
                    packetType: BleConstants.packetTypeAck,
                    messageId: packet.messageId,
                    senderPseudonym: self.localPseudonym,
                    payload: "DELIVERED"
                )
                if let ackChar = self.charAckMap[peripheral.identifier] {
                    peripheral.writeValue(ackPacket.toData(), for: ackChar, type: .withResponse)
                }

            case BleConstants.packetTypeAck:
                if let idx = self.chatMessages.firstIndex(where: { $0.id == packet.messageId }) {
                    self.chatMessages[idx].status = .delivered
                }

            case BleConstants.packetTypeSos:
                let alert = SosAlert(
                    id: packet.messageId,
                    senderAddress: peripheral.identifier.uuidString,
                    pseudonym: packet.senderPseudonym,
                    rssi: -50
                )
                self.activeSosAlert = alert
                EmergencyAlertManager.shared.triggerSosAlert(
                    senderPseudonym: packet.senderPseudonym,
                    message: packet.payload
                )

            case BleConstants.packetTypeSosCancel:
                self.activeSosAlert = nil
                EmergencyAlertManager.shared.stopEmergencySiren()

            default:
                break
            }
        }
    }
}

// MARK: - CBPeripheralManagerDelegate (GATT Server Hosting)
extension BleManager: CBPeripheralManagerDelegate {
    public func peripheralManagerDidUpdateState(_ peripheral: CBPeripheralManager) {
        if peripheral.state == .poweredOn {
            setupLocalGattServer()
            startAdvertising()
        }
    }

    private func setupLocalGattServer() {
        let service = CBMutableService(type: BleConstants.serviceUUID, primary: true)

        let msgChar = CBMutableCharacteristic(
            type: BleConstants.charMessageUUID,
            properties: [.write, .writeWithoutResponse, .notify, .read],
            value: nil,
            permissions: [.writeable, .readable]
        )

        let ackChar = CBMutableCharacteristic(
            type: BleConstants.charAckUUID,
            properties: [.write, .writeWithoutResponse, .notify, .read],
            value: nil,
            permissions: [.writeable, .readable]
        )

        let sosChar = CBMutableCharacteristic(
            type: BleConstants.charSosUUID,
            properties: [.write, .notify, .read],
            value: nil,
            permissions: [.writeable, .readable]
        )

        service.characteristics = [msgChar, ackChar, sosChar]
        peripheralManager.add(service)

        self.localMessageChar = msgChar
        self.localAckChar = ackChar
        self.localSosChar = sosChar
    }

    public func peripheralManager(_ peripheral: CBPeripheralManager, didReceiveWrite requests: [CBATTRequest]) {
        for request in requests {
            if let data = request.value, let packet = BlePacket.parse(data: data) {
                DispatchQueue.main.async {
                    if packet.packetType == BleConstants.packetTypeSos {
                        EmergencyAlertManager.shared.triggerSosAlert(
                            senderPseudonym: packet.senderPseudonym,
                            message: packet.payload
                        )
                    } else if packet.packetType == BleConstants.packetTypeSosCancel {
                        EmergencyAlertManager.shared.stopEmergencySiren()
                    } else if packet.packetType == BleConstants.packetTypeMsg {
                        let msg = ChatMessage(
                            id: packet.messageId,
                            senderPseudonym: packet.senderPseudonym,
                            recipientId: "LOCAL",
                            text: packet.payload,
                            isOutgoing: false
                        )
                        self.chatMessages.append(msg)
                        EmergencyAlertManager.shared.showChatMessageNotification(
                            senderPseudonym: packet.senderPseudonym,
                            text: packet.payload
                        )
                    }
                }
            }
            peripheralManager.respond(to: request, withResult: .success)
        }
    }
}
