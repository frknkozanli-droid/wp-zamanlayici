package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ScheduledMessage::class], version = 1, exportSchema = false)
abstract class WhatsAppDatabase : RoomDatabase() {

    abstract fun whatsAppDao(): WhatsAppDao

    companion object {
        @Volatile
        private var INSTANCE: WhatsAppDatabase? = null

        fun getDatabase(context: Context): WhatsAppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WhatsAppDatabase::class.java,
                    "whatsapp_scheduler_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
