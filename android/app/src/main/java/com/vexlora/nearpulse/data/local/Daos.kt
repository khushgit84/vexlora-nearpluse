package com.vexlora.nearpulse.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE peerAddress = :peerAddress ORDER BY timestamp ASC")
    fun getMessagesForPeer(peerAddress: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateStatus(messageId: String, status: String)

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()
}

@Dao
interface PeerDao {
    @Query("SELECT * FROM peers ORDER BY lastSeenTimestamp DESC")
    fun getAllPeers(): Flow<List<PeerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPeer(peer: PeerEntity)

    @Query("DELETE FROM peers")
    suspend fun clearAllPeers()
}

@Dao
interface SafetyCircleDao {
    @Query("SELECT * FROM safety_circle LIMIT 1")
    fun getMyCircle(): Flow<SafetyCircleEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCircle(circle: SafetyCircleEntity)

    @Query("DELETE FROM safety_circle")
    suspend fun leaveCircle()
}
