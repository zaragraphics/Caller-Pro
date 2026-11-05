
package com.callerpro.app

import android.content.Context
import android.os.Environment
import java.io.File

object RecordingStorage {

    fun getRecordingsFolder(context: Context): File? {
        val musicDirectory = context.getExternalFilesDir(
            Environment.DIRECTORY_MUSIC
        ) ?: return null

        val folder = File(
            musicDirectory,
            "Caller Pro/Recordings"
        )

        if (!folder.exists()) {
            folder.mkdirs()
        }

        return if (folder.exists() && folder.isDirectory) {
            folder
        } else {
            null
        }
    }

    fun scanAudioFiles(context: Context): List<File> {
        val folder = getRecordingsFolder(context) ?: return emptyList()

        val supportedExtensions = setOf(
            "m4a",
            "mp3",
            "aac",
            "wav",
            "3gp",
            "amr",
            "ogg",
            "opus"
        )

        return folder.listFiles()
            ?.filter { file ->
                file.isFile &&
                file.extension.lowercase() in supportedExtensions
            }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }
}
