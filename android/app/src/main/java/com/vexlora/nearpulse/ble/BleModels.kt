package com.vexlora.nearpulse.ble

import java.util.UUID

enum class SignalCategory(val label: String, val distanceEstimate: String) {
    IMMEDIATE("Immediate", "< 1m"),
    NEAR("Near", "1m - 5m"),
    MID("Mid-Range", "5m - 15m"),
    FAR("Far", "15m - 30m+");

    companion object {
        fun fromRssi(rssi: Int): SignalCategory = when {
            rssi >= -60 -> IMMEDIATE
            rssi >= -75 -> NEAR
            rssi >= -90 -> MID
            else -> FAR
        }
    }
}

data class DiscoveredPeer(
    val address: String,
    val pseudonym: String,
    val rssi: Int,
    val signalCategory: SignalCategory,
    val lastSeenTimestamp: Long,
    val isSosActive: Boolean = false,
    val circleCode: String? = null
)

data class BleMessage(
    val id: String = UUID.randomUUID().toString().take(8),
    val peerAddress: String,
    val senderPseudonym: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isOutgoing: Boolean,
    val isDelivered: Boolean = false
)

data class SosAlert(
    val id: String = UUID.randomUUID().toString().take(8),
    val senderAddress: String,
    val senderPseudonym: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val rssi: Int = -50
)
