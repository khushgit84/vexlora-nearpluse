package com.vexlora.nearpulse.ble

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

data class BlePacket(
    val packetType: Byte = BleConstants.PACKET_TYPE_MSG,
    val messageId: String,
    val senderPseudonym: String,
    val payload: String
) {
    fun toByteArray(): ByteArray {
        val idBytes = messageId.toByteArray(StandardCharsets.UTF_8)
        val senderBytes = senderPseudonym.toByteArray(StandardCharsets.UTF_8)
        val payloadBytes = payload.toByteArray(StandardCharsets.UTF_8)

        val buffer = ByteBuffer.allocate(6 + idBytes.size + senderBytes.size + payloadBytes.size)
        buffer.put(BleConstants.PROTOCOL_VERSION)
        buffer.put(packetType)
        buffer.put(idBytes.size.toByte())
        buffer.put(senderBytes.size.toByte())
        buffer.putShort(payloadBytes.size.toShort())
        buffer.put(idBytes)
        buffer.put(senderBytes)
        buffer.put(payloadBytes)
        return buffer.array()
    }

    companion object {
        fun parse(bytes: ByteArray): BlePacket? {
            if (bytes.size < 6) return null
            return try {
                val buffer = ByteBuffer.wrap(bytes)
                val version = buffer.get()
                if (version != BleConstants.PROTOCOL_VERSION) return null
                val packetType = buffer.get()
                val idLen = buffer.get().toInt() and 0xFF
                val senderLen = buffer.get().toInt() and 0xFF
                val payloadLen = buffer.short.toInt() and 0xFFFF

                if (buffer.remaining() < idLen + senderLen + payloadLen) return null

                val idBytes = ByteArray(idLen)
                buffer.get(idBytes)
                val senderBytes = ByteArray(senderLen)
                buffer.get(senderBytes)
                val payloadBytes = ByteArray(payloadLen)
                buffer.get(payloadBytes)

                BlePacket(
                    packetType = packetType,
                    messageId = String(idBytes, StandardCharsets.UTF_8),
                    senderPseudonym = String(senderBytes, StandardCharsets.UTF_8),
                    payload = String(payloadBytes, StandardCharsets.UTF_8)
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
