package com.callerpro.app

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Navy = Color(0xFF00022E)
private val Red = Color(0xFFE52D45)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CallerProScreen()
        }
    }
}

@Composable
fun CallerProScreen() {
    val context = LocalContext.current

    var selectedAudio by remember { mutableStateOf<Uri?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("No recording selected") }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
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
            isPlaying = false
            selectedAudio = uri
            message = "Audio selected successfully"
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player?.release()
        }
    }

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Navy)
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

            Spacer(Modifier.height(30.dp))

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

                    Text(
                        message,
                        color = Color.DarkGray
                    )

                    Spacer(Modifier.height(20.dp))

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
                                            isPlaying = false
                                            message = "Playback completed"
                                        }
                                        player?.start()
                                        isPlaying = true
                                        message = "Playing recording"
                                    } else {
                                        message = "Unable to play this audio file"
                                    }
                                } catch (_: Exception) {
                                    message = "Unable to play this audio file"
                                    isPlaying = false
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
                            isPlaying = false
                            message = "Playback stopped"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Stop Playback")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Private audio library",
                color = Color.LightGray,
                fontSize = 14.sp
            )
        }
    }
}
