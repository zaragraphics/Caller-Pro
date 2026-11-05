
package com.callerpro.app

import android.app.Application
import java.io.File

class CallerProApp : Application() {

    lateinit var recordingsDirectory: File
        private set

    override fun onCreate() {
        super.onCreate()

        recordingsDirectory = File(
            filesDir,
            "Caller Pro/Recordings"
        )

        if (!recordingsDirectory.exists()) {
            recordingsDirectory.mkdirs()
        }
    }
}
