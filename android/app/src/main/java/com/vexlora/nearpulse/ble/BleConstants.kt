package com.vexlora.nearpulse.ble

import java.util.UUID

object BleConstants {
    // 128-bit custom service UUID for VEXLORA NearPulse
    val SERVICE_UUID: UUID = UUID.fromString("7b14d2e0-252a-4df4-8d4e-e17f5412e601")

    // Characteristic for sending and receiving short text messages
    val CHAR_MESSAGE_UUID: UUID = UUID.fromString("7b14d2e1-252a-4df4-8d4e-e17f5412e601")

    // Characteristic for message acknowledgments (Delivery Receipts)
    val CHAR_ACK_UUID: UUID = UUID.fromString("7b14d2e2-252a-4df4-8d4e-e17f5412e601")

    // Characteristic for SOS emergency signaling & mutual rescue ACK
    val CHAR_SOS_UUID: UUID = UUID.fromString("7b14d2e3-252a-4df4-8d4e-e17f5412e601")

    // Characteristic for device pseudonym and safety circle handshake
    val CHAR_INFO_UUID: UUID = UUID.fromString("7b14d2e4-252a-4df4-8d4e-e17f5412e601")

    // Standard Client Characteristic Configuration Descriptor
    val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    const val PROTOCOL_VERSION: Byte = 0x01
    const val PACKET_TYPE_MSG: Byte = 0x10
    const val PACKET_TYPE_ACK: Byte = 0x20
    const val PACKET_TYPE_SOS: Byte = 0x30
    const val PACKET_TYPE_SOS_ACK: Byte = 0x31

    const val MAX_MESSAGE_LENGTH = 280
    const val REQUESTED_MTU = 512
}