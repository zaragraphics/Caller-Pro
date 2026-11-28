package com.callerpro.app

import android.app.InCallService
import android.telecom.Call
import android.content.Intent
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import android.util.Log

class CallerProInCallService : InCallService() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        createChannel()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        activeCall = call
        call.registerCallback(callback)
        showCallNotification(call)
        openCallScreen()
    }

    override fun onCallRemoved(call: Call) {
        call.unregisterCallback(callback)
        if (activeCall === call) activeCall = null
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        super.onCallRemoved(call)
    }

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            showCallNotification(call)
            InCallActivity.refreshRequested = true
        }
    }

    private fun openCallScreen() {
        try {
            startActivity(Intent(this, InCallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
        } catch (e: Exception) {
            Log.w(TAG, "Call UI will be available from the call notification", e)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Phone calls", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Incoming and ongoing call controls"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun showCallNotification(call: Call) {
        val openIntent = PendingIntent.getActivity(
            this, 10, Intent(this, InCallActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }.setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle(if (call.state == Call.STATE_RINGING) "Incoming call" else "Caller Pro call")
            .setContentText("Tap to open call controls")
            .setCategory(Notification.CATEGORY_CALL)
            .setPriority(Notification.PRIORITY_MAX)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .setFullScreenIntent(openIntent, true)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "CallerProInCall"
        private const val CHANNEL_ID = "caller_pro_calls"
        private const val NOTIFICATION_ID = 7301
        @Volatile var instance: CallerProInCallService? = null
            private set
        @Volatile var activeCall: Call? = null
            private set
    }
}
