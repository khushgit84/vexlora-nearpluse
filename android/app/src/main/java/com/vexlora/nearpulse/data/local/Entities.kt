package com.vexlora.nearpulse.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val peerAddress: String,
    val senderPseudonym: String,
    val content: String,
    val timestamp: Long,
    val isOutgoing: Boolean,
    val status: String,
    val isSos: Boolean = false
)

@Entity(tableName = "peers")
data class PeerEntity(
    @PrimaryKey val address: String,
    val pseudonym: String,
    val lastRssi: Int,
    val lastSeenTimestamp: Long,
    val circleCode: String?,
    val isFavorite: Boolean = false
)

@Entity(tableName = "safety_circle")
data class SafetyCircleEntity(
    @PrimaryKey val circleCode: String,
    val circleName: String,
    val createdAt: Long,
    val memberCount: Int
)
