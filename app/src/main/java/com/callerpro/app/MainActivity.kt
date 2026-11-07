package com.callerpro.app

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

private val Purple = Color(0xFF8B20F5)
private val DeepPurple = Color(0xFF4310A8)
private val Indigo = Color(0xFF3824C8)
private val LightPurple = Color(0xFFF2EAFE)
private val AppBackground = Color(0xFFF8F6FC)
private val AppText = Color(0xFF211A35)
private val MutedText = Color(0xFF817A91)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = android.graphics.Color.rgb(67, 16, 168)
        window.navigationBarColor = android.graphics.Color.rgb(255, 255, 255)

        setContent {
            MaterialTheme {
                CallerProApp()
            }
        }
    }
}

private enum class AppTab(
    val label: String,
    val icon: ImageVector
) {
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
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                AppTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(tab.icon, contentDescription = tab.label)
                        },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Purple,
                            selectedTextColor = Purple,
                            indicatorColor = LightPurple,
                            unselectedIconColor = MutedText,
                            unselectedTextColor = MutedText
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                AppTab.HOME -> HomeScreen(
                    onOpenFamily = { selectedTab = AppTab.FAMILY },
                    onOpenActivity = { selectedTab = AppTab.ACTIVITY },
                    onOpenAccount = { selectedTab = AppTab.ACCOUNT },
                    onNotice = { notice = it }
                )

                AppTab.FAMILY -> FamilyScreen(
                    onNotice = { notice = it }
                )

                AppTab.ACTIVITY -> ActivityScreen()

                AppTab.ACCOUNT -> AccountScreen()
            }

            notice?.let { message ->
                AlertDialog(
                    onDismissRequest = { notice = null },
                    title = { Text("Caller Pro") },
                    text = { Text(message) },
                    confirmButton = {
                        TextButton(onClick = { notice = null }) {
                            Text("OK")
                        }
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
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(DeepPurple, Purple, Indigo)
                )
            )
            .padding(horizontal = 22.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Call,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(27.dp)
            )
        }

        Spacer(Modifier.width(13.dp))

        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = Color.White,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                subtitle,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp
            )
        }

        if (showNotification) {
            IconButton(onClick = {}) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.White
                )
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
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            PurpleHeader(
                "Caller Pro",
                "Call • Family • Secure Backup"
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(DeepPurple, Purple, Indigo)
                        )
                    )
                    .padding(start = 22.dp, end = 22.dp, bottom = 28.dp)
            ) {
                Text(
                    "Welcome back",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    user?.displayName ?: "Welcome to Caller Pro",
                    color = Color.White,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    user?.email ?: "Sign in to connect your family",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )

                Spacer(Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(LightPurple),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.People,
                                null,
                                tint = Purple,
                                modifier = Modifier.size(29.dp)
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                "My Family",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppText
                            )
                            Text(
                                "Manage your family group",
                                fontSize = 12.sp,
                                color = MutedText
                            )
                        }

                        IconButton(onClick = onOpenFamily) {
                            Icon(
                                Icons.Default.ChevronRight,
                                null,
                                tint = Purple
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(18.dp)) {
                Text(
                    "Quick Access",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppText
                )

                Spacer(Modifier.height(13.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickCard(
                        "Call Logs",
                        Icons.Default.Call,
                        Modifier.weight(1f)
                    ) { onOpenActivity() }

                    QuickCard(
                        "Recordings",
                        Icons.Default.Mic,
                        Modifier.weight(1f)
                    ) {
                        onNotice("Recording library will be connected in the next development step.")
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickCard(
                        "SMS Logs",
                        Icons.Default.Message,
                        Modifier.weight(1f)
                    ) {
                        onNotice("SMS access will only be implemented where Android permissions and policies allow.")
                    }

                    QuickCard(
                        "Backup",
                        Icons.Default.CloudUpload,
                        Modifier.weight(1f)
                    ) {
                        onNotice("Cloud backup setup is coming next.")
                    }
                }

                Spacer(Modifier.height(22.dp))

                Text(
                    "Cloud Backup",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppText
                )

                Spacer(Modifier.height(10.dp))

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = LightPurple
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(17.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Cloud,
                            null,
                            tint = Purple,
                            modifier = Modifier.size(38.dp)
                        )

                        Spacer(Modifier.width(12.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                "Backup not configured",
                                fontWeight = FontWeight.Bold,
                                color = AppText
                            )
                            Text(
                                "Connect your account to continue",
                                color = MutedText,
                                fontSize = 12.sp
                            )
                        }

                        TextButton(onClick = onOpenAccount) {
                            Text("Connect")
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                Text(
                    "Privacy & Consent",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppText
                )

                Spacer(Modifier.height(10.dp))

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Security,
                            null,
                            tint = Purple,
                            modifier = Modifier.size(32.dp)
                        )

                        Spacer(Modifier.width(12.dp))

                        Text(
                            "Family data is shared only with the member's explicit consent. Members can review or revoke sharing.",
                            color = AppText,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(112.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                null,
                tint = Purple,
                modifier = Modifier.size(29.dp)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                title,
                color = AppText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun FamilyScreen(
    onNotice: (String) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        PurpleHeader(
            "Family Groups",
            "Manage members, roles and permissions"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(Modifier.padding(20.dp)) {
                    Icon(
                        Icons.Default.Groups,
                        null,
                        tint = Purple,
                        modifier = Modifier.size(42.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Your family group",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppText
                    )

                    Text(
                        "No family group connected yet.",
                        color = MutedText,
                        fontSize = 14.sp
                    )

                    Spacer(Modifier.height(18.dp))

                    Button(
                        onClick = {
                            onNotice("Family group creation and invitations will be connected to Firebase in the next step.")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Purple
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create Family Group")
                    }

                    OutlinedButton(
                        onClick = {
                            onNotice("You will be able to accept a family invitation here.")
                        },
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

            Text(
                "Consent & Privacy",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AppText
            )

            Spacer(Modifier.height(10.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = LightPurple
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Shield,
                        null,
                        tint = Purple,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Each member must explicitly approve data sharing. Members can see their sharing status and revoke consent.",
                        fontSize = 13.sp,
                        color = AppText
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityScreen() {
    Column(Modifier.fillMaxSize()) {
        PurpleHeader(
            "Activity",
            "Your call and recording activity"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.History,
                null,
                tint = Purple,
                modifier = Modifier.size(65.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "Activity will appear here",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = AppText
            )

            Spacer(Modifier.height(7.dp))

            Text(
                "Your available call logs and recordings will be displayed here after the required permissions and features are configured.",
                color = MutedText,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun AccountScreen() {
    val context = LocalContext.current
    val activity = context as? Activity

    var user by remember {
        mutableStateOf(FirebaseAuth.getInstance().currentUser)
    }

    var message by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    val authManager = remember(activity) {
        activity?.let { GoogleAuthManager(it) }
    }

    val signInLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            loading = false

            try {
                val manager = authManager

                if (manager == null) {
                    message = "Google Sign-In is unavailable."
                } else {
                    manager.handleSignInResult(result.data)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val signedInUser =
                                    FirebaseAuth.getInstance().currentUser

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
                                message = "Authentication failed: ${
                                    task.exception?.localizedMessage ?: "Unknown error"
                                }"
                            }
                        }
                }
            } catch (e: Exception) {
                message = "Google Sign-In error: ${
                    e.localizedMessage ?: e.javaClass.simpleName
                }"
            }
        }

    PurpleHeader(
        "My Account",
        "Your Caller Pro profile"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.AccountCircle,
            null,
            tint = Purple,
            modifier = Modifier.size(76.dp)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "Caller Pro Account",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = AppText
        )

        Spacer(Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(Modifier.padding(20.dp)) {
                if (user != null) {
                    Text(
                        "Signed in",
                        color = Purple,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        user?.displayName ?: "Google user",
                        color = AppText,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        user?.email ?: "",
                        color = MutedText,
                        fontSize = 13.sp
                    )

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
                    Text(
                        "Google sign-in is optional.",
                        color = AppText,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "You can use available local features without an account. Sign in to access family groups and cloud backup.",
                        color = MutedText,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(18.dp))

                    Button(
                        onClick = {
                            if (authManager == null) {
                                message = "Unable to open Google sign-in."
                            } else {
                                loading = true
                                signInLauncher.launch(
                                    authManager.getSignInIntent()
                                )
                            }
                        },
                        enabled = !loading && authManager != null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Purple
                        ),
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

                        Text(
                            if (loading) "Please wait..."
                            else "Sign in with Google"
                        )
                    }
                }

                message?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        it,
                        color = Purple,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        AccountOption(
            Icons.Default.Security,
            "Privacy & Security"
        )

        AccountOption(
            Icons.Default.Cloud,
            "Backup & Restore"
        )

        AccountOption(
            Icons.Default.Info,
            "About Caller Pro"
        )
    }
}

@Composable
fun AccountOption(
    icon: ImageVector,
    title: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Purple)
            Spacer(Modifier.width(12.dp))
            Text(
                title,
                color = AppText,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = MutedText
            )
        }
    }
}
