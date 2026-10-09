package com.vexlora.nearpulse.data.repository

import com.vexlora.nearpulse.ble.BleManager
import com.vexlora.nearpulse.ble.BleMessage
import com.vexlora.nearpulse.ble.MessageDeliveryStatus
import com.vexlora.nearpulse.data.local.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NearPulseRepository(
    private val database: NearPulseDatabase,
    val bleManager: BleManager
) {
    private val messageDao = database.messageDao()
    private val peerDao = database.peerDao()
    private val safetyCircleDao = database.safetyCircleDao()

    fun getMessagesForPeer(peerAddress: String): Flow<List<BleMessage>> {
        return messageDao.getMessagesForPeer(peerAddress).map { list ->
            list.map { entity ->
                BleMessage(
                    id = entity.id,
                    peerAddress = entity.peerAddress,
                    senderPseudonym = entity.senderPseudonym,
                    content = entity.content,
                    timestamp = entity.timestamp,
                    isOutgoing = entity.isOutgoing,
                    status = try {
                        MessageDeliveryStatus.valueOf(entity.status)
                    } catch (e: Exception) {
                        MessageDeliveryStatus.DELIVERED
                    },
                    isSos = entity.isSos
                )
            }
        }
    }

    suspend fun saveMessage(message: BleMessage) {
        val entity = MessageEntity(
            id = message.id,
            peerAddress = message.peerAddress,
            senderPseudonym = message.senderPseudonym,
            content = message.content,
            timestamp = message.timestamp,
            isOutgoing = message.isOutgoing,
            status = message.status.name,
            isSos = message.isSos
        )
        messageDao.insertMessage(entity)
    }

    suspend fun updateMessageStatus(messageId: String, status: MessageDeliveryStatus) {
        messageDao.updateStatus(messageId, status.name)
    }

    suspend fun saveDiscoveredPeer(address: String, pseudonym: String, rssi: Int, circleCode: String?) {
        peerDao.upsertPeer(
            PeerEntity(
                address = address,
                pseudonym = pseudonym,
                lastRssi = rssi,
                lastSeenTimestamp = System.currentTimeMillis(),
                circleCode = circleCode
            )
        )
    }

    fun getStoredCircle(): Flow<SafetyCircleEntity?> = safetyCircleDao.getMyCircle()

    suspend fun createOrJoinCircle(code: String, name: String) {
        safetyCircleDao.saveCircle(
            SafetyCircleEntity(
                circleCode = code,
                circleName = name,
                createdAt = System.currentTimeMillis(),
                memberCount = 1
            )
        )
        bleManager.currentCircleCode = code
    }

    suspend fun leaveCircle() {
        safetyCircleDao.leaveCircle()
        bleManager.currentCircleCode = null
    }

    suspend fun clearAllData() {
        messageDao.clearAllMessages()
        peerDao.clearAllPeers()
        safetyCircleDao.leaveCircle()
        bleManager.clearDiscoveredPeers()
    }
}
