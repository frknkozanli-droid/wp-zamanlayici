package com.example.ui

import android.app.AlarmManager
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ScheduledMessage
import com.example.data.WhatsAppRepository
import com.example.service.WhatsAppAccessibilityService
import com.example.service.WhatsAppSchedulerService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WhatsAppRepository(application)
    private val prefs = application.getSharedPreferences("whats_app_scheduler_prefs", Context.MODE_PRIVATE)

    val messages: StateFlow<List<ScheduledMessage>> = repository.allMessagesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    var isSchedulerActive by mutableStateOf(false)
        private set

    var isNotificationPermissionGranted by mutableStateOf(false)
    var isExactAlarmPermissionGranted by mutableStateOf(false)
    var isAccessibilityPermissionGranted by mutableStateOf(false)

    init {
        refreshSchedulerState()
    }

    fun refreshSchedulerState() {
        isSchedulerActive = prefs.getBoolean("scheduler_active", false)
        checkPermissions()
    }

    fun toggleScheduler(active: Boolean) {
        val context = getApplication<Application>()
        if (active) {
            WhatsAppSchedulerService.start(context)
        } else {
            WhatsAppSchedulerService.stop(context)
        }
        // Small delay to allow service to start and write preferences
        viewModelScope.launch {
            kotlinx.coroutines.delay(200)
            refreshSchedulerState()
        }
    }

    fun addScheduledMessage(contactName: String, phoneNumber: String, messageText: String, scheduledTime: Long) {
        viewModelScope.launch {
            val newMessage = ScheduledMessage(
                contactName = contactName,
                phoneNumber = phoneNumber,
                messageText = messageText,
                scheduledTime = scheduledTime,
                status = "PENDING"
            )
            repository.insertMessage(newMessage)
        }
    }

    fun addScheduledMessageWithStatus(contactName: String, phoneNumber: String, messageText: String, scheduledTime: Long, status: String) {
        viewModelScope.launch {
            val newMessage = ScheduledMessage(
                contactName = contactName,
                phoneNumber = phoneNumber,
                messageText = messageText,
                scheduledTime = scheduledTime,
                status = status
            )
            repository.insertMessage(newMessage)
        }
    }

    fun updateScheduledMessage(message: ScheduledMessage) {
        viewModelScope.launch {
            repository.updateMessage(message)
        }
    }

    fun deleteMessage(message: ScheduledMessage) {
        viewModelScope.launch {
            repository.deleteMessage(message)
        }
    }

    fun deleteMessageById(id: Int) {
        viewModelScope.launch {
            repository.deleteMessageById(id)
        }
    }

    fun checkPermissions() {
        val context = getApplication<Application>()

        // 1. Notification Permission (Android 13+)
        isNotificationPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        // 2. Exact Alarm Permission (Android 12+)
        isExactAlarmPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        // 3. Accessibility Service Permission
        isAccessibilityPermissionGranted = isAccessibilityServiceEnabled(context)
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponentName = ComponentName(context, WhatsAppAccessibilityService::class.java)
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = enabledServicesSetting.split(':')
        for (service in colonSplitter) {
            val enabledComponent = ComponentName.unflattenFromString(service)
            if (enabledComponent != null && enabledComponent == expectedComponentName) {
                return true
            }
        }
        return false
    }
}
