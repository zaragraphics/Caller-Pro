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
    private val trackedCalls = mutableListOf<Call>()
    private var preferredCall: Call? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        createChannel()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        if (trackedCalls.none { it === call }) {
            trackedCalls.add(call)
            call.registerCallback(callback)
        }
        refreshForegroundCall()
        openCallScreen()
    }

    override fun onCallRemoved(call: Call) {
        call.unregisterCallback(callback)
        trackedCalls.removeAll { it === call }
        if (preferredCall === call) preferredCall = null
        refreshForegroundCall()
        super.onCallRemoved(call)
    }

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            refreshForegroundCall()
        }

        override fun onDetailsChanged(call: Call, details: Call.Details) {
            refreshForegroundCall()
        }
    }

    fun getTrackedCalls(): List<Call> = trackedCalls.filter {
        it.state != Call.STATE_DISCONNECTED
    }.toList()

    fun selectCall(call: Call) {
        if (trackedCalls.any { it === call } && call.state != Call.STATE_DISCONNECTED) {
            preferredCall = call
            activeCall = call
            showCallNotification(call)
        }
    }

    private fun refreshForegroundCall() {
        if (preferredCall !in trackedCalls || preferredCall?.state == Call.STATE_DISCONNECTED) {
            preferredCall = null
        }
        val selected = preferredCall
            ?: trackedCalls.firstOrNull { it.state == Call.STATE_ACTIVE }
            ?: trackedCalls.firstOrNull { it.state == Call.STATE_RINGING }
            ?: trackedCalls.firstOrNull { it.state == Call.STATE_DIALING }
            ?: trackedCalls.firstOrNull { it.state == Call.STATE_CONNECTING }
            ?: trackedCalls.firstOrNull { it.state == Call.STATE_HOLDING }
            ?: trackedCalls.firstOrNull { it.state != Call.STATE_DISCONNECTED }

        activeCall = selected
        if (selected == null) {
            getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        } else {
            showCallNotification(selected)
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
        val caller = call.details?.handle?.schemeSpecificPart
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }.setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle(
                when (call.state) {
                    Call.STATE_RINGING -> "Incoming call"
                    Call.STATE_DIALING, Call.STATE_CONNECTING -> "Calling…"
                    Call.STATE_HOLDING -> "Call on hold"
                    else -> "Caller Pro call"
                }
            )
            .setContentText(caller ?: "Tap to open call controls")
            .setCategory(Notification.CATEGORY_CALL)
            .setPriority(Notification.PRIORITY_MAX)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .setFullScreenIntent(openIntent, true)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        trackedCalls.forEach { it.unregisterCallback(callback) }
        trackedCalls.clear()
        preferredCall = null
        activeCall = null
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
