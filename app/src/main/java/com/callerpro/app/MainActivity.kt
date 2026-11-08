package com.callerpro.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.provider.CallLog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

// Caller Pro: midnight blue, electric blue and cyan theme.
private val Blue = Color(0xFF168BEE)
private val DeepBlue = Color(0xFF07152F)
private val Navy = Color(0xFF102B60)
private val Cyan = Color(0xFF21D4E8)
private val PaleBlue = Color(0xFFEAF6FF)
private val AppBackground = Color(0xFFF4F7FC)
private val AppText = Color(0xFF17233F)
private val MutedText = Color(0xFF75829A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(7, 21, 47)
        window.navigationBarColor = android.graphics.Color.rgb(255, 255, 255)
        setContent {
            MaterialTheme {
                CallerProApp()
            }
        }
    }
}

private enum class AppTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    FAMILY("Family", Icons.Default.People),
    ACTIVITY("Activity", Icons.Default.Call),
    ACCOUNT("Account", Icons.Default.Person)
}

@Composable
fun CallerProApp() {
    var selectedTab by remember { mutableStateOf(AppTab.HOME) }
    var notice by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                AppTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Blue,
                            selectedTextColor = Blue,
                            indicatorColor = PaleBlue,
                            unselectedIconColor = MutedText,
                            unselectedTextColor = MutedText
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding)
        ) {
            when (selectedTab) {
                AppTab.HOME -> HomeScreen(
                    onOpenFamily = { selectedTab = AppTab.FAMILY },
                    onOpenActivity = { selectedTab = AppTab.ACTIVITY },
                    onOpenAccount = { selectedTab = AppTab.ACCOUNT },
                    onNotice = { notice = it }
                )
                AppTab.FAMILY -> FamilyScreen { notice = it }
                AppTab.ACTIVITY -> ActivityScreen()
                AppTab.ACCOUNT -> AccountScreen()
            }

            notice?.let { message ->
                AlertDialog(
                    onDismissRequest = { notice = null },
                    title = { Text("Caller Pro") },
                    text = { Text(message) },
                    confirmButton = {
                        TextButton(onClick = { notice = null }) { Text("OK") }
                    }
                )
            }
        }
    }
}

@Composable
fun PurpleHeader(
    title: String,
    subtitle: String,
    showNotification: Boolean = true
) {
    Row(
        Modifier.fillMaxWidth()
            .background(Brush.linearGradient(listOf(DeepBlue, Navy, Blue)))
            .padding(horizontal = 22.dp, vertical = 23.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Call, null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
        }
        if (showNotification) {
            IconButton(onClick = {}) {
                Icon(Icons.Default.Notifications, "Notifications", tint = Color.White)
            }
        }
    }
}

@Composable
fun HomeScreen(
    onOpenFamily: () -> Unit,
    onOpenActivity: () -> Unit,
    onOpenAccount: () -> Unit,
    onNotice: (String) -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AppBackground),
        contentPadding = PaddingValues(bottom = 26.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepBlue)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Call, null, tint = Cyan, modifier = Modifier.size(23.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("CALLER PRO", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Your personal call companion", color = Color.White.copy(alpha = 0.68f), fontSize = 11.sp)
                }
                IconButton(onClick = { onNotice("Notifications will be available in a future update.") }) {
                    Icon(Icons.Default.Notifications, "Notifications", tint = Color.White)
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepBlue)
                    .padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 28.dp)
            ) {
                Text(
                    "WELCOME BACK",
                    color = Cyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Hello ${user?.displayName?.substringBefore(" ") ?: "there"}, how can I help you today?",
                    color = Color.White,
                    fontSize = 30.sp,
                    lineHeight = 35.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    user?.email ?: "Connect your account to use family and cloud features.",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(22.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF12366D))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Cyan.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Groups, null, tint = Cyan, modifier = Modifier.size(25.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Your Family Space", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Manage your trusted family group", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                        }
                        IconButton(onClick = onOpenFamily) {
                            Icon(Icons.Default.ArrowForward, null, tint = Cyan)
                        }
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 22.dp)) {
                Text("Quick Access", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppText)
                Text("Choose what you want to open", fontSize = 12.sp, color = MutedText)
                Spacer(Modifier.height(15.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeActionCard(
                        title = "Call Activity",
                        description = "View call history",
                        icon = Icons.Default.Call,
                        modifier = Modifier.weight(1f)
                    ) { onOpenActivity() }

                    HomeActionCard(
                        title = "Recordings",
                        description = "Your audio files",
                        icon = Icons.Default.Mic,
                        modifier = Modifier.weight(1f)
                    ) {
                        onNotice("Recording library will be connected in the next development step.")
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HomeActionCard(
                        title = "SMS Logs",
                        description = "Message activity",
                        icon = Icons.Default.Message,
                        modifier = Modifier.weight(1f)
                    ) {
                        onNotice("SMS access will only be implemented where Android permissions and policies allow.")
                    }

                    HomeActionCard(
                        title = "Cloud Backup",
                        description = "Protect your data",
                        icon = Icons.Default.CloudUpload,
                        modifier = Modifier.weight(1f)
                    ) {
                        onOpenAccount()
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text("Account & Security", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = AppText)
                Spacer(Modifier.height(11.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Security, null, tint = Blue, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Privacy & Consent", color = AppText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("You control what is shared with family", color = MutedText, fontSize = 11.sp)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = MutedText)
                    }
                }

                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = PaleBlue)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Cloud, null, tint = Blue, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Backup status", color = AppText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Cloud backup is not configured yet", color = MutedText, fontSize = 11.sp)
                        }
                        TextButton(onClick = onOpenAccount) {
                            Text("Set up", color = Blue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(142.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1D3B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(15.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Cyan.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Cyan, modifier = Modifier.size(21.dp))
            }
            Column {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text(description, color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun FamilyScreen(onNotice: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        PurpleHeader("Family Groups", "Manage members, roles and permissions")
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Icon(Icons.Default.Groups, null, tint = Blue, modifier = Modifier.size(42.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Your family group", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppText)
                    Text("No family group connected yet.", color = MutedText, fontSize = 14.sp)
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = { onNotice("Family group creation and invitations will be connected to Firebase in the next step.") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create Family Group")
                    }
                    OutlinedButton(
                        onClick = { onNotice("You will be able to accept a family invitation here.") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Mail, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Join with Invitation")
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Consent & Privacy", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppText)
            Spacer(Modifier.height(10.dp))
            Card(colors = CardDefaults.cardColors(containerColor = PaleBlue), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, null, tint = Blue, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Each member must explicitly approve data sharing. Members can see their sharing status and revoke consent.",
                        fontSize = 13.sp, color = AppText
                    )
                }
            }
        }
    }
}

private data class CallEntry(
    val name: String,
    val number: String,
    val type: Int,
    val date: Long,
    val durationSeconds: Long
)

private enum class CallFilter(val label: String) {
    ALL("All"),
    INCOMING("Incoming"),
    OUTGOING("Outgoing"),
    MISSED("Missed")
}

@Composable
fun ActivityScreen() {
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALL_LOG
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var calls by remember { mutableStateOf<List<CallEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf(CallFilter.ALL) }
    var searchText by remember { mutableStateOf("") }

    val refreshCalls: () -> Unit = {
        if (hasPermission) {
            loading = true
            error = null
            try {
                val result = mutableListOf<CallEntry>()
                val projection = arrayOf(
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION
                )

                context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${CallLog.Calls.DATE} DESC"
                )?.use { cursor ->
                    val nameIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                    val numberIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                    val typeIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                    val dateIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                    val durationIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)

                    while (cursor.moveToNext() && result.size < 500) {
                        result.add(
                            CallEntry(
                                name = cursor.getString(nameIndex).orEmpty(),
                                number = cursor.getString(numberIndex).orEmpty(),
                                type = cursor.getInt(typeIndex),
                                date = cursor.getLong(dateIndex),
                                durationSeconds = cursor.getLong(durationIndex)
                            )
                        )
                    }
                }
                calls = result
            } catch (exception: Exception) {
                error = exception.localizedMessage ?: "Unable to read call history."
            } finally {
                loading = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) refreshCalls()
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) refreshCalls()
    }

    val visibleCalls = calls.filter { call ->
        val matchesFilter = when (selectedFilter) {
            CallFilter.ALL -> true
            CallFilter.INCOMING -> call.type == CallLog.Calls.INCOMING_TYPE
            CallFilter.OUTGOING -> call.type == CallLog.Calls.OUTGOING_TYPE
            CallFilter.MISSED -> call.type == CallLog.Calls.MISSED_TYPE
        }
        val query = searchText.trim()
        val matchesSearch = query.isEmpty() ||
            call.name.contains(query, ignoreCase = true) ||
            call.number.contains(query, ignoreCase = true)

        matchesFilter && matchesSearch
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        PurpleHeader("Call History", "Recent incoming, outgoing and missed calls")

        if (!hasPermission) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Call,
                    contentDescription = null,
                    tint = Blue,
                    modifier = Modifier.size(62.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Allow call history access",
                    color = AppText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Caller Pro needs call-log permission to display call records stored on this phone. Access is requested only when you open this screen.",
                    color = MutedText,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Blue)
                ) {
                    Text("Allow Call History")
                }
            }
        } else {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search name or phone number") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    CallFilter.values().forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.label) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${visibleCalls.size} calls", color = MutedText, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = { refreshCalls() },
                        enabled = !loading
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Refresh")
                    }
                }
            }

            when {
                loading && calls.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Blue)
                    }
                }

                error != null -> {
                    Box(
                        Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(error ?: "Error", color = MutedText)
                    }
                }

                visibleCalls.isEmpty() -> {
                    Box(
                        Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = Blue,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (calls.isEmpty()) "No call records found" else "No matching calls",
                                color = AppText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Call records available on this phone will appear here.",
                                color = MutedText,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                else -> {
                    val groupedCalls = visibleCalls.groupBy { call ->
                        val calendar = java.util.Calendar.getInstance().apply {
                            timeInMillis = call.date
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }
                        calendar.timeInMillis
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        groupedCalls.forEach { (dayStart, dayCalls) ->
                            item(key = "date-$dayStart") {
                                CallDateHeader(
                                    timestamp = dayStart,
                                    callCount = dayCalls.size
                                )
                            }
                            items(
                                items = dayCalls,
                                key = { call -> "${call.date}-${call.number}-${call.type}" }
                            ) { call ->
                                CallHistoryCard(call)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallDateHeader(timestamp: Long, callCount: Int) {
    val todayStart = remember {
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val yesterdayStart = remember(todayStart) {
        java.util.Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(java.util.Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis
    }
    val heading = when (timestamp) {
        todayStart -> "Today"
        yesterdayStart -> "Yesterday"
        else -> SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
            .format(Date(timestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            heading,
            color = AppText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.weight(1f))
        Text(
            "$callCount ${if (callCount == 1) "call" else "calls"}",
            color = MutedText,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun CallHistoryCard(call: CallEntry) {
    val incoming = call.type == CallLog.Calls.INCOMING_TYPE
    val outgoing = call.type == CallLog.Calls.OUTGOING_TYPE
    val missed = call.type == CallLog.Calls.MISSED_TYPE

    val callLabel = when {
        missed -> "Missed call"
        incoming -> "Incoming call"
        outgoing -> "Outgoing call"
        else -> "Call"
    }

    val callTint = if (missed) Color(0xFFE05252) else Blue

    val dateText = remember(call.date) {
        SimpleDateFormat(
            "hh:mm a",
            Locale.getDefault()
        ).format(Date(call.date))
    }

    val durationText = remember(call.durationSeconds) {
        val minutes = call.durationSeconds / 60
        val seconds = call.durationSeconds % 60
        if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(43.dp)
                    .clip(CircleShape)
                    .background(callTint.copy(alpha = 0.11f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when {
                        missed -> Icons.Default.CallMissed
                        incoming -> Icons.Default.CallReceived
                        outgoing -> Icons.Default.CallMade
                        else -> Icons.Default.Call
                    },
                    contentDescription = callLabel,
                    tint = callTint
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    call.name.ifBlank {
                        call.number.ifBlank { "Unknown caller" }
                    },
                    color = AppText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                if (call.name.isNotBlank() && call.number.isNotBlank()) {
                    Text(call.number, color = MutedText, fontSize = 12.sp)
                }

                Text(
                    "$callLabel • $dateText",
                    color = MutedText,
                    fontSize = 11.sp
                )
            }

            Text(
                durationText,
                color = AppText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AccountScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    var user by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    var message by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    val authManager = remember(activity) {
        activity?.let { GoogleAuthManager(it) }
    }

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        loading = false
        try {
            val manager = authManager
            if (manager == null) {
                message = "Google Sign-In is unavailable."
            } else {
                manager.handleSignInResult(result.data).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val signedInUser = FirebaseAuth.getInstance().currentUser
                        if (signedInUser != null) {
                            UserProfileRepository.saveUserProfile(
                                user = signedInUser,
                                onSuccess = {
                                    user = FirebaseAuth.getInstance().currentUser
                                    message = "Google sign-in successful. Profile saved."
                                },
                                onError = { error ->
                                    user = FirebaseAuth.getInstance().currentUser
                                    message = "Profile saving failed: $error"
                                }
                            )
                        } else {
                            message = "User details unavailable."
                        }
                    } else {
                        message = "Authentication failed: ${task.exception?.localizedMessage ?: "Unknown error"}"
                    }
                }
            }
        } catch (e: Exception) {
            message = "Google Sign-In error: ${e.localizedMessage ?: e.javaClass.simpleName}"
        }
    }

    Column(Modifier.fillMaxSize()) {
        PurpleHeader("My Account", "Your Caller Pro profile")
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.AccountCircle, null, tint = Blue, modifier = Modifier.size(76.dp))
            Spacer(Modifier.height(12.dp))
            Text("Caller Pro Account", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = AppText)
            Spacer(Modifier.height(18.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(20.dp)) {
                    if (user != null) {
                        Text("Signed in", color = Blue, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(user?.displayName ?: "Google user", color = AppText, fontWeight = FontWeight.SemiBold)
                        Text(user?.email ?: "", color = MutedText, fontSize = 13.sp)
                        Spacer(Modifier.height(18.dp))
                        OutlinedButton(
                            onClick = {
                                loading = true
                                authManager?.signOut {
                                    user = null
                                    loading = false
                                    message = "You have signed out."
                                } ?: run {
                                    loading = false
                                    message = "Unable to sign out."
                                }
                            },
                            enabled = !loading && authManager != null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sign Out")
                        }
                    } else {
                        Text("Google sign-in is optional.", color = AppText, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "You can use available local features without an account. Sign in to access family groups and cloud backup.",
                            color = MutedText, fontSize = 13.sp
                        )
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = {
                                if (authManager == null) {
                                    message = "Unable to open Google sign-in."
                                } else {
                                    loading = true
                                    signInLauncher.launch(authManager.getSignInIntent())
                                }
                            },
                            enabled = !loading && authManager != null,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Blue),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(19.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(if (loading) "Please wait..." else "Sign in with Google")
                        }
                    }
                    message?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = Blue, fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            AccountOption(Icons.Default.Security, "Privacy & Security")
            AccountOption(Icons.Default.Cloud, "Backup & Restore")
            AccountOption(Icons.Default.Info, "About Caller Pro")
        }
    }
}

@Composable
fun AccountOption(icon: ImageVector, title: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Blue)
            Spacer(Modifier.width(12.dp))
            Text(title, color = AppText, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = MutedText)
        }
    }
}
