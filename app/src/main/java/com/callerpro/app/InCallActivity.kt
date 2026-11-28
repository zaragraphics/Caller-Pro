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
import android.widget.ScrollView
import android.view.Gravity
import android.view.ViewGroup

class InCallActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var muteButton: Button
    private lateinit var speakerButton: Button
    private lateinit var holdButton: Button
    private lateinit var dtmfToggleButton: Button
    private lateinit var dtmfPanel: LinearLayout
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
            setPadding(24, 24, 24, 24)
        }
        status = TextView(this).apply {
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 16)
        }
        root.addView(status, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))

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

        holdButton = actionButton("Hold") { toggleHold() }
        root.addView(holdButton)

        dtmfToggleButton = actionButton("Show keypad") {
            dtmfPanel.visibility =
                if (dtmfPanel.visibility == android.view.View.VISIBLE) android.view.View.GONE
                else android.view.View.VISIBLE
            dtmfToggleButton.text =
                if (dtmfPanel.visibility == android.view.View.VISIBLE) "Hide keypad" else "Show keypad"
        }
        root.addView(dtmfToggleButton)

        dtmfPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = android.view.View.GONE
        }
        addDtmfKeypad(dtmfPanel)
        root.addView(dtmfPanel, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        root.addView(actionButton("End call") {
            currentCall()?.takeIf { it.state != Call.STATE_DISCONNECTED }?.disconnect()
        })

        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
        refresh()
    }

    private fun addDtmfKeypad(panel: LinearLayout) {
        val rows = listOf(
            listOf('1', '2', '3'),
            listOf('4', '5', '6'),
            listOf('7', '8', '9'),
            listOf('*', '0', '#')
        )
        rows.forEach { keys ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            keys.forEach { digit ->
                row.addView(Button(this).apply {
                    text = digit.toString()
                    textSize = 20f
                    setOnClickListener { sendDtmf(digit) }
                }, LinearLayout.LayoutParams(0, 54, 1f).apply {
                    setMargins(4, 3, 4, 3)
                })
            }
            panel.addView(row)
        }
    }

    private fun sendDtmf(digit: Char) {
        val call = currentCall() ?: return
        if (call.state != Call.STATE_ACTIVE) {
            status.text = "DTMF is available during an active call"
            return
        }
        try {
            call.playDtmfTone(digit)
            handler.postDelayed({ runCatching { call.stopDtmfTone() } }, 160)
        } catch (_: Exception) {
            status.text = "Unable to send keypad tone"
        }
    }

    private fun toggleHold() {
        val call = currentCall() ?: return
        try {
            when (call.state) {
                Call.STATE_ACTIVE -> call.hold()
                Call.STATE_HOLDING -> call.unhold()
                else -> status.text = "Hold is available during an active call"
            }
        } catch (_: Exception) {
            status.text = "Unable to change call hold state"
        }
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
        holdButton.text = when (call?.state) {
            Call.STATE_HOLDING -> "Resume"
            else -> "Hold"
        }
        val active = call?.state == Call.STATE_ACTIVE
        holdButton.isEnabled = active || call?.state == Call.STATE_HOLDING
        dtmfToggleButton.isEnabled = active
        for (i in 0 until dtmfPanel.childCount) {
            val row = dtmfPanel.getChildAt(i) as? LinearLayout ?: continue
            for (j in 0 until row.childCount) row.getChildAt(j).isEnabled = active
        }
    }
}
