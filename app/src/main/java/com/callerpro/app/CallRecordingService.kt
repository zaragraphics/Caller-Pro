
package com.callerpro.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallRecordingService : Service() {

    companion object {
        const val ACTION_STOP = "com.callerpro.app.STOP_CALL_RECORDING"
        private const val CHANNEL_ID = "caller_pro_recording"
        private const val NOTIFICATION_ID = 7101
    }

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var telephonyManager: TelephonyManager? = null
    private var phoneListener: PhoneStateListener? = null
    private var callHasConnected = false
    private var recordingStarted = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        if (intent?.action == ACTION_STOP) {
            stopRecordingAndService()
            return START_NOT_STICKY
        }

        if (phoneListener != null) return START_NOT_STICKY

        try {
            val notification = buildNotification(
                "Ready for your next call"
            )

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
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun registerCallStateListener() {
        val manager =
            getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        telephonyManager = manager

        val listener = object : PhoneStateListener() {
            override fun onCallStateChanged(
                state: Int,
                phoneNumber: String?
            ) {
                when (state) {
                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        callHasConnected = true
                        if (!recordingStarted) startRecording()
                    }

                    TelephonyManager.CALL_STATE_IDLE -> {
                        if (callHasConnected) {
                            stopRecordingAndService()
                        }
                    }
                }
            }
        }

        phoneListener = listener
        manager.listen(
            listener,
            PhoneStateListener.LISTEN_CALL_STATE
        )
    }

    private fun startRecording() {
        val folder =
            RecordingStorage.getRecordingsFolder(this) ?: return

        val stamp = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.US
        ).format(Date())

        val file = File(folder, "CallerPro_$stamp.m4a")

        try {
            val mediaRecorder =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(this)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }

            mediaRecorder.setAudioSource(
                MediaRecorder.AudioSource.MIC
            )
            mediaRecorder.setOutputFormat(
                MediaRecorder.OutputFormat.MPEG_4
            )
            mediaRecorder.setAudioEncoder(
                MediaRecorder.AudioEncoder.AAC
            )
            mediaRecorder.setAudioEncodingBitRate(128000)
            mediaRecorder.setAudioSamplingRate(44100)
            mediaRecorder.setOutputFile(file.absolutePath)

            mediaRecorder.prepare()
            mediaRecorder.start()

            recorder = mediaRecorder
            outputFile = file
            recordingStarted = true

            getSystemService(NotificationManager::class.java)
                .notify(
                    NOTIFICATION_ID,
                    buildNotification("Recording is active")
                )

        } catch (_: Exception) {
            try {
                recorder?.release()
            } catch (_: Exception) {
            }

            recorder = null
            file.delete()
            stopRecordingAndService()
        }
    }

    private fun stopRecordingAndService() {
        stopRecording()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopRecording() {
        try {
            recorder?.stop()
        } catch (_: Exception) {
            outputFile?.delete()
        }

        try {
            recorder?.reset()
            recorder?.release()
        } catch (_: Exception) {
        }

        recorder = null
        recordingStarted = false
    }

    private fun buildNotification(message: String): Notification {
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(
            this,
            CallRecordingService::class.java
        ).apply {
            action = ACTION_STOP
        }

        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Caller Pro")
            .setContentText(message)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(
                NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
            )
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop",
                stopPendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Call recording",
                NotificationManager.IMPORTANCE_LOW
            )

            channel.description =
                "Silent, compact Caller Pro recording status"

            channel.setSound(null, null)
            channel.enableVibration(false)
            channel.enableLights(false)
            channel.setShowBadge(false)

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        try {
            phoneListener?.let {
                telephonyManager?.listen(
                    it,
                    PhoneStateListener.LISTEN_NONE
                )
            }
        } catch (_: Exception) {
        }

        stopRecording()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
