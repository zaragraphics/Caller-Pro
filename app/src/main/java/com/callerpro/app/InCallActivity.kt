package com.callerpro.app

import android.app.Activity
import android.os.Bundle
import android.telecom.Call
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import android.view.ViewGroup

class InCallActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 32, 24, 32)
        }
        status = TextView(this).apply { textSize = 22f; gravity = Gravity.CENTER }
        root.addView(status, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(actionButton("Answer") { currentCall()?.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY) })
        root.addView(actionButton("Reject") { currentCall()?.reject(false, null) })
        root.addView(actionButton("Mute / Unmute") {
            currentCall()?.let { call ->
                val service = CallerProInCallService.instance
                service?.setMuted(!service.isMuted)
            }
        })
        root.addView(actionButton("Speaker") {
            val service = CallerProInCallService.instance
            service?.setAudioRoute(
                if (service.callAudioState?.route == android.telecom.CallAudioState.ROUTE_SPEAKER)
                    android.telecom.CallAudioState.ROUTE_EARPIECE
                else android.telecom.CallAudioState.ROUTE_SPEAKER
            )
        })
        root.addView(actionButton("End call") { currentCall()?.disconnect() })
        setContentView(root)
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { action(); refresh() }
    }

    private fun currentCall(): Call? = CallerProInCallService.activeCall

    private fun refresh() {
        val call = currentCall()
        status.text = when (call?.state) {
            Call.STATE_RINGING -> "Incoming call"
            Call.STATE_DIALING, Call.STATE_CONNECTING -> "Calling…"
            Call.STATE_ACTIVE -> "Call connected"
            Call.STATE_HOLDING -> "Call on hold"
            Call.STATE_DISCONNECTED -> "Call ended"
            else -> "Caller Pro call controls"
        }
    }

    companion object {
        @Volatile var refreshRequested: Boolean = false
    }
}
