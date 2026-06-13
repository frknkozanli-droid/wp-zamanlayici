package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.ScheduledMessage
import com.example.data.WhatsAppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getIntExtra("MESSAGE_ID", -1)
        Log.d("AlarmReceiver", "Alarm triggered for message ID: $messageId")

        if (messageId == -1) return

        val prefs = context.getSharedPreferences("whats_app_scheduler_prefs", Context.MODE_PRIVATE)
        val isSchedulerActive = prefs.getBoolean("scheduler_active", false)

        if (!isSchedulerActive) {
            Log.d("AlarmReceiver", "Scheduler is inactive, skipping send.")
            return
        }

        val repository = WhatsAppRepository(context)
        val goAsync = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val message = repository.getMessageById(messageId)
                if (message != null && message.status == "PENDING") {
                    // Pre-fill last auto send timestamp to let the Accessibility service click send safely
                    prefs.edit().putLong("last_auto_send_trigger", System.currentTimeMillis()).apply()

                    // Try to launch WhatsApp immediately
                    var launchedDirectly = false
                    try {
                        val wpIntent = createWhatsAppIntent(message.phoneNumber, message.messageText)
                        context.startActivity(wpIntent)
                        launchedDirectly = true
                        Log.d("AlarmReceiver", "Directly launched WhatsApp activity for msg: $messageId")
                    } catch (e: Exception) {
                        Log.e("AlarmReceiver", "Unable to start WhatsApp activity directly from background", e)
                    }

                    // Show notification in any case so user sees the message status, and can manual retry
                    showAlertNotification(context, message, launchedDirectly)

                    // Mark as SENT
                    repository.updateMessage(message.copy(
                        status = "SENT",
                        errorMessage = if (launchedDirectly) "Başlatıldı (Otomatik)" else "Bildirim Gönderildi (Tıklayarak Açın)"
                    ))
                }
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Error processing alarm", e)
            } finally {
                goAsync.finish()
            }
        }
    }

    private fun createWhatsAppIntent(phone: String, text: String): Intent {
        val cleanPhone = phone.replace("+", "").replace(" ", "").replace("-", "")
        val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(text)}"
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
    }

    private fun showAlertNotification(context: Context, message: ScheduledMessage, autoLaunched: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "whatsapp_alert_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "WhatsApp Mesaj Zamanlayıcı Uyarıları",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Zamanlanmış mesajlar tetiklendiğinde gösterilen yüksek öncelikli bildirimler."
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Action Intent to open WhatsApp directly
        val wpIntent = createWhatsAppIntent(message.phoneNumber, message.messageText)
        val wpPendingIntent = PendingIntent.getActivity(
            context,
            message.id,
            wpIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Info text
        val infoText = if (autoLaunched) {
            "${message.contactName} kişisine mesaj otomatik olarak hazırlanıp WhatsApp üzerinde tetiklendi."
        } else {
            "${message.contactName} kişisine mesaj gönderme vakti geldi! Göndermek için dokunun."
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Zamanlanmış Mesaj Gönderimi")
            .setContentText(infoText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(wpPendingIntent)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_send,
                "Şimdi Gönder (WhatsApp)",
                wpPendingIntent
            )
            .build()

        notificationManager.notify(message.id + 10000, notification)
    }
}
