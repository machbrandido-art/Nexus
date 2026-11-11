package com.nexus.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProviderEntity::class, ConversationEntity::class, MessageEntity::class, KanbanCardEntity::class],
    version = 2,
    exportSchema = true
)
abstract class NexusDatabase : RoomDatabase() {
    abstract fun providerDao(): ProviderDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun kanbanDao(): KanbanDao

    companion object {
        fun build(context: Context): NexusDatabase =
            Room.databaseBuilder(context.applicationContext, NexusDatabase::class.java, "nexus.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}