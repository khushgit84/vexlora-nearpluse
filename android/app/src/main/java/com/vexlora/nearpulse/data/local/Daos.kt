package com.vexlora.nearpulse.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE peerAddress = :peerAddress ORDER BY timestamp ASC")
    fun getMessagesForPeer(peerAddress: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET isDelivered = 1 WHERE id = :messageId")
    suspend fun markDelivered(messageId: String)

    @Query("DELETE FROM messages WHERE peerAddress = :peerAddress")
    suspend fun deleteMessagesForPeer(peerAddress: String)

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()
}

@Dao
interface PeerDao {
    @Query("SELECT * FROM peers ORDER BY lastSeenTimestamp DESC")
    fun getAllPeers(): Flow<List<PeerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePeer(peer: PeerEntity)

    @Query("SELECT * FROM peers WHERE address = :address LIMIT 1")
    suspend fun getPeerByAddress(address: String): PeerEntity?

    @Query("UPDATE peers SET circleCode = :circleCode WHERE address = :address")
    suspend fun updatePeerCircle(address: String, circleCode: String)

    @Query("DELETE FROM peers")
    suspend fun deleteAllPeers()
}

@Dao
interface SafetyCircleDao {
    @Query("SELECT * FROM safety_circles ORDER BY createdAt DESC")
    fun getAllCircles(): Flow<List<SafetyCircleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCircle(circle: SafetyCircleEntity)

    @Delete
    suspend fun deleteCircle(circle: SafetyCircleEntity)

    @Query("DELETE FROM safety_circles")
    suspend fun deleteAllCircles()
}
