package com.vexlora.nearpulse.ble

import java.nio.charset.StandardCharsets
import java.util.UUID

enum class SignalCategory(val label: String, val minRssi: Int, val maxRssi: Int) {
    EXCELLENT("Excellent Signal", -65, 0),
    STRONG("Strong Signal", -75, -66),
    MODERATE("Moderate Signal", -85, -76),
    WEAK("Weak Signal", -95, -86),
    MARGINAL("Marginal Signal", -120, -96);

    companion object {
        fun fromRssi(rssi: Int): SignalCategory = when {
            rssi >= -65 -> EXCELLENT
            rssi >= -75 -> STRONG
            rssi >= -85 -> MODERATE
            rssi >= -95 -> WEAK
            else -> MARGINAL
        }
    }
}

enum class BlePowerMode(val label: String, val dutyCycleScanSec: Int) {
    BALANCED("Balanced Power", 0),
    LOW_POWER("Low-Power Duty", 5),
    BATTERY_SAVER("Emergency Saver", 12)
}

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING
}

enum class MessageDeliveryStatus {
    PENDING,
    SENDING,
    DELIVERED,
    FAILED
}

data class DiscoveredPeer(
    val address: String,
    val pseudonym: String,
    val rssi: Int,
    val signalCategory: SignalCategory = SignalCategory.fromRssi(rssi),
    val rssiHistory: List<Int> = listOf(rssi),
    val batteryLevel: Int = 85,
    val powerMode: BlePowerMode = BlePowerMode.BALANCED,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val isSosActive: Boolean = false,
    val sosAlertDetails: String? = null,
    val circleCode: String? = null,
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED
)

data class BleMessage(
    val id: String = UUID.randomUUID().toString(),
    val peerAddress: String,
    val senderPseudonym: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isOutgoing: Boolean,
    val status: MessageDeliveryStatus = MessageDeliveryStatus.PENDING,
    val isSos: Boolean = false
)

data class SosAlert(
    val alertId: String,
    val broadcasterAddress: String,
    val broadcasterPseudonym: String,
    val timestamp: Long,
    val details: String,
    val isAcknowledgedByLocalUser: Boolean = false
)

data class BlePacket(
    val version: Byte = BleConstants.PROTOCOL_VERSION,
    val packetType: Byte,
    val messageId: String,
    val senderPseudonym: String,
    val payload: String
) {
    fun toByteArray(): ByteArray {
        val serialized = "${packetType}:${messageId}:${senderPseudonym}:${payload}"
        return serialized.toByteArray(StandardCharsets.UTF_8)
    }

    companion object {
        fun parse(data: ByteArray): BlePacket? {
            return try {
                val str = String(data, StandardCharsets.UTF_8)
                val parts = str.split(":", limit = 4)
                if (parts.size >= 4) {
                    val pType = parts[0].toByte()
                    val mId = parts[1]
                    val sName = parts[2]
                    val pLoad = parts[3]
                    BlePacket(
                        packetType = pType,
                        messageId = mId,
                        senderPseudonym = sName,
                        payload = pLoad
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}
