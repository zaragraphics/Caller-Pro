package com.callerpro.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class CallHistoryRecord(
    val id: Long = 0,
    val sourceCallLogId: Long? = null,
    val name: String,
    val number: String,
    val type: String,
    val dateMillis: Long,
    val durationSeconds: Long,
    val recordingFileName: String? = null
)

class CallHistoryDatabase private constructor(
    context: Context
) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "caller_pro_history.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_CALLS = "call_history"

        private const val COL_ID = "id"
        private const val COL_SOURCE_ID = "source_call_log_id"
        private const val COL_NAME = "contact_name"
        private const val COL_NUMBER = "phone_number"
        private const val COL_TYPE = "call_type"
        private const val COL_DATE = "date_millis"
        private const val COL_DURATION = "duration_seconds"
        private const val COL_RECORDING = "recording_file_name"

        @Volatile
        private var INSTANCE: CallHistoryDatabase? = null

        fun getInstance(context: Context): CallHistoryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CallHistoryDatabase(
                    context.applicationContext
                ).also { INSTANCE = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_CALLS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_SOURCE_ID INTEGER UNIQUE,
                $COL_NAME TEXT NOT NULL,
                $COL_NUMBER TEXT NOT NULL,
                $COL_TYPE TEXT NOT NULL,
                $COL_DATE INTEGER NOT NULL,
                $COL_DURATION INTEGER NOT NULL DEFAULT 0,
                $COL_RECORDING TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX idx_call_history_date
            ON $TABLE_CALLS ($COL_DATE DESC)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX idx_call_history_number
            ON $TABLE_CALLS ($COL_NUMBER)
            """.trimIndent()
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        // Preserve existing history during future database upgrades.
    }

    @Synchronized
    fun saveCall(record: CallHistoryRecord): Long {
        val db = writableDatabase

        if (record.sourceCallLogId != null) {
            val existingId = findIdBySourceCallLogId(
                db,
                record.sourceCallLogId
            )

            if (existingId != null) {
                val values = ContentValues().apply {
                    put(COL_NAME, record.name)
                    put(COL_NUMBER, record.number)
                    put(COL_TYPE, record.type)
                    put(COL_DATE, record.dateMillis)
                    put(COL_DURATION, record.durationSeconds)

                    // Do not erase an already linked recording when syncing.
                    if (record.recordingFileName != null) {
                        put(COL_RECORDING, record.recordingFileName)
                    }
                }

                db.update(
                    TABLE_CALLS,
                    values,
                    "$COL_ID = ?",
                    arrayOf(existingId.toString())
                )
                return existingId
            }
        }

        val values = ContentValues().apply {
            put(COL_NAME, record.name)
            put(COL_NUMBER, record.number)
            put(COL_TYPE, record.type)
            put(COL_DATE, record.dateMillis)
            put(COL_DURATION, record.durationSeconds)
            put(COL_RECORDING, record.recordingFileName)

            if (record.sourceCallLogId != null) {
                put(COL_SOURCE_ID, record.sourceCallLogId)
            }
        }

        return db.insert(TABLE_CALLS, null, values)
    }

    private fun findIdBySourceCallLogId(
        db: SQLiteDatabase,
        sourceCallLogId: Long
    ): Long? {
        db.query(
            TABLE_CALLS,
            arrayOf(COL_ID),
            "$COL_SOURCE_ID = ?",
            arrayOf(sourceCallLogId.toString()),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getLong(0) else null
        }
    }

    @Synchronized
    fun getAllCalls(): List<CallHistoryRecord> {
        return getCalls(0, Int.MAX_VALUE)
    }

    @Synchronized
    fun getCalls(
        offset: Int,
        limit: Int
    ): List<CallHistoryRecord> {
        val safeOffset = offset.coerceAtLeast(0)
        val safeLimit = limit.coerceAtLeast(1)
        val result = mutableListOf<CallHistoryRecord>()

        readableDatabase.query(
            TABLE_CALLS,
            null,
            null,
            null,
            null,
            null,
            "$COL_DATE DESC",
            "$safeLimit OFFSET $safeOffset"
        ).use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(COL_ID)
            val sourceIndex = cursor.getColumnIndexOrThrow(COL_SOURCE_ID)
            val nameIndex = cursor.getColumnIndexOrThrow(COL_NAME)
            val numberIndex = cursor.getColumnIndexOrThrow(COL_NUMBER)
            val typeIndex = cursor.getColumnIndexOrThrow(COL_TYPE)
            val dateIndex = cursor.getColumnIndexOrThrow(COL_DATE)
            val durationIndex = cursor.getColumnIndexOrThrow(COL_DURATION)
            val recordingIndex = cursor.getColumnIndexOrThrow(COL_RECORDING)

            while (cursor.moveToNext()) {
                result.add(
                    CallHistoryRecord(
                        id = cursor.getLong(idIndex),
                        sourceCallLogId = if (cursor.isNull(sourceIndex)) {
                            null
                        } else {
                            cursor.getLong(sourceIndex)
                        },
                        name = cursor.getString(nameIndex),
                        number = cursor.getString(numberIndex),
                        type = cursor.getString(typeIndex),
                        dateMillis = cursor.getLong(dateIndex),
                        durationSeconds = cursor.getLong(durationIndex),
                        recordingFileName = if (cursor.isNull(recordingIndex)) {
                            null
                        } else {
                            cursor.getString(recordingIndex)
                        }
                    )
                )
            }
        }

        return result
    }

    @Synchronized
    fun linkRecording(
        callId: Long,
        recordingFileName: String
    ): Boolean {
        val values = ContentValues().apply {
            put(COL_RECORDING, recordingFileName)
        }

        return writableDatabase.update(
            TABLE_CALLS,
            values,
            "$COL_ID = ?",
            arrayOf(callId.toString())
        ) > 0
    }

    @Synchronized
    fun linkRecordingToNearestCall(
        callDateMillis: Long,
        recordingFileName: String,
        maxDifferenceMillis: Long = 10 * 60 * 1000L
    ): Boolean {
        val nearestCallId = readableDatabase.rawQuery(
            """
            SELECT $COL_ID
            FROM $TABLE_CALLS
            WHERE ABS($COL_DATE - ?) <= ?
            ORDER BY ABS($COL_DATE - ?) ASC
            LIMIT 1
            """.trimIndent(),
            arrayOf(
                callDateMillis.toString(),
                maxDifferenceMillis.toString(),
                callDateMillis.toString()
            )
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else null
        } ?: return false

        return linkRecording(nearestCallId, recordingFileName)
    }

    @Synchronized
    fun getCallById(callId: Long): CallHistoryRecord? {
        return getCalls(0, Int.MAX_VALUE)
            .firstOrNull { it.id == callId }
    }

    @Synchronized
    fun getCallCount(): Int {
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_CALLS",
            null
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    @Synchronized
    fun deleteCall(callId: Long): Boolean {
        return writableDatabase.delete(
            TABLE_CALLS,
            "$COL_ID = ?",
            arrayOf(callId.toString())
        ) > 0
    }
}
