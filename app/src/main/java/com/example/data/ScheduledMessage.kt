package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scheduled_messages")
data class ScheduledMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contactName: String,
    val phoneNumber: String,
    val messageText: String,
    val scheduledTime: Long,
    val status: String = "PENDING", // PENDING, SENT, FAILED
    val errorMessage: String? = null
)
