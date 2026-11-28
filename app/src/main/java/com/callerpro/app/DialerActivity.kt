package com.callerpro.app

import android.app.Activity
import android.os.Bundle
import android.os.Build
import android.app.role.RoleManager
import android.telecom.TelecomManager
import android.net.Uri
import android.content.Context
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.view.ViewGroup
import android.view.Gravity
import android.text.InputType

class DialerActivity : Activity() {
    private lateinit var number: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 28, 24, 24)
        }
        root.addView(TextView(this).apply {
            text = "Caller Pro Dialer"
            textSize = 25f
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        number = EditText(this).apply {
            hint = "Enter phone number"
            textSize = 24f
            gravity = Gravity.CENTER
            inputType = InputType.TYPE_CLASS_PHONE
            setSingleLine(true)
            showSoftInputOnFocus = false
            intent?.data?.schemeSpecificPart?.let { setText(it) }
        }
        root.addView(number, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val keypad = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("*", "0", "#")
        )
        keypad.forEach { rowKeys ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            rowKeys.forEach { key ->
                row.addView(Button(this).apply {
                    text = key
                    textSize = 22f
                    setOnClickListener {
                        val end = number.selectionStart.coerceAtLeast(0)
                        number.text.insert(end, key)
                    }
                }, LinearLayout.LayoutParams(0, 58, 1f).apply {
                    setMargins(5, 3, 5, 3)
                })
            }
            root.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        actions.addView(Button(this).apply {
            text = "Delete"
            setOnClickListener {
                val end = number.selectionStart.coerceAtLeast(0)
                if (end > 0) number.text.delete(end - 1, end)
            }
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        actions.addView(Button(this).apply {
            text = "Call"
            setOnClickListener { placeCall() }
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(actions)
        root.addView(Button(this).apply {
            text = "Set as default phone app"
            setOnClickListener { requestDefaultDialerRole() }
        })
        setContentView(root)
    }

    private fun requestDefaultDialerRole() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val roleManager = getSystemService(RoleManager::class.java)
                if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) &&
                    !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
                ) {
                    startActivityForResult(
                        roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER),
                        REQUEST_DIALER_ROLE
                    )
                }
            } else {
                val changeIntent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                    putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
                }
                startActivityForResult(changeIntent, REQUEST_DIALER_ROLE)
            }
        } catch (_: Exception) {
            android.widget.Toast.makeText(this, "Unable to open default phone app settings", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun placeCall() {
        val dialString = number.text.toString().trim()
        if (dialString.isBlank()) {
            number.error = "Enter a phone number"
            return
        }
        val telecom = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        try {
            telecom.placeCall(Uri.fromParts("tel", dialString, null), Bundle())
        } catch (_: SecurityException) {
            number.error = "Phone permission is required"
        } catch (_: Exception) {
            number.error = "Unable to start call"
        }
    }

    companion object {
        private const val REQUEST_DIALER_ROLE = 4102
    }
}
