package com.example.data

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.receiver.AlarmReceiver
import kotlinx.coroutines.flow.Flow

class WhatsAppRepository(private val context: Context) {

    private val db = WhatsAppDatabase.getDatabase(context)
    private val dao = db.whatsAppDao()
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val allMessagesFlow: Flow<List<ScheduledMessage>> = dao.getAllMessagesFlow()

    suspend fun getMessageById(id: Int): ScheduledMessage? = dao.getMessageById(id)

    suspend fun insertMessage(message: ScheduledMessage): Int {
        val id = dao.insertMessage(message).toInt()
        val savedMessage = message.copy(id = id)
        if (isSchedulerActive() && savedMessage.status == "PENDING" && savedMessage.scheduledTime > System.currentTimeMillis()) {
            scheduleAlarm(savedMessage)
        }
        return id
    }

    suspend fun updateMessage(message: ScheduledMessage) {
        dao.updateMessage(message)
        if (isSchedulerActive()) {
            if (message.status == "PENDING" && message.scheduledTime > System.currentTimeMillis()) {
                scheduleAlarm(message)
            } else {
                cancelAlarm(message.id)
            }
        }
    }

    suspend fun deleteMessage(message: ScheduledMessage) {
        dao.deleteMessage(message)
        cancelAlarm(message.id)
    }

    suspend fun deleteMessageById(id: Int) {
        dao.deleteMessageById(id)
        cancelAlarm(id)
    }

    suspend fun scheduleAllPending() {
        val pending = dao.getPendingMessages()
        val currentTime = System.currentTimeMillis()
        for (message in pending) {
            if (message.scheduledTime > currentTime) {
                scheduleAlarm(message)
            } else {
                // If it was scheduled for the past while the service was off, mark it as failed
                dao.updateMessage(message.copy(status = "FAILED", errorMessage = "Zamanlayıcı kapalıyken süresi geçti"))
            }
        }
    }

    suspend fun cancelAllAlarms() {
        val pending = dao.getPendingMessages()
        for (message in pending) {
            cancelAlarm(message.id)
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleAlarm(message: ScheduledMessage) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("MESSAGE_ID", message.id)
            // Add action to ensure intent is unique
            action = "com.example.ACTION_SEND_WP_MESSAGE_${message.id}"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            message.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        message.scheduledTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        message.scheduledTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    message.scheduledTime,
                    pendingIntent
                )
            }
            Log.d("WhatsAppRepository", "Alarms scheduled for msg ID: ${message.id} at ${message.scheduledTime}")
        } catch (e: Exception) {
            Log.e("WhatsAppRepository", "Failed to schedule alarm", e)
        }
    }

    private fun cancelAlarm(messageId: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.ACTION_SEND_WP_MESSAGE_$messageId"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            messageId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("WhatsAppRepository", "Alarm canceled for msg ID: $messageId")
    }

    private fun isSchedulerActive(): Boolean {
        val prefs = context.getSharedPreferences("whats_app_scheduler_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("scheduler_active", false)
    }
}
