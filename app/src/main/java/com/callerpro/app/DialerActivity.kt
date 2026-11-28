package com.callerpro.app

import android.app.Activity
import android.os.Bundle
import android.telecom.TelecomManager
import android.net.Uri
import android.content.Context
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
            setPadding(28, 36, 28, 28)
        }
        root.addView(TextView(this).apply {
            text = "Caller Pro Dialer"
            textSize = 25f
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        number = EditText(this).apply {
            hint = "Enter phone number"
            textSize = 22f
            inputType = InputType.TYPE_CLASS_PHONE
            setSingleLine(true)
            intent?.data?.schemeSpecificPart?.let { setText(it) }
        }
        root.addView(number, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(Button(this).apply {
            text = "Call"
            setOnClickListener { placeCall() }
        })
        root.addView(Button(this).apply {
            text = "Clear"
            setOnClickListener { number.text.clear() }
        })
        setContentView(root)
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
        } catch (e: SecurityException) {
            number.error = "Phone permission is required"
        } catch (e: Exception) {
            number.error = "Unable to start call"
        }
    }
}
