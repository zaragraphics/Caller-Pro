package com.callerpro.app

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.SettingsPhone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DialerNavy = Color(0xFF07152F)
private val DialerBlue = Color(0xFF168BEE)
private val DialerCyan = Color(0xFF21D4E8)
private val DialerBg = Color(0xFFF4F7FC)
private val DialerMuted = Color(0xFF75829A)

class DialerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(7, 21, 47)
        window.navigationBarColor = android.graphics.Color.rgb(244, 247, 252)
        window.decorView.systemUiVisibility = 0
        val initialNumber = intent?.data?.schemeSpecificPart.orEmpty()
        setContent {
            MaterialTheme {
                CallerProDialerScreen(
                    initialNumber = initialNumber,
                    onCall = ::placeCall,
                    onDefaultDialer = ::requestDefaultDialerRole
                )
            }
        }
    }

    private fun requestDefaultDialerRole() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val manager = getSystemService(RoleManager::class.java)
                if (manager.isRoleAvailable(RoleManager.ROLE_DIALER) && !manager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                    startActivityForResult(manager.createRequestRoleIntent(RoleManager.ROLE_DIALER), 4102)
                } else if (manager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                    Toast.makeText(this, "Caller Pro is already your default phone app", Toast.LENGTH_SHORT).show()
                }
            } else {
                startActivityForResult(
                    Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).putExtra(
                        TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName
                    ), 4102
                )
            }
        } catch (_: Exception) {
            Toast.makeText(this, "Unable to open default phone app settings", Toast.LENGTH_LONG).show()
        }
    }

    private fun placeCall(value: String) {
        if (value.isBlank()) {
            Toast.makeText(this, "Enter a phone number", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val telecom = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            telecom.placeCall(Uri.fromParts("tel", value, null), Bundle())
        } catch (_: SecurityException) {
            Toast.makeText(this, "Phone permission is required", Toast.LENGTH_LONG).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Unable to start call", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
private fun CallerProDialerScreen(
    initialNumber: String,
    onCall: (String) -> Unit,
    onDefaultDialer: () -> Unit
) {
    var number by remember { mutableStateOf(initialNumber) }
    val keys = listOf(
        listOf("1" to "", "2" to "ABC", "3" to "DEF"),
        listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
        listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
        listOf("*" to "", "0" to "+", "#" to "")
    )
    Column(
        modifier = Modifier.fillMaxSize().background(DialerBg).padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(46.dp).background(DialerNavy, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.SettingsPhone, null, tint = DialerCyan, modifier = Modifier.size(25.dp)) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("CALLER PRO", color = DialerCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Phone Dialer", color = DialerNavy, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = number,
                onValueChange = { number = it.filter { c -> c.isDigit() || c == '+' || c == '*' || c == '#' } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = DialerNavy, fontSize = 27.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium
                ),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (number.isEmpty()) Text("Enter phone number", color = DialerMuted, fontSize = 18.sp)
                        inner()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(20.dp))
        keys.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { (digit, letters) ->
                    Column(
                        Modifier.weight(1f).padding(vertical = 5.dp).height(66.dp)
                            .background(Color.White, CircleShape)
                            .clickable { number += digit },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(digit, color = DialerNavy, fontSize = 27.sp, fontWeight = FontWeight.Medium)
                        if (letters.isNotEmpty()) Text(letters, color = DialerMuted, fontSize = 9.sp, letterSpacing = 1.5.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { number = "" }) { Text("CLEAR", color = DialerMuted, fontWeight = FontWeight.Bold) }
            Box(
                Modifier.size(66.dp).background(Color(0xFF16A765), CircleShape).clickable { onCall(number) },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Call, "Call", tint = Color.White, modifier = Modifier.size(30.dp)) }
            IconButton(onClick = { if (number.isNotEmpty()) number = number.dropLast(1) }) {
                Icon(Icons.Default.Backspace, "Delete digit", tint = DialerNavy, modifier = Modifier.size(25.dp))
            }
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(
            onClick = onDefaultDialer,
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DialerBlue)
        ) {
            Text("SET AS DEFAULT PHONE APP", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        }
    }
}
