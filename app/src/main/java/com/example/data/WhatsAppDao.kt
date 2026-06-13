package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WhatsAppDao {

    @Query("SELECT * FROM scheduled_messages ORDER BY scheduledTime ASC")
    fun getAllMessagesFlow(): Flow<List<ScheduledMessage>>

    @Query("SELECT * FROM scheduled_messages WHERE id = :id")
    suspend fun getMessageById(id: Int): ScheduledMessage?

    @Query("SELECT * FROM scheduled_messages WHERE status = 'PENDING' ORDER BY scheduledTime ASC")
    suspend fun getPendingMessages(): List<ScheduledMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ScheduledMessage): Long

    @Update
    suspend fun updateMessage(message: ScheduledMessage)

    @Delete
    suspend fun deleteMessage(message: ScheduledMessage)

    @Query("DELETE FROM scheduled_messages WHERE id = :id")
    suspend fun deleteMessageById(id: Int)
}
