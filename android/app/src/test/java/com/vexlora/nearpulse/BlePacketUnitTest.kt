package com.vexlora.nearpulse

import com.vexlora.nearpulse.ble.*
import org.junit.Assert.*
import org.junit.Test

class BlePacketUnitTest {
    @Test
    fun testBlePacketSerialization() {
        val packet = BlePacket(
            packetType = BleConstants.PACKET_TYPE_MSG,
            messageId = "101",
            senderPseudonym = "NP-1",
            payload = "Hi"
        )
        val bytes = packet.toByteArray()
        val parsed = BlePacket.parse(bytes)
        assertNotNull(parsed)
        assertEquals("101", parsed?.messageId)
        assertEquals("NP-1", parsed?.senderPseudonym)
        assertEquals("Hi", parsed?.payload)
        assertEquals(BleConstants.PACKET_TYPE_MSG, parsed?.packetType)
    }

    @Test
    fun testSosPacket() {
        val packet = BlePacket(
            packetType = BleConstants.PACKET_TYPE_SOS,
            messageId = "SOS-99",
            senderPseudonym = "RESCUE-X",
            payload = "MEDICAL EMERGENCY"
        )
        val parsed = BlePacket.parse(packet.toByteArray())
        assertNotNull(parsed)
        assertEquals(BleConstants.PACKET_TYPE_SOS, parsed?.packetType)
        assertEquals("MEDICAL EMERGENCY", parsed?.payload)
    }

    @Test
    fun testSignalCategoryCalculations() {
        assertEquals(SignalCategory.IMMEDIATE, SignalCategory.fromRssi(-50))
        assertEquals(SignalCategory.NEAR, SignalCategory.fromRssi(-70))
        assertEquals(SignalCategory.MID, SignalCategory.fromRssi(-85))
        assertEquals(SignalCategory.FAR, SignalCategory.fromRssi(-98))
    }
}