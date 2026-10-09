package com.vexlora.nearpulse

import com.vexlora.nearpulse.ble.BleConstants
import com.vexlora.nearpulse.ble.BlePacket
import com.vexlora.nearpulse.ble.SignalCategory
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class BlePacketUnitTest {

    @Test
    fun testBlePacketSerializationAndParsing() {
        val originalMessageId = UUID.randomUUID().toString()
        val originalPacket = BlePacket(
            version = BleConstants.PROTOCOL_VERSION,
            packetType = BleConstants.PACKET_TYPE_MSG,
            messageId = originalMessageId,
            senderPseudonym = "NP-ALPH",
            payload = "Hello via offline Bluetooth LE!"
        )

        val bytes = originalPacket.toByteArray()
        assertNotNull(bytes)
        assertTrue(bytes.isNotEmpty())

        val parsed = BlePacket.parse(bytes)
        assertNotNull(parsed)
        assertEquals(BleConstants.PACKET_TYPE_MSG, parsed?.packetType)
        assertEquals(originalMessageId, parsed?.messageId)
        assertEquals("NP-ALPH", parsed?.senderPseudonym)
        assertEquals("Hello via offline Bluetooth LE!", parsed?.payload)
    }

    @Test
    fun testSosPacketHandling() {
        val sosId = "SOS-991"
        val sosPacket = BlePacket(
            packetType = BleConstants.PACKET_TYPE_SOS,
            messageId = sosId,
            senderPseudonym = "RESCUE-01",
            payload = "Injury reported at checkpoint 3"
        )

        val bytes = sosPacket.toByteArray()
        val parsed = BlePacket.parse(bytes)

        assertNotNull(parsed)
        assertEquals(BleConstants.PACKET_TYPE_SOS, parsed?.packetType)
        assertEquals(sosId, parsed?.messageId)
        assertEquals("Injury reported at checkpoint 3", parsed?.payload)
    }

    @Test
    fun testSignalCategoryRssiCategorization() {
        // Excellent: >= -65 dBm
        assertEquals(SignalCategory.EXCELLENT, SignalCategory.fromRssi(-45))
        assertEquals(SignalCategory.EXCELLENT, SignalCategory.fromRssi(-65))

        // Strong: -75 to -66 dBm
        assertEquals(SignalCategory.STRONG, SignalCategory.fromRssi(-66))
        assertEquals(SignalCategory.STRONG, SignalCategory.fromRssi(-75))

        // Moderate: -85 to -76 dBm
        assertEquals(SignalCategory.MODERATE, SignalCategory.fromRssi(-80))

        // Weak: -95 to -86 dBm
        assertEquals(SignalCategory.WEAK, SignalCategory.fromRssi(-90))

        // Marginal: < -95 dBm
        assertEquals(SignalCategory.MARGINAL, SignalCategory.fromRssi(-105))
    }

    @Test
    fun testMalformedPacketReturnsNull() {
        val malformedBytes = "corrupted_packet_without_delimiter".toByteArray()
        val parsed = BlePacket.parse(malformedBytes)
        assertNull(parsed)
    }
}
