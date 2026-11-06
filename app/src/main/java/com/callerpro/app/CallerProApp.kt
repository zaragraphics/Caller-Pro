package com.callerpro.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

class CallHistoryReceiver : BroadcastReceiver() {

    companion object {
        private const val PREFS_NAME = "caller_pro_call_state"
        private const val KEY_CALL_IN_PROGRESS = "call_in_progress"
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
                        Thread.sleep(2500)
                        CallHistorySync.syncFromPhoneCallLog(appContext)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                    } catch (_: Exception) {
                        // A later app refresh can retry synchronization.
                    } finally {
                        pendingResult.finish()
                    }
                }.start()
            }
        }
    }
}
