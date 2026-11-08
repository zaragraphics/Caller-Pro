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
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

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

private data class FamilyGroupItem(
    val groupId: String,
    val name: String,
    val role: String
)

private data class FamilyMemberItem(
    val groupId: String,
    val uid: String,
    val name: String,
    val email: String,
    val role: String
)

private data class FamilyInvitationItem(
    val invitationId: String,
    val groupId: String,
    val groupName: String,
    val inviterName: String,
    val recipientEmail: String
)

@Composable
fun FamilyScreen(onNotice: (String) -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    val db = remember { FirebaseFirestore.getInstance() }

    var groupName by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var loadingGroups by remember { mutableStateOf(false) }
    var groups by remember { mutableStateOf<List<FamilyGroupItem>>(emptyList()) }
    var loadError by remember { mutableStateOf<String?>(null) }

    var showInviteDialog by remember { mutableStateOf(false) }
    var selectedInviteGroup by remember { mutableStateOf<FamilyGroupItem?>(null) }
    var inviteEmail by remember { mutableStateOf("") }
    var sendingInvite by remember { mutableStateOf(false) }
    var invitations by remember { mutableStateOf<List<FamilyInvitationItem>>(emptyList()) }
    var loadingInvitations by remember { mutableStateOf(false) }
    var respondingInvitation by remember { mutableStateOf<String?>(null) }
    var membersByGroup by remember { mutableStateOf<Map<String, List<FamilyMemberItem>>>(emptyMap()) }
    var loadingMembersByGroup by remember { mutableStateOf<Set<String>>(emptySet()) }
    var consentGroup by remember { mutableStateOf<FamilyGroupItem?>(null) }
    var allowCallDetails by remember { mutableStateOf(false) }
    var allowRecordingDetails by remember { mutableStateOf(false) }
    var allowSmsDetails by remember { mutableStateOf(false) }
    var savingConsent by remember { mutableStateOf(false) }
    var selectedMember by remember { mutableStateOf<FamilyMemberItem?>(null) }
    var memberConsentSummary by remember { mutableStateOf<Map<String, Boolean>?>(null) }
    var loadingMemberConsent by remember { mutableStateOf(false) }
    var removingMember by remember { mutableStateOf(false) }
    var showRemoveConfirmation by remember { mutableStateOf(false) }

    fun loadInvitations() {
        val email = user?.email?.trim()?.lowercase(Locale.ROOT)
        if (email.isNullOrBlank()) {
            invitations = emptyList()
            return
        }
        loadingInvitations = true
        db.collection("invitations")
            .whereEqualTo("recipientEmail", email)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snapshot ->
                invitations = snapshot.documents.mapNotNull { doc ->
                    val expiresAt = doc.getTimestamp("expiresAt")
                    if (expiresAt != null && expiresAt.toDate().time <= System.currentTimeMillis()) {
                        null
                    } else {
                        FamilyInvitationItem(
                            invitationId = doc.id,
                            groupId = doc.getString("groupId").orEmpty(),
                            groupName = doc.getString("groupName") ?: "Family Group",
                            inviterName = doc.getString("inviterName") ?: "Family Admin",
                            recipientEmail = doc.getString("recipientEmail").orEmpty()
                        )
                    }
                }
                loadingInvitations = false
            }
            .addOnFailureListener { error ->
                loadingInvitations = false
                onNotice("Unable to load invitations: ${error.localizedMessage ?: "Please try again."}")
            }
    }

    fun respondToInvitation(invitation: FamilyInvitationItem, accept: Boolean) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null || currentUser.email.isNullOrBlank()) {
            onNotice("Please sign in with the invited Google account to respond.")
            return
        }
        respondingInvitation = invitation.invitationId
        val invitationRef = db.collection("invitations").document(invitation.invitationId)
        val batch = db.batch()
        val response = hashMapOf<String, Any>(
            "status" to if (accept) "accepted" else "rejected",
            "acceptedByUid" to currentUser.uid,
            "respondedAt" to FieldValue.serverTimestamp()
        )
        batch.update(invitationRef, response)

        if (accept) {
            val membershipRef = db.collection("memberships")
                .document("${invitation.groupId}_${currentUser.uid}")
            val membershipData = hashMapOf<String, Any>(
                "groupId" to invitation.groupId,
                "userId" to currentUser.uid,
                "role" to "Member",
                "status" to "active",
                "memberName" to (currentUser.displayName ?: "Caller Pro Member"),
                "memberEmail" to (currentUser.email ?: ""),
                "createdAt" to FieldValue.serverTimestamp(),
                "invitationId" to invitation.invitationId
            )
            batch.set(membershipRef, membershipData)
        }

        batch.commit()
            .addOnSuccessListener {
                respondingInvitation = null
                invitations = invitations.filterNot { it.invitationId == invitation.invitationId }
                if (accept) {
                    onNotice("Invitation accepted. Please review your family data-sharing permissions.")
                    val acceptedGroup = FamilyGroupItem(invitation.groupId, invitation.groupName, "Member")
                    groups = (groups + acceptedGroup).distinctBy { it.groupId }
                    allowCallDetails = false
                    allowRecordingDetails = false
                    allowSmsDetails = false
                    consentGroup = acceptedGroup
                } else {
                    onNotice("Invitation rejected.")
                }
            }
            .addOnFailureListener { error ->
                respondingInvitation = null
                onNotice("Unable to respond to invitation: ${error.localizedMessage ?: "Please try again."}")
            }
    }

    fun saveMemberConsent() {
        val currentUser = user
        val group = consentGroup
        if (currentUser == null || group == null) return
        savingConsent = true
        val consentData = hashMapOf<String, Any>(
            "groupId" to group.groupId,
            "userId" to currentUser.uid,
            "callDetails" to allowCallDetails,
            "recordingDetails" to allowRecordingDetails,
            "smsDetails" to allowSmsDetails,
            "consentVersion" to 1,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("consents").document("${group.groupId}_${currentUser.uid}")
            .set(consentData)
            .addOnSuccessListener {
                savingConsent = false
                consentGroup = null
                onNotice("Your sharing permissions have been saved. You can change them later.")
            }
            .addOnFailureListener { error ->
                savingConsent = false
                onNotice("Unable to save permissions: ${error.localizedMessage ?: "Please try again."}")
            }
    }

    fun sendInvitation() {
        val admin = user
        val group = selectedInviteGroup
        val email = inviteEmail.trim().lowercase(Locale.ROOT)

        if (admin == null || admin.email.isNullOrBlank() || group == null) {
            onNotice("Sign in and select one of your Admin groups.")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            onNotice("Please enter a valid email address.")
            return
        }
        if (email == admin.email?.trim()?.lowercase(Locale.ROOT)) {
            onNotice("You cannot invite your own account.")
            return
        }

        sendingInvite = true
        db.collection("invitations")
            .whereEqualTo("groupId", group.groupId)
            .whereEqualTo("recipientEmail", email)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { existing ->
                if (!existing.isEmpty) {
                    sendingInvite = false
                    onNotice("A pending invitation already exists for this email.")
                    return@addOnSuccessListener
                }

                val invitationRef = db.collection("invitations").document()
                val invitationData = hashMapOf<String, Any>(
                    "invitationId" to invitationRef.id,
                    "groupId" to group.groupId,
                    "groupName" to group.name,
                    "inviterUid" to admin.uid,
                    "inviterName" to (admin.displayName ?: "Family Admin"),
                    "recipientEmail" to email,
                    "role" to "Member",
                    "status" to "pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "expiresAt" to Date(System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L)
                )
                invitationRef.set(invitationData)
                    .addOnSuccessListener {
                        sendingInvite = false
                        showInviteDialog = false
                        inviteEmail = ""
                        selectedInviteGroup = null
                        onNotice("Invitation sent to $email. It will appear in their Caller Pro app when they sign in with this email.")
                    }
                    .addOnFailureListener { error ->
                        sendingInvite = false
                        onNotice("Unable to send invitation: ${error.localizedMessage ?: "Please try again."}")
                    }
            }
            .addOnFailureListener { error ->
                sendingInvite = false
                onNotice("Unable to check existing invitations: ${error.localizedMessage ?: "Please try again."}")
            }
    }

    fun loadGroups() {
        val uid = user?.uid
        if (uid.isNullOrBlank()) {
            groups = emptyList()
            loadError = "Sign in with Google to create and view family groups."
            return
        }

        loadingGroups = true
        loadError = null
        db.collection("memberships")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { membershipSnapshot ->
                if (membershipSnapshot.isEmpty) {
                    groups = emptyList()
                    loadingGroups = false
                } else {
                    val loadedGroups = mutableListOf<FamilyGroupItem>()
                    var pending = membershipSnapshot.documents.size

                    membershipSnapshot.documents.forEach { membership ->
                        val groupId = membership.getString("groupId").orEmpty()
                        val role = membership.getString("role").orEmpty()
                        // Older memberships may not contain profile fields. Let the member's own
                        // signed-in app safely refresh only its name and email.
                        if (membership.getString("status") == "active" && role == "Member") {
                            val profileName = user?.displayName.orEmpty()
                            val profileEmail = user?.email.orEmpty()
                            if (profileName.isNotBlank() || profileEmail.isNotBlank()) {
                                membership.reference.update(
                                    mapOf(
                                        "memberName" to profileName,
                                        "memberEmail" to profileEmail
                                    )
                                )
                            }
                        }
                        if (groupId.isBlank()) {
                            pending--
                            if (pending == 0) {
                                groups = loadedGroups.sortedBy { it.name.lowercase() }
                                loadingGroups = false
                            }
                        } else {
                            db.collection("familyGroups").document(groupId).get()
                                .addOnSuccessListener { groupDocument ->
                                    if (groupDocument.exists()) {
                                        loadedGroups.add(
                                            FamilyGroupItem(
                                                groupId = groupId,
                                                name = groupDocument.getString("name") ?: "Family Group",
                                                role = role
                                            )
                                        )
                                    }
                                    pending--
                                    if (pending == 0) {
                                        groups = loadedGroups.sortedBy { it.name.lowercase() }
                                        loadingGroups = false
                                    }
                                }
                                .addOnFailureListener { error ->
                                    pending--
                                    if (pending == 0) {
                                        groups = loadedGroups.sortedBy { it.name.lowercase() }
                                        loadingGroups = false
                                        loadError = error.localizedMessage ?: "Unable to load one or more groups."
                                    }
                                }
                        }
                    }
                }
            }
            .addOnFailureListener { error ->
                loadingGroups = false
                loadError = error.localizedMessage ?: "Unable to load family groups."
            }
    }

    fun loadGroupMembers(group: FamilyGroupItem) {
        loadingMembersByGroup = loadingMembersByGroup + group.groupId
        db.collection("memberships")
            .whereEqualTo("groupId", group.groupId)
            .get()
            .addOnSuccessListener { snapshot ->
                val loaded = snapshot.documents.filter {
                    it.getString("status") == "active"
                }.map { doc ->
                    val uid = doc.getString("userId").orEmpty()
                    FamilyMemberItem(
                        groupId = group.groupId,
                        uid = uid,
                        name = doc.getString("memberName")?.takeIf { it.isNotBlank() }
                            ?: if (uid == user?.uid) (user?.displayName ?: "You") else "Family member",
                        email = doc.getString("memberEmail").orEmpty(),
                        role = doc.getString("role") ?: "Member"
                    )
                }.sortedWith(compareBy<FamilyMemberItem> { if (it.role == "Admin") 0 else 1 }.thenBy { it.name.lowercase() })
                membersByGroup = membersByGroup + (group.groupId to loaded)
                loadingMembersByGroup = loadingMembersByGroup - group.groupId
            }
            .addOnFailureListener { error ->
                loadingMembersByGroup = loadingMembersByGroup - group.groupId
                onNotice("Unable to load members for ${group.name}: ${error.localizedMessage ?: "Check Firestore membership read rules."}")
            }
    }

    fun openMemberDetails(member: FamilyMemberItem) {
        selectedMember = member
        memberConsentSummary = null
        loadingMemberConsent = true
        db.collection("consents").document("${member.groupId}_${member.uid}").get()
            .addOnSuccessListener { doc ->
                memberConsentSummary = if (doc.exists()) {
                    mapOf(
                        "callDetails" to (doc.getBoolean("callDetails") ?: false),
                        "recordingDetails" to (doc.getBoolean("recordingDetails") ?: false),
                        "smsDetails" to (doc.getBoolean("smsDetails") ?: false)
                    )
                } else emptyMap()
                loadingMemberConsent = false
            }
            .addOnFailureListener { error ->
                loadingMemberConsent = false
                memberConsentSummary = emptyMap()
                onNotice("Unable to load member permissions: ${error.localizedMessage ?: "Please try again."}")
            }
    }

    fun removeSelectedMember() {
        val member = selectedMember ?: return
        val admin = user ?: return
        if (member.role == "Admin") {
            onNotice("The group Admin cannot be removed from this screen.")
            return
        }
        removingMember = true
        db.collection("memberships").document("${member.groupId}_${member.uid}")
            .update(
                mapOf(
                    "status" to "removed",
                    "removedAt" to FieldValue.serverTimestamp(),
                    "removedBy" to admin.uid
                )
            )
            .addOnSuccessListener {
                membersByGroup = membersByGroup + (
                    member.groupId to membersByGroup[member.groupId].orEmpty().filterNot { it.uid == member.uid }
                )
                selectedMember = null
                memberConsentSummary = null
                showRemoveConfirmation = false
                removingMember = false
                onNotice("Member removed. Historical records and consent documents have been retained. You can now send a fresh invitation to the same email.")
            }
            .addOnFailureListener { error ->
                removingMember = false
                onNotice("Unable to remove member: ${error.localizedMessage ?: "Please check the updated Firestore rules."}")
            }
    }

    LaunchedEffect(user?.uid, user?.email) {
        loadGroups()
        loadInvitations()
    }

    LaunchedEffect(groups) {
        groups.filter { it.role == "Admin" }.forEach { group ->
            if (membersByGroup[group.groupId] == null && group.groupId !in loadingMembersByGroup) {
                loadGroupMembers(group)
            }
        }
    }

    fun createFamilyGroup() {
        val uid = user?.uid
        val cleanName = groupName.trim()

        if (uid.isNullOrBlank()) {
            onNotice("Please sign in with Google before creating a family group.")
            return
        }
        if (cleanName.isBlank()) {
            onNotice("Please enter a family group name.")
            return
        }
        if (cleanName.length > 60) {
            onNotice("Family group name must be 60 characters or fewer.")
            return
        }

        creating = true
        val newGroupId = UUID.randomUUID().toString()
            .replace("-", "")
            .take(10)
            .uppercase(Locale.ROOT)

        val groupRef = db.collection("familyGroups").document(newGroupId)
        val membershipRef = db.collection("memberships")
            .document("${newGroupId}_${uid}")

        val groupData = hashMapOf(
            "groupId" to newGroupId,
            "name" to cleanName,
            "creatorUid" to uid,
            "adminUid" to uid,
            "status" to "active",
            "createdAt" to FieldValue.serverTimestamp()
        )
        val membershipData = hashMapOf(
            "groupId" to newGroupId,
            "userId" to uid,
            "role" to "Admin",
            "status" to "active",
            "memberName" to (user?.displayName ?: "Family Admin"),
            "memberEmail" to (user?.email ?: ""),
            "createdAt" to FieldValue.serverTimestamp()
        )

        val batch = db.batch()
        batch.set(groupRef, groupData)
        batch.set(membershipRef, membershipData)
        batch.commit()
            .addOnSuccessListener {
                creating = false
                showCreateDialog = false
                groupName = ""
                onNotice("Family group created successfully. You are the Admin. Group ID: $newGroupId")
                loadGroups()
            }
            .addOnFailureListener { error ->
                creating = false
                onNotice(
                    "Unable to create family group: ${
                        error.localizedMessage ?: "Please check your internet connection and Firestore Rules."
                    }"
                )
            }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        PurpleHeader("Family Groups", "Manage members, roles and permissions")

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Icon(Icons.Default.Groups, null, tint = Blue, modifier = Modifier.size(42.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Your family groups", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppText)
                        Text(
                            "Create a group and become its Admin. You can also belong to other groups as a Member.",
                            color = MutedText,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = {
                                if (user == null) {
                                    onNotice("Please sign in with Google before creating a family group.")
                                } else {
                                    groupName = ""
                                    showCreateDialog = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !creating,
                            colors = ButtonDefaults.buttonColors(containerColor = Blue),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Create Family Group")
                        }
                        OutlinedButton(
                            onClick = { loadInvitations() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Mail, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Refresh Invitations")
                        }
                    }
                }
            }

            item {
                Text("Family Invitations", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppText)
            }

            if (loadingInvitations) {
                item {
                    Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Blue)
                    }
                }
            } else if (invitations.isEmpty()) {
                item {
                    Text("No pending invitations for ${user?.email ?: "your account"}.", color = MutedText, fontSize = 12.sp)
                }
            } else {
                items(invitations, key = { it.invitationId }) { invitation ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(invitation.groupName, color = AppText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Invited by ${invitation.inviterName}", color = MutedText, fontSize = 12.sp)
                            Text("Invited email: ${invitation.recipientEmail}", color = MutedText, fontSize = 11.sp)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = { respondToInvitation(invitation, false) },
                                    enabled = respondingInvitation != invitation.invitationId,
                                    modifier = Modifier.weight(1f)
                                ) { Text("Reject") }
                                Button(
                                    onClick = { respondToInvitation(invitation, true) },
                                    enabled = respondingInvitation != invitation.invitationId,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Blue)
                                ) {
                                    if (respondingInvitation == invitation.invitationId) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Text("Accept")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Your Groups", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppText)
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = { loadGroups() },
                        enabled = !loadingGroups
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Refresh")
                    }
                }
            }

            if (loadingGroups) {
                item {
                    Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Blue)
                    }
                }
            } else if (groups.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Groups, null, tint = MutedText, modifier = Modifier.size(34.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("No family groups yet", color = AppText, fontWeight = FontWeight.SemiBold)
                            Text("Groups you create or join will appear here.", color = MutedText, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(groups, key = { it.groupId }) { group ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Groups, null, tint = Blue, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(group.name, color = AppText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Group ID: ${group.groupId}", color = MutedText, fontSize = 11.sp)
                                }
                                Surface(
                                    color = PaleBlue,
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(
                                        group.role,
                                        color = Blue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Members", color = AppText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(Modifier.width(7.dp))
                                Surface(color = PaleBlue, shape = RoundedCornerShape(20.dp)) {
                                    Text(
                                        "${membersByGroup[group.groupId]?.size ?: 0}",
                                        color = Blue,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                TextButton(
                                    onClick = { loadGroupMembers(group) },
                                    enabled = group.groupId !in loadingMembersByGroup
                                ) {
                                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("Refresh", fontSize = 11.sp)
                                }
                            }
                            if (group.groupId in loadingMembersByGroup) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Blue)
                            } else {
                                val groupMembers = membersByGroup[group.groupId].orEmpty()
                                if (groupMembers.isEmpty()) {
                                    Text("No active members found yet. Accepted members will appear here.", color = MutedText, fontSize = 12.sp)
                                } else {
                                    groupMembers.forEach { member ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth()
                                                .padding(vertical = 6.dp)
                                                .clip(RoundedCornerShape(13.dp))
                                                .background(PaleBlue)
                                                .clickable { openMemberDetails(member) }
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                Modifier.size(36.dp).clip(CircleShape).background(Blue.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Person, null, tint = Blue)
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(member.name, color = AppText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                                Text(member.email.ifBlank { member.uid.take(10) }, color = MutedText, fontSize = 11.sp)
                                            }
                                            Surface(
                                                color = if (member.role == "Admin") Navy else Color.White,
                                                shape = RoundedCornerShape(20.dp)
                                            ) {
                                                Text(
                                                    member.role,
                                                    color = if (member.role == "Admin") Color.White else Blue,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (group.role == "Admin") {
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = {
                                        selectedInviteGroup = group
                                        inviteEmail = ""
                                        showInviteDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.PersonAdd, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Invite Member")
                                }
                            }
                        }
                    }
                }
            }

            loadError?.let { message ->
                item {
                    Text(message, color = Color(0xFFD04444), fontSize = 12.sp)
                }
            }

            item {
                Text("Consent & Privacy", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppText)
                Card(
                    colors = CardDefaults.cardColors(containerColor = PaleBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, null, tint = Blue, modifier = Modifier.size(30.dp))
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

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { if (!creating) showCreateDialog = false },
            title = { Text("Create Family Group") },
            text = {
                Column {
                    Text("Enter a name for your family group.", color = MutedText, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { if (it.length <= 60) groupName = it },
                        label = { Text("Family Group Name") },
                        placeholder = { Text("Example: Mulla Family") },
                        singleLine = true,
                        enabled = !creating
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { createFamilyGroup() },
                    enabled = !creating && groupName.trim().isNotEmpty()
                ) {
                    if (creating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Create")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreateDialog = false },
                    enabled = !creating
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showInviteDialog) {
        Dialog(onDismissRequest = { if (!sendingInvite) showInviteDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = AppBackground,
                shadowElevation = 18.dp
            ) {
                Column {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .background(Brush.linearGradient(listOf(DeepBlue, Navy, Blue)))
                            .padding(horizontal = 22.dp, vertical = 22.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(46.dp).clip(RoundedCornerShape(15.dp))
                                    .background(Color.White.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PersonAdd, null, tint = Cyan, modifier = Modifier.size(25.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Invite Family Member", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                Text("Add someone to your trusted group", color = Color.White.copy(alpha = 0.78f), fontSize = 11.sp)
                            }
                        }
                    }
                    Column(Modifier.padding(20.dp)) {
                        Text("Family group", color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(selectedInviteGroup?.name ?: "Family Group", color = AppText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Enter the member's registered Google email. They must sign in to Caller Pro with this same email to receive and accept the invitation.",
                            color = MutedText,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(14.dp))
                        OutlinedTextField(
                            value = inviteEmail,
                            onValueChange = { inviteEmail = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Email address") },
                            placeholder = { Text("example@gmail.com") },
                            leadingIcon = { Icon(Icons.Default.Email, null, tint = Blue) },
                            singleLine = true,
                            enabled = !sendingInvite,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Blue,
                                focusedLabelColor = Blue,
                                cursorColor = Blue,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .background(PaleBlue, RoundedCornerShape(12.dp))
                                .padding(11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, null, tint = Blue, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Invitation remains valid for 7 days.", color = Navy, fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { showInviteDialog = false },
                                enabled = !sendingInvite,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(13.dp)
                            ) { Text("Cancel", color = MutedText) }
                            Button(
                                onClick = { sendInvitation() },
                                enabled = !sendingInvite && inviteEmail.isNotBlank(),
                                modifier = Modifier.weight(1.35f),
                                shape = RoundedCornerShape(13.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Blue)
                            ) {
                                if (sendingInvite) {
                                    CircularProgressIndicator(modifier = Modifier.size(17.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(Modifier.width(7.dp))
                                    Text("Sending")
                                } else {
                                    Icon(Icons.Default.Send, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Send Invite")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedMember?.let { member ->
        AlertDialog(
            onDismissRequest = { if (!removingMember) { selectedMember = null; memberConsentSummary = null } },
            title = { Text("Member Details", color = AppText, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(member.name, color = AppText, fontWeight = FontWeight.SemiBold)
                    Text(member.email.ifBlank { "Email not available" }, color = MutedText, fontSize = 12.sp)
                    Text("Role: ${member.role}", color = MutedText, fontSize = 12.sp)
                    Divider()
                    Text("Data-sharing permissions", color = AppText, fontWeight = FontWeight.Bold)
                    if (loadingMemberConsent) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Blue)
                    } else {
                        val consent = memberConsentSummary
                        if (consent.isNullOrEmpty()) {
                            Text("No saved consent record found.", color = MutedText, fontSize = 12.sp)
                        } else {
                            Text("Calls: ${if (consent["callDetails"] == true) "Allowed" else "Not allowed"}", color = AppText, fontSize = 12.sp)
                            Text("Recordings: ${if (consent["recordingDetails"] == true) "Allowed" else "Not allowed"}", color = AppText, fontSize = 12.sp)
                            Text("SMS: ${if (consent["smsDetails"] == true) "Allowed" else "Not allowed"}", color = AppText, fontSize = 12.sp)
                        }
                    }
                    Text("Removing this member stops their group membership. Historical records are retained.", color = MutedText, fontSize = 11.sp)
                }
            },
            confirmButton = {
                if (member.role != "Admin") {
                    TextButton(onClick = { showRemoveConfirmation = true }, enabled = !removingMember) {
                        if (removingMember) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text("Remove Member", color = Color(0xFFD04444))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedMember = null; memberConsentSummary = null }, enabled = !removingMember) {
                    Text("Close")
                }
            }
        )
    }

    if (showRemoveConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!removingMember) showRemoveConfirmation = false },
            title = { Text("Remove family member?") },
            text = {
                Text("This removes the member's access to this group. Historical records and consent documents will be retained. You can invite the same email again later.")
            },
            confirmButton = {
                TextButton(onClick = { removeSelectedMember() }, enabled = !removingMember) {
                    if (removingMember) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Remove", color = Color(0xFFD04444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirmation = false }, enabled = !removingMember) {
                    Text("Cancel")
                }
            }
        )
    }

    consentGroup?.let { group ->
        AlertDialog(
            onDismissRequest = { /* Consent must be explicitly saved; keep this screen open. */ },
            title = { Text("Family Data Sharing Permissions", color = AppText, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "You joined ${group.name}. Choose what this group's Admin may access. All options are optional.",
                        color = MutedText,
                        fontSize = 13.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Call Details", color = AppText, fontWeight = FontWeight.SemiBold)
                            Text("Incoming, outgoing and missed call history", color = MutedText, fontSize = 11.sp)
                        }
                        Checkbox(checked = allowCallDetails, onCheckedChange = { allowCallDetails = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Recording Details", color = AppText, fontWeight = FontWeight.SemiBold)
                            Text("Available recording list and playback", color = MutedText, fontSize = 11.sp)
                        }
                        Checkbox(checked = allowRecordingDetails, onCheckedChange = { allowRecordingDetails = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("SMS Details", color = AppText, fontWeight = FontWeight.SemiBold)
                            Text("SMS history and message details", color = MutedText, fontSize = 11.sp)
                        }
                        Checkbox(checked = allowSmsDetails, onCheckedChange = { allowSmsDetails = it })
                    }
                    Text(
                        "You may leave any option turned off. Permission management for changing these choices will be added in a later Family Settings update. Android may not support capturing every call or message.",
                        color = MutedText,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { saveMemberConsent() },
                    enabled = !savingConsent,
                    colors = ButtonDefaults.buttonColors(containerColor = Blue)
                ) {
                    if (savingConsent) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("Save Permissions")
                }
            }
        )
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
