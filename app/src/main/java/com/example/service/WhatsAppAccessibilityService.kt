package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class WhatsAppAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val rootNode = rootInActiveWindow ?: return
        val packageName = event.packageName?.toString() ?: ""

        if (packageName != "com.whatsapp") return

        // Check if we got triggered recently by our scheduler
        val prefs = getSharedPreferences("whats_app_scheduler_prefs", Context.MODE_PRIVATE)
        val lastTriggerTime = prefs.getLong("last_auto_send_trigger", 0L)
        val timeDiff = System.currentTimeMillis() - lastTriggerTime

        // Allow up to 15 seconds for WhatsApp to open and load the text input
        if (timeDiff > 15000) {
            return
        }

        // Run search in the active node tree to find and click the green Send button
        val clicked = findAndClickSendButton(rootNode)
        if (clicked) {
            Log.d("WP_Accessibility", "Message sent successfully using automation.")
        }
    }

    private fun findAndClickSendButton(node: AccessibilityNodeInfo): Boolean {
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""

        // Matches WhatsApp standard send button:
        // - ID matching "com.whatsapp:id/send"
        // - Content descriptions like "gönder", "send"
        if (viewId.contains("com.whatsapp:id/send") ||
            contentDesc == "gönder" ||
            contentDesc == "send" ||
            contentDesc == "gönderin"
        ) {
            if (node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d("WP_Accessibility", "Send button clicked!")
                resetTriggerTime()
                return true
            } else {
                // Sometime the image itself is nested in a clickable wrapper
                var parent = node.parent
                while (parent != null) {
                    if (parent.isClickable) {
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        Log.d("WP_Accessibility", "Click action delegated to parent view!")
                        resetTriggerTime()
                        return true
                    }
                    parent = parent.parent
                }
            }
        }

        // Breadth-first / Depth-first recursive exploration
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                if (findAndClickSendButton(child)) {
                    return true
                }
            }
        }
        return false
    }

    private fun resetTriggerTime() {
        val prefs = getSharedPreferences("whats_app_scheduler_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_auto_send_trigger", 0L).apply()
    }

    override fun onInterrupt() {
        Log.d("WP_Accessibility", "Accessibility service interrupted.")
    }
}
