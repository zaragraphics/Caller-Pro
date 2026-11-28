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
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.TelephonyCallback
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
    // Recorder writes to a pending file; only validated recordings get the .m4a extension.
    private var outputFile: File? = null
    private var finalOutputFile: File? = null
    private val legacyTelephonyListeners = mutableMapOf<Int, Pair<TelephonyManager, PhoneStateListener>>()
    private val telephonyCallbacks = mutableMapOf<Int, Pair<TelephonyManager, TelephonyCallback>>()
    private val activeSubscriptionCalls = mutableSetOf<Int>()
    private var recordingStarted = false
    private var lastRecordingSaved: Boolean? = null
    @Volatile private var recorderReportedError = false

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
        cleanupAbandonedRecordings()
    }

    private fun cleanupAbandonedRecordings() {
        val folder = RecordingStorage.getRecordingsFolder(this) ?: return
        folder.listFiles()
            ?.filter { it.isFile && it.name.startsWith("CallerPro_") && it.name.endsWith(".m4a.pending") }
            ?.forEach { file ->
                try { file.delete() } catch (_: Exception) {}
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopMonitoring()
            return START_NOT_STICKY
        }

        if (legacyTelephonyListeners.isNotEmpty() || telephonyCallbacks.isNotEmpty()) return START_NOT_STICKY

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
        val defaultManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val subscriptionManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager

        val subscriptionIds = try {
            subscriptionManager.activeSubscriptionInfoList
                ?.map { it.subscriptionId }
                ?.distinct()
                .orEmpty()
        } catch (_: SecurityException) {
            emptyList()
        }

        // Keep one registration per active SIM so dual-SIM calls are tracked independently.
        val targets = if (subscriptionIds.isNotEmpty()) {
            subscriptionIds.map { it to defaultManager.createForSubscriptionId(it) }
        } else {
            listOf(SubscriptionManager.INVALID_SUBSCRIPTION_ID to defaultManager)
        }

        var failedSubscriptionCount = 0

        for ((subscriptionId, manager) in targets) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val callback = object : TelephonyCallback(),
                        TelephonyCallback.CallStateListener {
                        override fun onCallStateChanged(state: Int) {
                            handleCallStateChanged(subscriptionId, state)
                        }
                    }
                    telephonyCallbacks[subscriptionId] = manager to callback
                    manager.registerTelephonyCallback(mainExecutor, callback)
                } else {
                    @Suppress("DEPRECATION")
                    val listener = object : PhoneStateListener() {
                        @Deprecated("Deprecated in Android 12; retained for API 26–30 compatibility")
                        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                            handleCallStateChanged(subscriptionId, state)
                        }
                    }
                    legacyTelephonyListeners[subscriptionId] = manager to listener
                    @Suppress("DEPRECATION")
                    manager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
                }
            } catch (_: Exception) {
                legacyTelephonyListeners.remove(subscriptionId)
                telephonyCallbacks.remove(subscriptionId)
                failedSubscriptionCount++
            }
        }

        if (legacyTelephonyListeners.isEmpty() && telephonyCallbacks.isEmpty()) {
            throw IllegalStateException("Unable to register call-state listeners")
        }

        if (failedSubscriptionCount > 0) {
            getSystemService(NotificationManager::class.java).notify(
                NOTIFICATION_ID,
                buildNotification(
                    "Monitoring limited • unable to monitor $failedSubscriptionCount SIM subscription(s)"
                )
            )
        }
    }

    private fun handleCallStateChanged(subscriptionId: Int, state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                activeSubscriptionCalls.add(subscriptionId)
                if (!recordingStarted) startRecording()
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                activeSubscriptionCalls.remove(subscriptionId)
                // Keep the shared recorder alive until every monitored SIM is idle.
                if (activeSubscriptionCalls.isEmpty() && recordingStarted) {
                    stopRecording()
                    val resultMessage = when {
                        lastRecordingSaved == false -> "Recording failed • no usable audio file was saved"
                        lastRecordingSaved == true && lastRecordingDetectedAudio == true -> "Recording saved • microphone signal detected"
                        lastRecordingSaved == true -> "Recording saved • no microphone signal detected"
                        else -> "Monitoring enabled • ready for next call"
                    }
                    getSystemService(NotificationManager::class.java).notify(
                        NOTIFICATION_ID,
                        buildNotification(resultMessage)
                    )
                }
            }
        }
    }

    private fun startRecording() {
        if (!hasPermission(android.Manifest.permission.RECORD_AUDIO)) {
            lastRecordingSaved = false
            return
        }
        val folder = RecordingStorage.getRecordingsFolder(this) ?: run {
            lastRecordingSaved = false
            return
        }
        // Milliseconds prevent two calls started in the same second from colliding.
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val file = File(folder, "CallerPro_$stamp.m4a.pending")
        val completedFile = File(folder, "CallerPro_$stamp.m4a")
        var candidate: MediaRecorder? = null

        try {
            candidate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            candidate.setOnErrorListener { _, what, extra ->
                recorderReportedError = true
                getSystemService(NotificationManager::class.java).notify(
                    NOTIFICATION_ID,
                    buildNotification("Recording error • Android audio capture stopped (code $what/$extra)")
                )
            }
            candidate.setOnInfoListener { _, what, extra ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
                    what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED
                ) {
                    recorderReportedError = true
                    getSystemService(NotificationManager::class.java).notify(
                        NOTIFICATION_ID,
                        buildNotification("Recording limit reached • this recording may be incomplete")
                    )
                }
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
            finalOutputFile = completedFile
            recordingStarted = true
            silentSampleCount = 0
            recordingDetectedAudio = false
            silenceWarningShown = false
            lastRecordingDetectedAudio = null
            lastRecordingSaved = null
            recorderReportedError = false
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
            finalOutputFile = null
            lastRecordingDetectedAudio = null
            lastRecordingSaved = false
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
        val pendingFile = outputFile
        val completedFile = finalOutputFile
        var stopSucceeded = false
        if (recordingStarted) {
            lastRecordingDetectedAudio = recordingDetectedAudio
        }
        try {
            if (currentRecorder != null) {
                currentRecorder.stop()
                stopSucceeded = true
            }
        } catch (_: Exception) {
            stopSucceeded = false
        } finally {
            try { currentRecorder?.reset() } catch (_: Exception) {}
            try { currentRecorder?.release() } catch (_: Exception) {}
            recorder = null
            recordingStarted = false
            outputFile = null
            finalOutputFile = null
        }

        // Keep unfinished captures out of the library and call-history linker.
        val validPendingFile = stopSucceeded && !recorderReportedError &&
            pendingFile?.isFile == true && pendingFile.length() > 1024L
        val finalized = validPendingFile &&
            completedFile != null &&
            !completedFile.exists() &&
            try { pendingFile.renameTo(completedFile) } catch (_: Exception) { false }

        lastRecordingSaved = finalized
        if (!finalized) {
            try { pendingFile?.delete() } catch (_: Exception) {}
            try { completedFile?.delete() } catch (_: Exception) {}
        }
    }

    private fun unregisterCallStateListeners() {
        legacyTelephonyListeners.values.forEach { (manager, listener) ->
            try {
                @Suppress("DEPRECATION")
                manager.listen(listener, PhoneStateListener.LISTEN_NONE)
            } catch (_: Exception) {
            }
        }
        legacyTelephonyListeners.clear()

        telephonyCallbacks.values.forEach { (manager, callback) ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    manager.unregisterTelephonyCallback(callback)
                }
            } catch (_: Exception) {
            }
        }
        telephonyCallbacks.clear()
        activeSubscriptionCalls.clear()
    }

    private fun stopMonitoring() {
        stopRecording()
        unregisterCallStateListeners()
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
        unregisterCallStateListeners()
        stopRecording()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
