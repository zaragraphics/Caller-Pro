
package com.callerpro.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog

data class CallHistorySyncResult(
    val savedCount: Int,
    val errorMessage: String? = null
)

object CallHistorySync {

    /**
     * Copies currently available phone call logs into
     * Caller Pro's independent database.
     *
     * Existing Caller Pro entries are never deleted.
     *
     * Call this function on a background thread.
     */
    fun syncFromPhoneCallLog(
        context: Context
    ): CallHistorySyncResult {

        if (
            context.checkSelfPermission(
                Manifest.permission.READ_CALL_LOG
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return CallHistorySyncResult(
                savedCount = 0,
                errorMessage = "Call Log permission is not granted."
            )
        }

        val database = CallHistoryDatabase.getInstance(context)

        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )

        var savedCount = 0

        return try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            ) ?: return CallHistorySyncResult(
                savedCount = 0,
                errorMessage = "Unable to read phone call history."
            )

            cursor.use {
                val idIndex =
                    it.getColumnIndexOrThrow(CallLog.Calls._ID)

                val nameIndex =
                    it.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)

                val numberIndex =
                    it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)

                val typeIndex =
                    it.getColumnIndexOrThrow(CallLog.Calls.TYPE)

                val dateIndex =
                    it.getColumnIndexOrThrow(CallLog.Calls.DATE)

                val durationIndex =
                    it.getColumnIndexOrThrow(CallLog.Calls.DURATION)

                while (it.moveToNext()) {
                    val sourceId = it.getLong(idIndex)

                    val savedName = it.getString(nameIndex)
                    val savedNumber = it.getString(numberIndex)

                    val callType = when (it.getInt(typeIndex)) {
                        CallLog.Calls.INCOMING_TYPE -> "Incoming"
                        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                        CallLog.Calls.MISSED_TYPE -> "Missed"
                        CallLog.Calls.REJECTED_TYPE -> "Rejected"
                        CallLog.Calls.BLOCKED_TYPE -> "Blocked"
                        CallLog.Calls.VOICEMAIL_TYPE -> "Voicemail"
                        else -> "Other"
                    }

                    val dateMillis = it.getLong(dateIndex)
                    val durationSeconds = it.getLong(durationIndex)

                    val record = CallHistoryRecord(
                        sourceCallLogId = sourceId,
                        name = savedName
                            ?.takeIf { name -> name.isNotBlank() }
                            ?: "Unknown caller",
                        number = savedNumber
                            ?.takeIf { number -> number.isNotBlank() }
                            ?: "Unknown number",
                        type = callType,
                        dateMillis = dateMillis,
                        durationSeconds = durationSeconds
                    )

                    val result = database.saveCall(record)

                    if (result > 0) {
                        savedCount++
                    }
                }
            }

            CallHistorySyncResult(savedCount = savedCount)

        } catch (_: SecurityException) {
            CallHistorySyncResult(
                savedCount = savedCount,
                errorMessage = "Phone call history permission was denied."
            )
        } catch (_: Exception) {
            CallHistorySyncResult(
                savedCount = savedCount,
                errorMessage = "Unable to synchronize call history."
            )
        }
    }
}
