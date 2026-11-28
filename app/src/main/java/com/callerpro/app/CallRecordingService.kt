package com.callerpro.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallRecordingService : Service() {

    companion object {
        const val ACTION_STOP = "com.callerpro.app.STOP_CALL_RECORDING"
        const val PREFS_NAME = "caller_pro_recording"
        const val PREF_MONITOR_ENABLED = "monitor_enabled"
        private const val CHANNEL_ID = "caller_pro_recording"
        private const val NOTIFICATION_ID = 7101
        private const val AMPLITUDE_CHECK_INTERVAL_MS = 1500L
        private const val SILENT_SAMPLE_LIMIT = 8
    }

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var telephonyManager: TelephonyManager? = null
    private var phoneListener: PhoneStateListener? = null
    private var callHasConnected = false
    private var recordingStarted = false

    private val amplitudeHandler = Handler(Looper.getMainLooper())
    private var silentSampleCount = 0
    private var recordingDetectedAudio = false
    private var silenceWarningShown = false
    private var lastRecordingDetectedAudio: Boolean? = null

    private val amplitudeCheck = object : Runnable {
        override fun run() {
            val activeRecorder = recorder ?: return
            val amplitude = try {
                activeRecorder.maxAmplitude
            } catch (_: Exception) {
                0
            }

            if (amplitude > 0) {
                recordingDetectedAudio = true
                silentSampleCount = 0
            } else {
                silentSampleCount += 1
                if (silentSampleCount >= SILENT_SAMPLE_LIMIT && !silenceWarningShown) {
                    silenceWarningShown = true
                    getSystemService(NotificationManager::class.java).notify(
                        NOTIFICATION_ID,
                        buildNotification(
                            "No microphone signal • Android may restrict mic access during calls"
                        )
                    )
                }
            }

            if (recordingStarted) {
                amplitudeHandler.postDelayed(this, AMPLITUDE_CHECK_INTERVAL_MS)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopMonitoring()
            return START_NOT_STICKY
        }

        if (phoneListener != null) return START_NOT_STICKY

        if (!hasPermission(android.Manifest.permission.RECORD_AUDIO) ||
            !hasPermission(android.Manifest.permission.READ_PHONE_STATE)
        ) {
            clearMonitorPreference()
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            val notification = buildNotification("Monitoring enabled • waiting for a call")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            registerCallStateListener()
        } catch (_: Exception) {
            clearMonitorPreference()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun registerCallStateListener() {
        val manager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager = manager

        val listener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                when (state) {
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        callHasConnected = true
                        if (!recordingStarted) startRecording()
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {
                        if (callHasConnected) {
                            stopRecording()
                            callHasConnected = false
                            val resultMessage = when (lastRecordingDetectedAudio) {
                                true -> "Recording saved • microphone signal detected"
                                false -> "Recording saved • no microphone signal detected"
                                null -> "Monitoring enabled • ready for next call"
                            }
                            getSystemService(NotificationManager::class.java).notify(
                                NOTIFICATION_ID,
                                buildNotification(resultMessage)
                            )
                        }
                    }
                }
            }
        }

        phoneListener = listener
        manager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    private fun startRecording() {
        if (!hasPermission(android.Manifest.permission.RECORD_AUDIO)) return
        val folder = RecordingStorage.getRecordingsFolder(this) ?: return
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(folder, "CallerPro_$stamp.m4a")
        var candidate: MediaRecorder? = null

        try {
            candidate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            candidate.setAudioSource(MediaRecorder.AudioSource.MIC)
            candidate.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            candidate.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            candidate.setAudioEncodingBitRate(128000)
            candidate.setAudioSamplingRate(44100)
            candidate.setOutputFile(file.absolutePath)
            candidate.prepare()
            candidate.start()

            recorder = candidate
            outputFile = file
            recordingStarted = true
            silentSampleCount = 0
            recordingDetectedAudio = false
            silenceWarningShown = false
            lastRecordingDetectedAudio = null
            amplitudeHandler.removeCallbacks(amplitudeCheck)
            amplitudeHandler.postDelayed(amplitudeCheck, AMPLITUDE_CHECK_INTERVAL_MS)

            getSystemService(NotificationManager::class.java).notify(
                NOTIFICATION_ID,
                buildNotification("Recording active • checking microphone input")
            )
        } catch (_: Exception) {
            try { candidate?.reset() } catch (_: Exception) {}
            try { candidate?.release() } catch (_: Exception) {}
            recorder = null
            recordingStarted = false
            outputFile = null
            lastRecordingDetectedAudio = null
            amplitudeHandler.removeCallbacks(amplitudeCheck)
            file.delete()
            getSystemService(NotificationManager::class.java).notify(
                NOTIFICATION_ID,
                buildNotification("Recording unavailable • monitoring continues")
            )
        }
    }

    private fun stopRecording() {
        amplitudeHandler.removeCallbacks(amplitudeCheck)
        val currentRecorder = recorder
        if (recordingStarted) {
            lastRecordingDetectedAudio = recordingDetectedAudio
        }
        try {
            currentRecorder?.stop()
        } catch (_: Exception) {
            outputFile?.delete()
        } finally {
            try { currentRecorder?.reset() } catch (_: Exception) {}
            try { currentRecorder?.release() } catch (_: Exception) {}
            recorder = null
            recordingStarted = false
            outputFile = null
        }
    }

    private fun stopMonitoring() {
        stopRecording()
        try {
            phoneListener?.let { listener ->
                telephonyManager?.listen(listener, PhoneStateListener.LISTEN_NONE)
            }
        } catch (_: Exception) {
        }
        phoneListener = null
        telephonyManager = null
        callHasConnected = false
        clearMonitorPreference()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun clearMonitorPreference() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_MONITOR_ENABLED, false)
            .apply()
    }

    private fun buildNotification(message: String): Notification {
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = Intent(this, CallRecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Caller Pro")
            .setContentText(message)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(android.R.drawable.ic_media_pause, "Stop monitoring", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Call recording", NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Visible status for Caller Pro call monitoring and recording"
            channel.setSound(null, null)
            channel.enableVibration(false)
            channel.enableLights(false)
            channel.setShowBadge(false)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        amplitudeHandler.removeCallbacks(amplitudeCheck)
        try {
            phoneListener?.let { telephonyManager?.listen(it, PhoneStateListener.LISTEN_NONE) }
        } catch (_: Exception) {
        }
        stopRecording()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
