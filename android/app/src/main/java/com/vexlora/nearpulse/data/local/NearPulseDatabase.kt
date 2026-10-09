package com.vexlora.nearpulse.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MessageEntity::class, PeerEntity::class, SafetyCircleEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NearPulseDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun peerDao(): PeerDao
    abstract fun safetyCircleDao(): SafetyCircleDao

    companion object {
        @Volatile
        private var INSTANCE: NearPulseDatabase? = null

        fun getDatabase(context: Context): NearPulseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NearPulseDatabase::class.java,
                    "vexlora_nearpulse.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
