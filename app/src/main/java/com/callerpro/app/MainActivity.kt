
package com.callerpro.app

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Navy = Color(0xFF00022E)
private val Red = Color(0xFFE52D45)

data class CallEntry(
    val name: String,
    val number: String,
    val type: String,
    val date: String,
    val duration: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CallerProScreen() }
    }
}

@Composable
fun CallerProScreen() {
    val context = LocalContext.current

    var calls by remember { mutableStateOf(listOf<CallEntry>()) }
    var callMessage by remember { mutableStateOf("Checking permission...") }
    var hasMore by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var permissionGranted by remember {
        mutableStateOf(
            context.checkSelfPermission(
                Manifest.permission.READ_CALL_LOG
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var selectedAudio by remember { mutableStateOf<Uri?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var message by remember { mutableStateOf("No recording selected") }

    fun loadCallHistory(loadMore: Boolean = false) {
        if (!permissionGranted || isLoading) return

        if (!loadMore) {
            calls = emptyList()
            hasMore = false
        }

        val offset = if (loadMore) calls.size else 0
        isLoading = true
        callMessage = "Loading calls..."

        Thread {
            try {
                val result = mutableListOf<CallEntry>()
                var moreAvailable = false

                val projection = arrayOf(
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION
                )

                val cursor = context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${CallLog.Calls.DATE} DESC"
                )

                if (cursor == null) {
                    throw Exception("Call history provider returned no data")
                }

                cursor.use {
                    val nameIndex =
                        it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                    val numberIndex =
                        it.getColumnIndex(CallLog.Calls.NUMBER)
                    val typeIndex =
                        it.getColumnIndex(CallLog.Calls.TYPE)
                    val dateIndex =
                        it.getColumnIndex(CallLog.Calls.DATE)
                    val durationIndex =
                        it.getColumnIndex(CallLog.Calls.DURATION)

                    if (nameIndex < 0 || numberIndex < 0 ||
                        typeIndex < 0 || dateIndex < 0 ||
                        durationIndex < 0
                    ) {
                        throw Exception("Required call-log columns are unavailable")
                    }

                    if (it.moveToPosition(offset)) {
                        var count = 0

                        do {
                            if (count >= 25) {
                                moreAvailable = true
                                break
                            }

                            val savedName = it.getString(nameIndex)
                            val number = it.getString(numberIndex)

                            val type = when (it.getInt(typeIndex)) {
                                CallLog.Calls.INCOMING_TYPE -> "Incoming"
                                CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                                CallLog.Calls.MISSED_TYPE -> "Missed"
                                CallLog.Calls.REJECTED_TYPE -> "Rejected"
                                else -> "Other"
                            }

                            val dateValue = it.getLong(dateIndex)
                            val durationValue = it.getLong(durationIndex)

                            val formattedDate = SimpleDateFormat(
                                "dd MMM yyyy, hh:mm a",
                                Locale.getDefault()
                            ).format(Date(dateValue))

                            result.add(
                                CallEntry(
                                    name = savedName?.takeIf {
                                        it.isNotBlank()
                                    } ?: "Unknown caller",
                                    number = number ?: "Unknown number",
                                    type = type,
                                    date = formattedDate,
                                    duration = "${durationValue / 60} min ${durationValue % 60} sec"
                                )
                            )

                            count++
                        } while (it.moveToNext())
                    }
                }

                Handler(Looper.getMainLooper()).post {
                    calls = if (loadMore) calls + result else result
                    hasMore = moreAvailable
                    isLoading = false

                    callMessage = if (calls.isEmpty()) {
                        "No call history found"
                    } else {
                        "Showing ${calls.size} calls"
                    }
                }

            } catch (e: SecurityException) {
                Handler(Looper.getMainLooper()).post {
                    isLoading = false
                    callMessage = "Permission error: ${e.message ?: "Access denied"}"
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    isLoading = false
                    callMessage = "Error: ${e.message ?: e.javaClass.simpleName}"
                }
            }
        }.start()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted

        if (granted) {
            loadCallHistory()
        } else {
            callMessage = "Call History permission denied. Please allow it in App Settings."
        }
    }

    LaunchedEffect(Unit) {
        if (permissionGranted) {
            loadCallHistory()
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
        }
    }

    val audioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }

            player?.release()
            player = null
            selectedAudio = uri
            message = "Audio selected successfully"
        }
    }

    DisposableEffect(Unit) {
        onDispose { player?.release() }
    }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Navy)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "CALLER PRO",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Your personal call library",
                color = Color.LightGray,
                fontSize = 15.sp
            )

            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "Call History",
                        color = Navy,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(10.dp))
                    Text(callMessage, color = Color.DarkGray)
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (permissionGranted) {
                                loadCallHistory()
                            } else {
                                permissionLauncher.launch(
                                    Manifest.permission.READ_CALL_LOG
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Red
                        )
                    ) {
                        Text("Refresh Call History")
                    }

                    Spacer(Modifier.height(12.dp))

                    calls.forEach { call ->
                        HorizontalDivider()

                        Column(Modifier.padding(vertical = 12.dp)) {
                            Text(
                                call.name,
                                color = Navy,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )

                            Text(call.number, color = Color.DarkGray)

                            Text(
                                "${call.type} • ${call.duration}",
                                color = Red
                            )

                            Text(
                                call.date,
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (hasMore || isLoading) {
                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                loadCallHistory(loadMore = true)
                            },
                            enabled = hasMore && !isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Navy
                            )
                        ) {
                            Text(
                                if (isLoading) "Loading..."
                                else "Load More (25)"
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "Recording Library",
                        color = Navy,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(12.dp))
                    Text(message, color = Color.DarkGray)
                    Spacer(Modifier.height(18.dp))

                    Button(
                        onClick = {
                            audioPicker.launch(arrayOf("audio/*"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Red
                        )
                    ) {
                        Text("Select Audio Recording")
                    }

                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val uri = selectedAudio

                            if (uri == null) {
                                message = "Please select an audio file first"
                            } else {
                                try {
                                    player?.release()
                                    player = MediaPlayer.create(context, uri)

                                    if (player != null) {
                                        player?.setOnCompletionListener {
                                            message = "Playback completed"
                                        }
                                        player?.start()
                                        message = "Playing recording"
                                    } else {
                                        message = "Unable to play this audio file"
                                    }
                                } catch (_: Exception) {
                                    message = "Unable to play this audio file"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedAudio != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Navy
                        )
                    ) {
                        Text("Play Recording")
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            player?.release()
                            player = null
                            message = "Playback stopped"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Stop Playback")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Private audio library",
                color = Color.LightGray,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(20.dp))
        }
    }
}
