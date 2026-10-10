package com.vexlora.nearpulse.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val peerAddress: String,
    val senderPseudonym: String,
    val text: String,
    val timestamp: Long,
    val isOutgoing: Boolean,
    val isDelivered: Boolean
)

@Entity(tableName = "peers")
data class PeerEntity(
    @PrimaryKey
    val address: String,
    val pseudonym: String,
    val lastSeenTimestamp: Long,
    val lastRssi: Int,
    val circleCode: String = "GENERAL",
    val isTrusted: Boolean = false
)

@Entity(tableName = "safety_circles")
data class SafetyCircleEntity(
    @PrimaryKey
    val circleCode: String,
    val circleName: String,
    val memberCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)
