
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Navy = Color(0xFF061B49)
private val DeepBlue = Color(0xFF00113D)
private val Blue = Color(0xFF087BFF)
private val Cyan = Color(0xFF22C8F5)
private val PaleBlue = Color(0xFFEAF4FF)
private val Ink = Color(0xFF101828)
private val Muted = Color(0xFF667085)
private val Line = Color(0xFFE7ECF3)

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
        setContent {
            CallerProScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallerProScreen() {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var calls by remember { mutableStateOf(listOf<CallEntry>()) }
    var callMessage by remember { mutableStateOf("Checking permission...") }
    var hasMore by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    var selectedFilter by remember { mutableStateOf("All") }
    var searchText by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    var selectedMenu by remember { mutableStateOf("All Calls") }
    var infoDialog by remember { mutableStateOf<String?>(null) }

    var permissionGranted by remember {
        mutableStateOf(
            context.checkSelfPermission(
                Manifest.permission.READ_CALL_LOG
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var selectedAudio by remember { mutableStateOf<Uri?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var audioMessage by remember {
        mutableStateOf("No recording selected")
    }

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
                ) ?: throw Exception("Call history unavailable")

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
                        throw Exception("Call-log data unavailable")
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

                            val type = when (
                                it.getInt(typeIndex)
                            ) {
                                CallLog.Calls.INCOMING_TYPE ->
                                    "Incoming"
                                CallLog.Calls.OUTGOING_TYPE ->
                                    "Outgoing"
                                CallLog.Calls.MISSED_TYPE ->
                                    "Missed"
                                CallLog.Calls.REJECTED_TYPE ->
                                    "Rejected"
                                else -> "Other"
                            }

                            val dateValue = it.getLong(dateIndex)
                            val durationValue =
                                it.getLong(durationIndex)

                            val formattedDate =
                                SimpleDateFormat(
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
                    calls = if (loadMore) {
                        calls + result
                    } else {
                        result
                    }

                    hasMore = moreAvailable
                    isLoading = false

                    callMessage = if (calls.isEmpty()) {
                        "No call history found"
                    } else {
                        "Showing ${calls.size} calls"
                    }
                }

            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    isLoading = false
                    callMessage =
                        "Unable to load calls. Please try again."
                }
            }
        }.start()
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            permissionGranted = granted

            if (granted) {
                loadCallHistory()
            } else {
                callMessage =
                    "Permission denied. Allow Call Logs access in App Settings."
            }
        }

    LaunchedEffect(Unit) {
        if (permissionGranted) {
            loadCallHistory()
        } else {
            permissionLauncher.launch(
                Manifest.permission.READ_CALL_LOG
            )
        }
    }

    val audioPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                try {
                    context.contentResolver
                        .takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                } catch (_: Exception) {
                }

                player?.release()
                player = null
                selectedAudio = uri
                audioMessage = "Audio selected successfully"
            }
        }

    DisposableEffect(Unit) {
        onDispose {
            player?.release()
        }
    }

    val filteredCalls = calls.filter { call ->
        val matchesFilter = when (selectedFilter) {
            "Incoming" -> call.type == "Incoming"
            "Outgoing" -> call.type == "Outgoing"
            "Missed" -> call.type == "Missed"
            else -> true
        }

        val matchesSearch =
            searchText.isBlank() ||
            call.name.contains(searchText, true) ||
            call.number.contains(searchText, true)

        matchesFilter && matchesSearch
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .fillMaxHeight(),
                drawerContainerColor = Color.White,
                drawerShape = RoundedCornerShape(
                    topEnd = 26.dp,
                    bottomEnd = 26.dp
                )
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(DeepBlue, Blue, Cyan)
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(42.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            "CALLER PRO",
                            color = Color.White,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Your personal call library",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                val menuItems = listOf(
                    Triple("All Calls", Icons.Default.Home, "calls"),
                    Triple("Incoming Calls", Icons.Default.CallReceived, "calls"),
                    Triple("Outgoing Calls", Icons.Default.CallMade, "calls"),
                    Triple("Missed Calls", Icons.Default.CallMissed, "calls"),
                    Triple("Recording Library", Icons.Default.Mic, "recordings"),
                    Triple("Favorites", Icons.Default.Star, "info"),
                    Triple("Settings", Icons.Default.Settings, "info"),
                    Triple("Privacy & Security", Icons.Default.Security, "info"),
                    Triple("Backup & Restore", Icons.Default.CloudUpload, "info"),
                    Triple("Storage & Files", Icons.Default.Folder, "info"),
                    Triple("About Caller Pro", Icons.Default.Info, "info")
                )

                menuItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = {
                            Icon(item.second, null, tint = Blue)
                        },
                        label = {
                            Text(
                                item.first,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        selected = selectedMenu == item.first,
                        onClick = {
                            selectedMenu = item.first
                            scope.launch {
                                drawerState.close()
                            }

                            when (item.third) {
                                "calls" -> {
                                    selectedFilter = when (item.first) {
                                        "Incoming Calls" -> "Incoming"
                                        "Outgoing Calls" -> "Outgoing"
                                        "Missed Calls" -> "Missed"
                                        else -> "All"
                                    }
                                }

                                "recordings" -> {
                                    infoDialog = "Recording Library"
                                }

                                else -> {
                                    infoDialog = item.first
                                }
                            }
                        },
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 2.dp
                        ),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = PaleBlue,
                            selectedTextColor = Blue
                        )
                    )
                }
            }
        }
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            DeepBlue,
                            Color(0xFF062B70),
                            DeepBlue
                        )
                    )
                )
        ) {

            // TOP APP BAR

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 12.dp,
                        vertical = 10.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = {
                        scope.launch {
                            drawerState.open()
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.Menu,
                        "Menu",
                        tint = Color.White
                    )
                }

                Icon(
                    Icons.Default.Call,
                    null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            Blue,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(6.dp)
                )

                Spacer(Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "Caller Pro",
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Your personal call library",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = {
                        searchOpen = !searchOpen
                    }
                ) {
                    Icon(
                        Icons.Default.Search,
                        "Search",
                        tint = Color.White
                    )
                }

                Surface(
                    color = if (permissionGranted) {
                        Color(0xFF0C8F67)
                    } else {
                        Color(0xFF667085)
                    },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        if (permissionGranted) "ACCESS ON" else "ACCESS OFF",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            horizontal = 9.dp,
                            vertical = 7.dp
                        )
                    )
                }
            }

            if (searchOpen) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = {
                        searchText = it
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = {
                        Text("Search name or number")
                    },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                searchText = ""
                                searchOpen = false
                            }
                        ) {
                            Icon(Icons.Default.Close, "Close")
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = Ink,
                        unfocusedTextColor = Ink
                    )
                )
            }

            // SUMMARY CARDS

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                StatCard(
                    "All",
                    calls.size.toString(),
                    Icons.Default.Call,
                    Blue,
                    Modifier.weight(1f)
                )

                StatCard(
                    "Incoming",
                    calls.count { it.type == "Incoming" }.toString(),
                    Icons.Default.CallReceived,
                    Color(0xFF19A86B),
                    Modifier.weight(1f)
                )

                StatCard(
                    "Outgoing",
                    calls.count { it.type == "Outgoing" }.toString(),
                    Icons.Default.CallMade,
                    Color(0xFF00A6D6),
                    Modifier.weight(1f)
                )

                StatCard(
                    "Missed",
                    calls.count { it.type == "Missed" }.toString(),
                    Icons.Default.CallMissed,
                    Color(0xFFE5485D),
                    Modifier.weight(1f)
                )
            }

            // WHITE MAIN CONTENT

            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(
                    topStart = 25.dp,
                    topEnd = 25.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = 14.dp,
                            vertical = 12.dp
                        )
                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Call History",
                            color = Ink,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            callMessage,
                            color = Muted,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        IconButton(
                            onClick = {
                                if (permissionGranted) {
                                    loadCallHistory()
                                } else {
                                    permissionLauncher.launch(
                                        Manifest.permission.READ_CALL_LOG
                                    )
                                }
                            },
                            enabled = !isLoading
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                "Refresh",
                                tint = Blue
                            )
                        }
                    }

                    // FILTER TABS

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(
                                rememberScrollState()
                            ),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        listOf(
                            "All",
                            "Incoming",
                            "Outgoing",
                            "Missed"
                        ).forEach { filter ->

                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = {
                                    selectedFilter = filter
                                },
                                label = {
                                    Text(filter)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Blue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    if (filteredCalls.isEmpty() && !isLoading) {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (!permissionGranted) {
                                    "Please grant Call History permission"
                                } else {
                                    "No calls found"
                                },
                                color = Muted
                            )
                        }

                    } else {

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(
                                bottom = 10.dp
                            )
                        ) {

                            items(filteredCalls) { call ->

                                CallRow(call) {
                                    infoDialog =
                                        "${call.name}\n" +
                                        "${call.number}\n" +
                                        "${call.type} • ${call.duration}\n" +
                                        call.date
                                }
                            }

                            if (hasMore || isLoading) {
                                item {
                                    Button(
                                        onClick = {
                                            loadCallHistory(
                                                loadMore = true
                                            )
                                        },
                                        enabled = hasMore && !isLoading,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        shape = RoundedCornerShape(28.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Blue
                                        )
                                    ) {
                                        Text(
                                            if (isLoading) {
                                                "Loading..."
                                            } else {
                                                "Load More (25)"
                                            },
                                            fontWeight = FontWeight.Bold
                                        )

                                        Icon(
                                            Icons.Default.ExpandMore,
                                            null
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // RECORDING LIBRARY

                    HorizontalDivider(color = Line)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Recording Library",
                                color = Ink,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )

                            Text(
                                audioMessage,
                                color = Muted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = {
                                audioPicker.launch(arrayOf("audio/*"))
                            }
                        ) {
                            Icon(
                                Icons.Default.FolderOpen,
                                "Select audio",
                                tint = Blue
                            )
                        }

                        IconButton(
                            enabled = selectedAudio != null,
                            onClick = {
                                try {
                                    player?.release()
                                    player = MediaPlayer.create(
                                        context,
                                        selectedAudio
                                    )

                                    if (player != null) {
                                        player?.start()
                                        audioMessage = "Playing recording"
                                    } else {
                                        audioMessage =
                                            "Unable to play audio"
                                    }
                                } catch (_: Exception) {
                                    audioMessage =
                                        "Unable to play this audio file"
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                "Play",
                                tint = Blue
                            )
                        }

                        IconButton(
                            onClick = {
                                player?.release()
                                player = null
                                audioMessage = "Playback stopped"
                            }
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                "Stop",
                                tint = Muted
                            )
                        }
                    }
                }
            }
        }
    }

    if (infoDialog != null) {
        AlertDialog(
            onDismissRequest = {
                infoDialog = null
            },
            title = {
                Text(infoDialog!!.substringBefore("\n"))
            },
            text = {
                Text(infoDialog!!)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        infoDialog = null
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    count: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(88.dp),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )

            Text(
                count,
                color = Ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                title,
                color = Muted,
                fontSize = 9.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CallRow(
    call: CallEntry,
    onClick: () -> Unit
) {
    val accent = when (call.type) {
        "Incoming" -> Color(0xFF16A36A)
        "Outgoing" -> Blue
        "Missed", "Rejected" -> Color(0xFFE5485D)
        else -> Muted
    }

    val icon = when (call.type) {
        "Incoming" -> Icons.Default.CallReceived
        "Outgoing" -> Icons.Default.CallMade
        "Missed", "Rejected" -> Icons.Default.CallMissed
        else -> Icons.Default.Call
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(43.dp)
                .clip(CircleShape)
                .background(PaleBlue),
            contentAlignment = Alignment.Center
        ) {
            Text(
                call.name.firstOrNull()?.uppercase() ?: "?",
                color = Blue,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                call.name,
                color = Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                call.number,
                color = Muted,
                fontSize = 12.sp,
                maxLines = 1
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    null,
                    tint = accent,
                    modifier = Modifier.size(14.dp)
                )

                Spacer(Modifier.width(4.dp))

                Text(
                    call.type,
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    " • ${call.duration}",
                    color = Muted,
                    fontSize = 11.sp
                )
            }

            Text(
                call.date,
                color = Muted,
                fontSize = 10.sp
            )
        }

        IconButton(onClick = onClick) {
            Icon(
                Icons.Default.PlayCircleOutline,
                "Call details",
                tint = Blue,
                modifier = Modifier.size(25.dp)
            )
        }

        IconButton(onClick = onClick) {
            Icon(
                Icons.Default.MoreVert,
                "More options",
                tint = Muted
            )
        }
    }

    HorizontalDivider(color = Line)
}
