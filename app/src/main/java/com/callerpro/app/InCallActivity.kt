package com.callerpro.app

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.VideoProfile
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import android.view.ViewGroup

class InCallActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var muteButton: Button
    private lateinit var speakerButton: Button
    private val handler = Handler(Looper.getMainLooper())

    private val refreshTask = object : Runnable {
        override fun run() {
            refresh()
            if (!isFinishing) handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 32, 24, 32)
        }
        status = TextView(this).apply {
            textSize = 22f
            gravity = Gravity.CENTER
        }
        root.addView(status, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(actionButton("Answer") {
            currentCall()?.takeIf { it.state == Call.STATE_RINGING }
                ?.answer(VideoProfile.STATE_AUDIO_ONLY)
        })
        root.addView(actionButton("Reject") {
            currentCall()?.takeIf { it.state == Call.STATE_RINGING }?.reject(false, null)
        })
        muteButton = actionButton("Mute") {
            CallerProInCallService.instance?.let { service ->
                val currentlyMuted = service.callAudioState?.isMuted == true
                service.setMuted(!currentlyMuted)
            }
        }
        root.addView(muteButton)
        speakerButton = actionButton("Speaker") {
            CallerProInCallService.instance?.let { service ->
                val speakerOn = service.callAudioState?.route == CallAudioState.ROUTE_SPEAKER
                service.setAudioRoute(
                    if (speakerOn) CallAudioState.ROUTE_EARPIECE
                    else CallAudioState.ROUTE_SPEAKER
                )
            }
        }
        root.addView(speakerButton)
        root.addView(actionButton("End call") {
            currentCall()?.takeIf { it.state != Call.STATE_DISCONNECTED }?.disconnect()
        })
        setContentView(root)
        refresh()
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(refreshTask)
        handler.post(refreshTask)
    }

    override fun onPause() {
        handler.removeCallbacks(refreshTask)
        super.onPause()
    }

    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener {
            action()
            refresh()
        }
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
        val service = CallerProInCallService.instance
        muteButton.text = if (service?.callAudioState?.isMuted == true) "Unmute" else "Mute"
        speakerButton.text = if (service?.callAudioState?.route == CallAudioState.ROUTE_SPEAKER)
            "Speaker on" else "Speaker"
    }
}
