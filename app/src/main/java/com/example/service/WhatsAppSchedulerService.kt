package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.WhatsAppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class WhatsAppSchedulerService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var repository: WhatsAppRepository

    override fun onCreate() {
        super.onCreate()
        repository = WhatsAppRepository(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d("WhatsAppSchedulerService", "Service started on action: $action")

        if (action == ACTION_STOP_SERVICE) {
            stopServiceAndScheduler()
            return START_NOT_STICKY
        }

        // Default or ACTION_START_SERVICE
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ requires a category or special designation, but let's make it standard
            startForeground(NOTIFICATION_ID, notification)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Set master preference to true
        updateSchedulerPrefs(true)

        // Reschedule everything
        scope.launch {
            repository.scheduleAllPending()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun stopServiceAndScheduler() {
        updateSchedulerPrefs(false)
        scope.launch {
            repository.cancelAllAlarms()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun updateSchedulerPrefs(active: Boolean) {
        val prefs = getSharedPreferences("whats_app_scheduler_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("scheduler_active", active).apply()
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, WhatsAppSchedulerService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WhatsApp Zamanlayıcı Aktif")
            .setContentText("Mesajlarınız belirlenen saatte otomatik gönderilecektir.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(mainPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Hizmeti Durdur",
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "WhatsApp Zamanlayıcı Arka Plan Hizmeti",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Uygulamanın arka planda aktif kalmasını sağlayan sistem bildirim kanalı."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_START_SERVICE = "com.example.service.action.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.service.action.STOP_SERVICE"
        const val CHANNEL_ID = "whatsapp_scheduler_channel"
        const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, WhatsAppSchedulerService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, WhatsAppSchedulerService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
