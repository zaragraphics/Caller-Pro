package com.callerpro.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

class CallHistoryReceiver : BroadcastReceiver() {

    companion object {
        private const val PREFS_NAME = "caller_pro_call_state"
        private const val KEY_CALL_IN_PROGRESS = "call_in_progress"
        private const val RECORDING_LOOKBACK_MILLIS = 15 * 60 * 1000L
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            return
        }

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            ?: return

        val preferences = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING,
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                preferences.edit()
                    .putBoolean(KEY_CALL_IN_PROGRESS, true)
                    .apply()
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                val wasInCall = preferences.getBoolean(
                    KEY_CALL_IN_PROGRESS,
                    false
                )

                if (!wasInCall) return

                preferences.edit()
                    .putBoolean(KEY_CALL_IN_PROGRESS, false)
                    .apply()

                val pendingResult = goAsync()
                val appContext = context.applicationContext

                Thread {
                    try {
                        // Allow the phone app to finish writing its call-log entry.
                        Thread.sleep(2500)

                        CallHistorySync.syncFromPhoneCallLog(appContext)
                        linkRecentRecordingsToCalls(appContext)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                    } catch (_: Exception) {
                        // A later app refresh can retry call-history synchronization.
                    } finally {
                        pendingResult.finish()
                    }
                }.start()
            }
        }
    }

    private fun linkRecentRecordingsToCalls(context: Context) {
        val folder = RecordingStorage.getRecordingsFolder(context)
            ?: return

        val files = folder.listFiles()
            ?.filter { file ->
                file.isFile &&
                    file.extension.equals("m4a", ignoreCase = true) &&
                    System.currentTimeMillis() - file.lastModified() <=
                    RECORDING_LOOKBACK_MILLIS
            }
            ?.sortedByDescending { it.lastModified() }
            ?: return

        if (files.isEmpty()) return

        val database = CallHistoryDatabase.getInstance(context)
        val alreadyLinked = database.getAllCalls()
            .mapNotNull { it.recordingFileName }
            .toSet()

        for (file in files) {
            if (file.name in alreadyLinked) continue

            database.linkRecordingToNearestCall(
                callDateMillis = file.lastModified(),
                recordingFileName = file.name
            )
        }
    }
}
