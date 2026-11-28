package com.callerpro.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager

class CallHistoryReceiver : BroadcastReceiver() {

    companion object {
        private const val PREFS_NAME = "caller_pro_call_state"
        private const val KEY_ACTIVE_SUBSCRIPTIONS = "active_call_subscriptions"
        private const val KEY_UNKNOWN_CALL_IN_PROGRESS = "unknown_subscription_call_in_progress"
        private const val RECORDING_LOOKBACK_MILLIS = 15 * 60 * 1000L
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            return
        }

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            ?: return
        val subscriptionId = intent.getIntExtra(
            SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX,
            SubscriptionManager.INVALID_SUBSCRIPTION_ID
        )

        val preferences = context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING,
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                if (subscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    val active = preferences.getStringSet(
                        KEY_ACTIVE_SUBSCRIPTIONS,
                        emptySet()
                    )?.toMutableSet() ?: mutableSetOf()
                    active.add(subscriptionId.toString())
                    preferences.edit()
                        .putStringSet(KEY_ACTIVE_SUBSCRIPTIONS, active)
                        .apply()
                } else {
                    preferences.edit()
                        .putBoolean(KEY_UNKNOWN_CALL_IN_PROGRESS, true)
                        .apply()
                }
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                val active = preferences.getStringSet(
                    KEY_ACTIVE_SUBSCRIPTIONS,
                    emptySet()
                )?.toMutableSet() ?: mutableSetOf()

                if (subscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    active.remove(subscriptionId.toString())
                } else {
                    preferences.edit()
                        .putBoolean(KEY_UNKNOWN_CALL_IN_PROGRESS, false)
                        .apply()
                }

                preferences.edit()
                    .putStringSet(KEY_ACTIVE_SUBSCRIPTIONS, active)
                    .apply()

                val unknownCallActive = preferences.getBoolean(
                    KEY_UNKNOWN_CALL_IN_PROGRESS,
                    false
                )
                // One SIM going idle must not trigger final sync while another
                // subscription still has an active or ringing call.
                if (active.isNotEmpty() || unknownCallActive) return

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

        val now = System.currentTimeMillis()
        val files = folder.listFiles()
            ?.filter { file ->
                val ageMillis = now - file.lastModified()
                file.isFile &&
                    file.extension.equals("m4a", ignoreCase = true) &&
                    file.length() > 1024L &&
                    ageMillis in 0..RECORDING_LOOKBACK_MILLIS
            }
            ?.sortedByDescending { it.lastModified() }
            ?: return

        if (files.isEmpty()) return

        val database = CallHistoryDatabase.getInstance(context)
        // Keep this set mutable during the pass: after one file is linked,
        // it must never be reconsidered for a second nearby call.
        val linkedFileNames = database.getAllCalls()
            .mapNotNull { it.recordingFileName }
            .toMutableSet()

        for (file in files) {
            if (!linkedFileNames.add(file.name)) continue

            // MediaRecorder finalizes the file when the call ends. Match that
            // timestamp to CallLog.DATE + DURATION, not to the call start time.
            val linked = database.linkRecordingToNearestCall(
                recordingEndMillis = file.lastModified(),
                recordingFileName = file.name
            )
            if (!linked) {
                linkedFileNames.remove(file.name)
            }
        }
    }
}
