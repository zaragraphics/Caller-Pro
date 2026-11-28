package com.callerpro.app

import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private val LibraryBlue = Color(0xFF168BEE)
private val LibraryBackground = Color(0xFFF4F7FC)
private val LibraryText = Color(0xFF17233F)
private val LibraryMuted = Color(0xFF75829A)

class RecordingLibraryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                RecordingLibraryScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
private fun RecordingLibraryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val files = remember {
        mutableStateListOf<File>().apply {
            addAll(RecordingStorage.scanAudioFiles(context))
        }
    }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingFile by remember { mutableStateOf<File?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var durationMs by remember { mutableIntStateOf(0) }
    var positionMs by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<File?>(null) }

    fun stopPlayer() {
        try { player?.stop() } catch (_: Exception) {}
        try { player?.release() } catch (_: Exception) {}
        player = null
        playingFile = null
        isPlaying = false
        durationMs = 0
        positionMs = 0
    }

    fun playFile(file: File) {
        stopPlayer()
        try {
            val next = MediaPlayer()
            next.setDataSource(file.absolutePath)
            next.setOnCompletionListener {
                try { it.release() } catch (_: Exception) {}
                if (player === it) {
                    player = null
                    playingFile = null
                    isPlaying = false
                    positionMs = 0
                }
            }
            next.prepare()
            next.start()
            player = next
            playingFile = file
            durationMs = next.duration.coerceAtLeast(0)
            positionMs = 0
            isPlaying = true
        } catch (error: Exception) {
            stopPlayer()
            message = "Unable to play this recording. The file may be damaged or unsupported."
        }
    }

    DisposableEffect(Unit) {
        onDispose { stopPlayer() }
    }

    LaunchedEffect(player, isPlaying) {
        while (player != null && isPlaying) {
            val active = player ?: break
            positionMs = try { active.currentPosition } catch (_: Exception) { positionMs }
            delay(300)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(LibraryBackground)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF07152F))
                .padding(horizontal = 16.dp, vertical = 17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text("Recording Library", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("${files.size} audio file(s)", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }

        if (files.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = LibraryBlue, modifier = Modifier.size(58.dp))
                Spacer(Modifier.height(14.dp))
                Text("No recordings yet", color = LibraryText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                Text("When Caller Pro saves an audio file, it will appear here.", color = LibraryMuted, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                items(files, key = { it.absolutePath }) { file ->
                    val active = playingFile == file
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(15.dp)) {
                            Text(file.nameWithoutExtension, color = LibraryText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(file.lastModified()))} • ${formatFileSize(file.length())}",
                                color = LibraryMuted, fontSize = 11.sp
                            )
                            if (active && durationMs > 0) {
                                Spacer(Modifier.height(9.dp))
                                Slider(
                                    value = positionMs.coerceIn(0, durationMs).toFloat(),
                                    onValueChange = { value ->
                                        positionMs = value.toInt()
                                        try { player?.seekTo(positionMs) } catch (_: Exception) {}
                                    },
                                    valueRange = 0f..durationMs.toFloat()
                                )
                                Text("${formatTime(positionMs)} / ${formatTime(durationMs)}", color = LibraryMuted, fontSize = 10.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FilledTonalButton(
                                    onClick = {
                                        if (active && isPlaying) {
                                            try { player?.pause() } catch (_: Exception) {}
                                            isPlaying = false
                                        } else if (active && player != null) {
                                            try { player?.start() } catch (_: Exception) {}
                                            isPlaying = true
                                        } else {
                                            playFile(file)
                                        }
                                    }
                                ) {
                                    Icon(if (active && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(5.dp))
                                    Text(if (active && isPlaying) "Pause" else if (active) "Resume" else "Play")
                                }
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = {
                                    try {
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "audio/*"
                                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share recording"))
                                    } catch (_: Exception) {
                                        message = "Unable to share this recording."
                                    }
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = LibraryBlue)
                                }
                                IconButton(onClick = { deleteTarget = file }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFD64545))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete recording?") },
            text = { Text("This will permanently delete ${target.name}.") },
            confirmButton = {
                TextButton(onClick = {
                    if (playingFile == target) stopPlayer()
                    if (target.delete()) {
                        files.remove(target)
                        message = "Recording deleted."
                    } else {
                        message = "Could not delete this recording."
                    }
                    deleteTarget = null
                }) { Text("Delete", color = Color(0xFFD64545)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }

    message?.let { text ->
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text("Caller Pro") },
            text = { Text(text) },
            confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } }
        )
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "${bytes} B"
    bytes < 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0)
    else -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
}

private fun formatTime(milliseconds: Int): String {
    val totalSeconds = (milliseconds / 1000).coerceAtLeast(0)
    return String.format(Locale.getDefault(), "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
}
